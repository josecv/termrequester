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

import java.util.Date;
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
import static org.junit.Assert.assertNotEquals;
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
    private CoreContainer cores = null;

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
    public void tearDown() throws IOException, InterruptedException
    {
        client.shutdown();
        if (cores != null) {
            cores.shutdown();
            cores = null;
        }
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
        startUpSolr();
        SolrQuery q = new SolrQuery().setQuery(SolrDatabaseService.WILDCARD_QSTRING);
        QueryResponse resp = solr.query(q);
        List<SolrDocument> results = resp.getResults();
        assertEquals(1, results.size());
        SolrDocument doc = results.get(0);
        assertEquals(result.getId().get(), doc.getFieldValue(Schema.ID));
        assertEquals(result.getName(), doc.getFieldValue(Schema.NAME));
        assertEquals(result.getDescription(), doc.getFieldValue(Schema.DEFINITION));
    }

    /**
     * Test that we can create more than one new document.
     */
    @Test
    public void testMultiCreate() throws IOException, SolrServerException
    {
        Phenotype pt1 = new Phenotype(PT_NAME, PT_DESC);
        Phenotype pt2 = new Phenotype(PT_NAME + PT_NAME, PT_DESC);
        Phenotype pt3 = new Phenotype(PT_NAME + PT_NAME + PT_NAME, PT_DESC);
        assertTrue(pt1 == client.savePhenotype(pt1));
        assertTrue(pt2 == client.savePhenotype(pt2));
        assertTrue(pt3 == client.savePhenotype(pt3));
        assertTrue(pt1.getId().isPresent());
        assertTrue(pt2.getId().isPresent());
        assertTrue(pt3.getId().isPresent());
        assertNotEquals(pt1.getId().get(), pt2.getId().get());
        assertNotEquals(pt1.getId().get(), pt3.getId().get());
        assertNotEquals(pt2.getId().get(), pt3.getId().get());
        startUpSolr();
        SolrQuery q = new SolrQuery().
            setQuery(SolrDatabaseService.WILDCARD_QSTRING).
            setSort(Schema.TIME_CREATED, SolrQuery.ORDER.asc);
        QueryResponse resp = solr.query(q);
        List<SolrDocument> results = resp.getResults();
        assertEquals(3, results.size());
        SolrDocument doc1 = results.get(0);
        SolrDocument doc2 = results.get(1);
        SolrDocument doc3 = results.get(2);
        assertEquals(pt1.getId().get(), doc1.getFieldValue(Schema.ID));
        assertEquals(pt2.getId().get(), doc2.getFieldValue(Schema.ID));
        assertEquals(pt3.getId().get(), doc3.getFieldValue(Schema.ID));
    }

    /**
     * Test that we can save and overwrite an existing document.
     */
    @Test
    public void testSaveExisting() throws IOException, SolrServerException
    {
        Phenotype pt = new Phenotype(PT_NAME, PT_DESC);
        assertTrue(pt == client.savePhenotype(pt));
        String id = pt.getId().get();
        Date timeCreated = pt.getTimeCreated().get();
        Date timeModified = pt.getTimeModified().get();
        String newName = "Name2";
        pt.setName(newName);
        assertTrue(pt == client.savePhenotype(pt));
        assertEquals(id, pt.getId().get());
        assertEquals(timeCreated, pt.getTimeCreated().get());
        assertNotEquals(timeModified, pt.getTimeModified().get());
        startUpSolr();
        SolrQuery q = new SolrQuery().setQuery(SolrDatabaseService.WILDCARD_QSTRING);
        List<SolrDocument> results = solr.query(q).getResults();
        assertEquals(1, results.size());
        SolrDocument doc = results.get(0);
        assertEquals(pt.getId().get(), doc.getFieldValue(Schema.ID));
        assertEquals(newName, doc.getFieldValue(Schema.NAME));

    }
    
    @Test
    public void testGetById() throws IOException
    {
        Phenotype pt = new Phenotype(PT_NAME, PT_DESC);
        client.savePhenotype(pt);
        Phenotype result = client.getPhenotypeById(pt.getId().get());
        assertEquals(pt.getId().get(), result.getId().get());
        assertEquals(pt.getName(), result.getName());
        assertEquals(pt.getDescription(), result.getDescription());
    }

    /**
     * Start up our own solr client. This is separate from usual start up to
     * make sure that our solr client doesn't mess with the instance being tested.
     * Will therefore shutdown the instance for the remainder of the test.
     */
    private void startUpSolr() throws IOException
    {
        client.shutdown();
        if (cores == null) {
            cores = new CoreContainer(folder.getRoot().toPath().resolve("solr").toString());
            cores.load();
            solr = new EmbeddedSolrServer(cores, SolrDatabaseService.CORE_NAME);
        }
    }
}
