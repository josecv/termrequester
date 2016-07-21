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

import java.io.IOException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.core.CoreContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.di.HPORequestModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the SolrDatabaseService class.
 *
 * @version $Id$
 */
public class SolrDatabaseServiceTest
{
    /**
     * The name of the test phenotype.
     */
    private static final String PT_NAME = "Test phenotype";

    /**
     * The description of the test phenotype.
     */
    private static final String PT_DESC = "Test phenotype description";

    /**
     * A dependency injector.
     */
    private static Injector injector;

    /**
     * The object under test.
     */
    private DatabaseService client;

    /**
     * The solr core container we're gonna be using.
     */
    private CoreContainer cores;

    /**
     * The solr client we'll use to verify the databaseservice did its job.
     */
    private SolrClient solr;

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Set up the suite.
     */
    @BeforeClass
    public static void beforeClass()
    {
        injector = Guice.createInjector(new HPORequestModule());
    }

    /**
     * Set up an individual test.
     */
    @Before
    public void setUp() throws IOException
    {
        client = injector.getInstance(SolrDatabaseService.class);
        client.init(folder.getRoot().toPath());
    }

    @After
    public void tearDown() throws IOException
    {
        client.shutdown();
    }

    /**
     * Test creating a new document.
     */
    @Test
    public void testCreate() throws IOException, SolrServerException
    {
        Phenotype pt = new Phenotype(PT_NAME, PT_DESC);
        Phenotype result = client.savePhenotype(pt);
        assertTrue(pt == result);
        assertTrue(result.getId().isPresent());
        assertTrue(result.getTimeCreated().isPresent());
        assertTrue(result.getTimeModified().isPresent());
        assertEquals(result.getTimeCreated(), result.getTimeModified());
        client.shutdown();
        startUpSolr();
        SolrQuery q = new SolrQuery().setQuery("*:*");
        QueryResponse resp = solr.query(q);
        List<SolrDocument> results = resp.getResults();
        assertEquals(1, results.size());
        SolrDocument doc = results.get(0);
        assertEquals(result.getId().get(), doc.getFieldValue(Schema.ID));
        assertEquals(result.getName(), doc.getFieldValue(Schema.NAME));
        assertEquals(result.getDescription(), doc.getFieldValue(Schema.DEFINITION));
    }

    /**
     * Start up our own solr client. This is separate from usual start up to
     * make sure that our solr client doesn't mess with the instance being tested.
     * In other words, should be called _after_ the instance has been shutdown() (to
     * check it did what it was supposed to).
     */
    private void startUpSolr() throws IOException
    {
        cores = new CoreContainer(folder.getRoot().toPath().resolve("solr").toString());
        cores.load();
        solr = new EmbeddedSolrServer(cores, SolrDatabaseService.CORE_NAME);
    }
}
