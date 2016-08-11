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

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.utils.IdUtils;
import org.phenotips.variantstore.db.DatabaseException;
import org.phenotips.variantstore.shared.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Manages a solr instance as a database.
 *
 * @version $Id : $
 */
@Singleton
class SolrDatabaseService implements DatabaseService
{
    /* Lots of this came from org.phenotips.variantstore.db.solr.SolrController */

    /**
     * A query string to match all docuemnts.
     */
    public static final String WILDCARD_QSTRING = "*:*";

    /**
     * The name of the solr core.
     */
    public static final String CORE_NAME = "termrequester";


    /**
     * A joiner to join different parts of a Solr query with an OR.
     */
    private static final Joiner OR_QUERY_JOINER = Joiner.on(' ').skipNulls();

    /**
     * A query string format to check for field equality.
     */
    private static final String FIELD_IS = "%s:\"%s\"";

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
    private boolean up;

    /**
     * Whether we should commit at the end of every write.
     */
    private boolean autocommit;

    /**
     * The solr mapper to use to turn phenotypes to documents and vice-versa.
     */
    private SolrMapper mapper;

    @Override
    public synchronized void init(Path path) throws IOException
    {
        /* Make sure initialization is idempotent */
        if (!up) {
            up = true;
            File resources;
            this.path = path.resolve("solr");
            try {
                ResourceManager.copyResourcesToPath(Paths.get("solr/"), this.path, SolrDatabaseService.class);
            } catch (DatabaseException e) {
                throw new IOException(e);
            }
            cores = new CoreContainer(this.path.toString());
            cores.load();
            server = new EmbeddedSolrServer(cores, CORE_NAME);
            if (server == null) {
                up = false;
                throw new IOException("Solr returned null server");
            }
            mapper = new SolrMapper();
        }
    }

    @Override
    public synchronized void shutdown() throws IOException
    {
        if (up) {
            commit();
            server.close();
            cores.shutdown();
            up = false;
        }
    }

    @Override
    public void commit() throws IOException
    {
        checkUp();
        try {
            server.commit();
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype savePhenotype(Phenotype pt) throws IOException
    {
        checkUp();
        /* Nothing to do, it hasn't been changed */
        if (!pt.isDirty()) {
            return pt;
        }
        if (pt.getId().isPresent()) {
            try {
                String id = pt.getId().get();
                checkState(server.getById(id) != null, "ID %s does not exist when expected to", id);
                server.deleteById(pt.getId().get());
            } catch (SolrServerException e) {
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
        checkUp();
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
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype getPhenotypeById(String id) throws IOException
    {
        checkUp();
        try {
            SolrDocument doc = server.getById(id);
            if (doc == null) {
                return Phenotype.NULL;
            }
            Phenotype pt = mapper.fromDoc(doc);
            return pt;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Phenotype getPhenotypeByIssueNumber(String issueNumber) throws IOException
    {
        checkUp();
        return getPhenotypeByField(Schema.ISSUE_NUMBER, issueNumber);
    }

    @Override
    public Phenotype getPhenotypeByHpoId(String hpoId) throws IOException
    {
        checkUp();
        String queryString = String.format("(%s) AND (%s)",
                String.format(FIELD_IS, Schema.STATUS, Phenotype.Status.ACCEPTED.toString()),
                String.format(FIELD_IS, Schema.HPO_ID, hpoId));
        SolrQuery q = new SolrQuery().setQuery(queryString).setRows(1);
        return runQuery(q);
    }

    @Override
    public Phenotype getPhenotype(Phenotype other) throws IOException
    {
        checkUp();
        Set<String> names = other.getSynonyms();
        names.add(other.getName());
        List<String> queryPieces = new ArrayList<>(names.size() * 2 + 2);
        if (other.getId().isPresent()) {
            queryPieces.add(String.format(FIELD_IS, Schema.ID,
                        ClientUtils.escapeQueryChars(other.getId().get())));
        }
        if (other.getIssueNumber().isPresent()) {
            queryPieces.add(String.format(FIELD_IS, Schema.ISSUE_NUMBER,
                        ClientUtils.escapeQueryChars(other.getIssueNumber().get())));
        }
        for (String name : names) {
            queryPieces.add(String.format(FIELD_IS, Schema.NAME_EXACT,
                        ClientUtils.escapeQueryChars(name)));
            queryPieces.add(String.format(FIELD_IS, Schema.SYNONYM_EXACT,
                        ClientUtils.escapeQueryChars(name)));
        }
        String queryString = OR_QUERY_JOINER.join(queryPieces);
        SolrQuery q = new SolrQuery().setQuery(queryString).setRows(1);
        return runQuery(q);
    }

    @Override
    public List<Phenotype> searchPhenotypes(String text) throws IOException
    {
        checkUp();
        try {
            SolrQuery q = new SolrQuery();
            String escaped = ClientUtils.escapeQueryChars(text);
            q.add(CommonParams.Q, escaped);
            q.add(SpellingParams.SPELLCHECK_Q, text);
            q.add(DisMaxParams.PF, String.format("%s^20 %s^36 %s^100 %s^30 %s^15 %s^25 %s^70 %s^20 %s^3 %s^5",
                        Schema.NAME, Schema.NAME_SPELL, Schema.NAME_EXACT, Schema.NAME_PREFIX,
                        Schema.SYNONYM, Schema.SYNONYM_SPELL, Schema.SYNONYM_EXACT, Schema.SYNONYM_PREFIX,
                        Schema.TEXT, Schema.TEXT_SPELL));
            String qstring = String.format("%s^10 %s^18 %s^5 %s^6 %s^10 %s^3 %s^1 %s^2 %s^0.5",
                    Schema.NAME, Schema.NAME_SPELL, Schema.NAME_STUB, Schema.SYNONYM, Schema.SYNONYM_SPELL,
                    Schema.SYNONYM_STUB, Schema.TEXT, Schema.TEXT, Schema.TEXT_SPELL, Schema.TEXT_STUB);
            qstring = addStatusFilter(qstring, Phenotype.Status.SYNONYM);
            q.add(DisMaxParams.QF, qstring);
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
    public List<Phenotype> getPhenotypesByStatus(Phenotype.Status status) throws IOException
    {
        checkUp();
        return getPhenotypesByField(Schema.STATUS, status.toString(), false);
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
     * Add a filter preventing the query given from returning phenotypes with the given status.
     *
     * @param qstring the query
     * @param status the status
     * @return the updated query
     */
    private String addStatusFilter(String qstring, Phenotype.Status status)
    {
        return String.format("( %s ) AND -( %s )", qstring,
                String.format(FIELD_IS, Schema.STATUS, status.toString()));
    }

    /**
     * Get one single phenotype where the field given has the value given.
     *
     * @param field the field
     * @param value the value
     * @return the phenotype
     */
    private Phenotype getPhenotypeByField(String field, String value)
        throws IOException
    {
        List<Phenotype> phenotypes = getPhenotypesByField(field, value, true);
        if (phenotypes.size() == 0) {
            return Phenotype.NULL;
        }
        return phenotypes.get(0);
    }

    /**
     * Get all the phenotypes where the field given has the value given.
     * @param field the field
     * @param value the value
     * @param limitOne whether to make sure only one result comes back
     * @return the results
     */
    private List<Phenotype> getPhenotypesByField(String field, String value, boolean limitOne)
        throws IOException
    {
        try {
            String queryString = String.format(FIELD_IS, field, value);
            SolrQuery q = new SolrQuery().
                setQuery(queryString);
            if (limitOne)
            {
                q = q.setRows(1);
            }
            QueryResponse resp = server.query(q);
            List<SolrDocument> documents = resp.getResults();
            List<Phenotype> results = new ArrayList<>(documents.size());
            for (SolrDocument doc : documents) {
                results.add(mapper.fromDoc(doc));
            }
            return results;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    /**
     * Run the query given and return one result from it.
     *
     * @param q the query
     * @throws IOException if solr throws
     */
    private Phenotype runQuery(SolrQuery q) throws IOException
    {
        try {
            QueryResponse resp = server.query(q);
            List<SolrDocument> results = resp.getResults();
            if (results.size() == 0) {
                return Phenotype.NULL;
            }
            return mapper.fromDoc(results.get(0));
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
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
            return IdUtils.INITIAL_ID;
        }
        String latestId = (String) results.get(0).getFieldValue(Schema.ID);
        return IdUtils.incrementId(latestId);
    }

    /**
     * Check that the solr is up and throw if it isn't.
     *
     * @throws IllegalStateException if it's down.
     */
    private void checkUp()
    {
        checkState(up, "Solr service is down");
    }
}
