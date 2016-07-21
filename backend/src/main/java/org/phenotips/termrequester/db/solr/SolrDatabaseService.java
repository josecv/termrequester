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

import java.util.Date;
import java.util.List;
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
import org.apache.solr.core.CoreContainer;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;

import com.google.common.base.Optional;
import com.google.inject.Singleton;

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
        if (pt.getId().isPresent()) {
            try {
                String id = pt.getId().get();
                checkState(server.getById(id) != null, "ID " + id + " does not exist on server when expected to");
                server.deleteById(pt.getId().get());
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
        commit();
        return pt;
    }

    @Override
    public boolean deletePhenotype(Phenotype pt)
    {
        throw new UnsupportedOperationException();
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
    public Phenotype getPhenotypeByIssueNumber(String issueNumber)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Phenotype getPhenotype(Phenotype other)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Phenotype> searchPhenotypes(String text)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the next available id.
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
