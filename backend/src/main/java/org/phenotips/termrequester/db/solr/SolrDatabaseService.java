/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.termrequester.db.solr;

import java.io.File;
import java.io.IOException;

import java.net.URISyntaxException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.core.CoreContainer;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Manages a solr instance as a database.
 *
 * @version $Id : $
 */
@Singleton
public class SolrDatabaseService implements DatabaseService
{
    /* Lots of this came from org.phenotips.variantstore.db.solr.SolrController */

    /**
     * A pattern to parse our IDs.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("NONHPO_(\\d{6})");

    /**
     * The very fist id to use.
     */
    private static final String INITIAL_ID = "NONHPO_000001";

    /**
     * A joiner to join different parts of a Solr query with an OR.
     */
    private static final Joiner OR_QUERY_JOINER = Joiner.on(' ').skipNulls();

    /**
     * A query string to match all docuemnts.
     */
    public static final String WILDCARD_QSTRING = "*:*";

    /**
     * The name of the solr core.
     */
    public static final String CORE_NAME = "termrequester";

    /**
     * The path where the database is.
     */
    private Path path;

    /**
     * The solr core container.
     */
    private CoreContainer cores;

    /**
     * The solr client.
     */
    private SolrClient server;

    /**
     * Whether we've been initialized.
     */
    private boolean up = false;

    /**
     * Whether we should commit at the end of every write.
     */
    private boolean autocommit = false;

    /**
     * The solr mapper to use to turn phenotypes to documents and vice-versa.
     */
    private SolrMapper mapper;

    @Override
    public void init(Path path) throws IOException
    {
        /* Make sure initialization is idempotent */
        if (!up) {
            up = true;
            File resources;
            try {
                resources = new File(this.getClass().getResource("/solr").toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            FileUtils.copyDirectoryToDirectory(resources, new File(path.toUri()));
            this.path = path.resolve("solr");
            cores = new CoreContainer(this.path.toString());
            cores.load();
            server = new EmbeddedSolrServer(cores, CORE_NAME);
            mapper = new SolrMapper();
        }
    }

    @Override
    public void shutdown() throws IOException
    {
        if (up) {
            commit();
            cores.shutdown();
            up = false;
        }
    }

    @Override
    public void commit() throws IOException
    {
        try {
            server.commit();
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype savePhenotype(Phenotype pt) throws IOException
    {
        /* Nothing to do, it hasn't been changed */
        if (!pt.isDirty()) {
            return pt;
        }
        if (pt.getId().isPresent()) {
            try {
                String id = pt.getId().get();
                checkState(server.getById(id) != null, "ID %s does not exist when expected to", id);
                server.deleteById(pt.getId().get());
                /* We don't wanna trigger anything (two versions of the same document co-existing) a
                 * little later when saving, so just commit now */
                commit();
            } catch(SolrServerException e) {
                throw new IOException(e);
            }
        } else {
            String nextId = getNextId();
            pt.setId(nextId);
        }
        SolrInputDocument doc = mapper.toDoc(pt);
        pt.setTimeCreated((Date) doc.getFieldValue(Schema.TIME_CREATED));
        pt.setTimeModified((Date) doc.getFieldValue(Schema.TIME_MODIFIED));
        try {
            server.add(doc);
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
        if (autocommit) {
            commit();
        }
        pt.setClean();
        return pt;
    }

    @Override
    public boolean deletePhenotype(Phenotype pt) throws IOException
    {
        checkArgument(pt.getId().isPresent(), "Phenotype %s cannot be deleted without an id", pt);
        try {
            SolrDocument doc = server.getById(pt.getId().get());
            if (doc == null) {
                return false;
            }
            server.deleteById(pt.getId().get());
            if (autocommit) {
                commit();
            }
            return true;
        } catch(SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype getPhenotypeById(String id) throws IOException
    {
        try {
            SolrDocument doc = server.getById(id);
            if (doc == null) {
                return Phenotype.NULL;
            }
            Phenotype pt = mapper.fromDoc(doc);
            String parentId = (String) doc.getFieldValue(Schema.PARENT);
            SolrDocument parentDoc = server.getById(parentId);
            pt.setParent(parentDoc == null ? Phenotype.NULL : mapper.fromDoc(parentDoc));
            return pt;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype getPhenotypeByIssueNumber(String issueNumber) throws IOException
    {
        try {
            String queryString = String.format("%s:%s", Schema.ISSUE_NUMBER, issueNumber);
            SolrQuery q = new SolrQuery().
                setQuery(queryString);
            QueryResponse resp = server.query(q);
            List<SolrDocument> results = resp.getResults();
            checkState(results.size() <= 1, "Multiple documents with issue number %s", issueNumber);
            if (results.size() == 0) {
                return Phenotype.NULL;
            }
            return mapper.fromDoc(results.get(0));
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype getPhenotype(Phenotype other) throws IOException
    {
        Set<String> names = other.getSynonyms();
        names.add(other.getName());
        List<String> queryPieces = new ArrayList<>(names.size() * 2 + 2);
        /* To test for field equality */
        String fieldIs = "%s:\"%s\"";
        if (other.getId().isPresent()) {
            queryPieces.add(String.format(fieldIs, Schema.ID,
                        ClientUtils.escapeQueryChars(other.getId().get())));
        }
        if (other.getIssueNumber().isPresent()) {
            queryPieces.add(String.format(fieldIs, Schema.ISSUE_NUMBER,
                        ClientUtils.escapeQueryChars(other.getIssueNumber().get())));
        }
        for (String name : names) {
            queryPieces.add(String.format(fieldIs, Schema.NAME_EXACT,
                        ClientUtils.escapeQueryChars(name)));
            queryPieces.add(String.format(fieldIs, Schema.SYNONYM_EXACT,
                        ClientUtils.escapeQueryChars(name)));
        }
        String queryString = OR_QUERY_JOINER.join(queryPieces);
        SolrQuery q = new SolrQuery().setQuery(queryString);
        try {
            QueryResponse resp = server.query(q);
            List<SolrDocument> results = resp.getResults();
            /* TODO: This check might not be the best idea */
            checkState(results.size() <= 1, "Multiple documents match %s", other);
            if (results.size() == 0) {
                return Phenotype.NULL;
            }
            return mapper.fromDoc(results.get(0));
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<Phenotype> searchPhenotypes(String text) throws IOException
    {
        try {
            SolrQuery q = new SolrQuery();
            String escaped = ClientUtils.escapeQueryChars(text);
            q.add(CommonParams.Q, escaped);
            q.add(SpellingParams.SPELLCHECK_Q, text);
            q.add(DisMaxParams.PF, String.format("%s^20 %s^36 %s^100 %s^30 %s^15 %s^25 %s^70 %s^20 %s^3 %s^5",
                        Schema.NAME, Schema.NAME_SPELL, Schema.NAME_EXACT, Schema.NAME_PREFIX,
                        Schema.SYNONYM, Schema.SYNONYM_SPELL, Schema.SYNONYM_EXACT, Schema.SYNONYM_PREFIX,
                        Schema.TEXT, Schema.TEXT_SPELL));
            q.add(DisMaxParams.QF, "%s^10 %s^18 %s^5 %s^6 %s^10 %s^3 %s^1 %s^2 %s^0.5",
                    Schema.NAME, Schema.NAME_SPELL, Schema.NAME_STUB, Schema.SYNONYM, Schema.SYNONYM_SPELL,
                    Schema.SYNONYM_STUB, Schema.TEXT, Schema.TEXT, Schema.TEXT_SPELL, Schema.TEXT_STUB);
            q.add("spellcheck", Boolean.toString(true));
            q.add(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
            q.add(SpellingParams.SPELLCHECK_COUNT, "100");
            q.add(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
            q.add("lowercaseOperators", Boolean.toString(false));
            q.add("defType", "edismax");
            QueryResponse resp = server.query(q);
            List<SolrDocument> results = resp.getResults();
            List<Phenotype> retval = new ArrayList<>(results.size());
            for (SolrDocument doc : results) {
                retval.add(mapper.fromDoc(doc));
            }
            return retval;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean getAutocommit()
    {
        return autocommit;
    }
    
    @Override
    public void setAutocommit(boolean autocommit)
    {
        this.autocommit = autocommit;
    }

    /**
     * Get the next available id.
     * @return the next id
     */
    private String getNextId() throws IOException
    {
        SolrQuery q = new SolrQuery().
            setQuery(WILDCARD_QSTRING).
            setRows(1).
            setSort(Schema.TIME_CREATED, SolrQuery.ORDER.desc);
        QueryResponse resp;
        try {
            resp = server.query(q);
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
        List<SolrDocument> results = resp.getResults();
        if (results.size() == 0) {
            return INITIAL_ID;
        }
        String latestId = (String) results.get(0).getFieldValue(Schema.ID);
        Matcher m = ID_PATTERN.matcher(latestId);
        if (!m.matches()) {
            throw new IllegalStateException("id " + latestId + " does not look like it should");
        }
        int number = Integer.parseInt(m.group(1));
        number++;
        String newId = String.format("NONHPO_%06d", number);
        return newId;
    }
}
