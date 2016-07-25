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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.phenotips.termrequester.di.TermRequesterBackendModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    private static final String PT_DESC = "Descriptions are cool";

    /**
     * The issue number of the test phenotype.
     */
    private static final String PT_NUM = "1";

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
        injector = Guice.createInjector(new TermRequesterBackendModule());
    }

    /**
     * Set up an individual test.
     */
    @Before
    public void setUp() throws IOException
    {
        client = injector.getInstance(SolrDatabaseService.class);
        client.init(folder.getRoot().toPath());
        client.setAutocommit(true);
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
    
    /**
     * Test the get by id method.
     */
    @Test
    public void testGetById() throws IOException
    {
        Phenotype pt = new Phenotype(PT_NAME, PT_DESC);
        pt.addSynonym("synone");
        pt.addSynonym("syntwo");
        client.savePhenotype(pt);
        Phenotype result = client.getPhenotypeById(pt.getId().get());
        assertEquals(pt.getId().get(), result.getId().get());
        assertEquals(pt.getName(), result.getName());
        assertEquals(pt.getDescription(), result.getDescription());
        assertEquals(pt.getTimeCreated(), result.getTimeCreated());
        assertEquals(pt.getTimeModified(), result.getTimeModified());
        assertEquals(pt.getSynonyms(), result.getSynonyms());
        result = client.getPhenotypeById("imaginary");
        assertEquals(Phenotype.NULL, result);
    }

    /**
     * Test the deletePhenotype method.
     */
    @Test
    public void testDelete() throws IOException, SolrServerException
    {
        Phenotype victim = new Phenotype(PT_NAME, PT_DESC);
        Phenotype other = new Phenotype(PT_NAME + " yes yes", PT_DESC);
        client.savePhenotype(victim);
        client.savePhenotype(other);
        assertTrue(client.deletePhenotype(victim));
        assertFalse(client.deletePhenotype(victim));
        startUpSolr();
        SolrQuery q = new SolrQuery().setQuery(SolrDatabaseService.WILDCARD_QSTRING);
        List<SolrDocument> results = solr.query(q).getResults();
        assertEquals(1, results.size());
        SolrDocument doc = results.get(0);
        assertEquals(other.getId().get(), doc.getFieldValue(Schema.ID));
        assertEquals(other.getName(), doc.getFieldValue(Schema.NAME));
    }

    /**
     * Test the getPhenotypeByIssueNumber method.
     */
    @Test
    public void testGetByIssueNumber() throws IOException
    {
        Phenotype pt1 = new Phenotype(PT_NAME, PT_DESC);
        pt1.setIssueNumber(PT_NUM);
        pt1.setStatus(Phenotype.Status.SUBMITTED);
        Phenotype pt2 = new Phenotype(PT_NAME + " other", PT_DESC);
        /* Wanna be extra sure substrings won't match */
        pt2.setIssueNumber(PT_NUM + "0");
        pt2.setStatus(Phenotype.Status.SUBMITTED);
        client.savePhenotype(pt1);
        client.savePhenotype(pt2);
        Phenotype result = client.getPhenotypeByIssueNumber(pt1.getIssueNumber().get());
        assertEquals(pt1.getId().get(), result.getId().get());
        assertEquals(pt1.getName(), result.getName());
        assertEquals(pt1.getIssueNumber().get(), result.getIssueNumber().get());
        result = client.getPhenotypeByIssueNumber(pt2.getIssueNumber().get());
        assertEquals(pt2.getId().get(), result.getId().get());
        assertEquals(pt2.getName(), result.getName());
        assertEquals(pt2.getIssueNumber().get(), result.getIssueNumber().get());
        result = client.getPhenotypeByIssueNumber(PT_NUM + "2");
        assertEquals(Phenotype.NULL, result);
    }

    /**
     * Test the getPhenotype method.
     */
    @Test
    public void testGetPhenotype() throws IOException
    {
        /* All four of these should be the same one:
         *  pt2 has pt1 as synonym
         *  pt1 has pt3 as synonym
         *  pt1 and pt4 have the same name
         *  pt1 and pt5 have the same id 
         *  pt1 and pt6 have the same issueNumber
         */
        Phenotype pt1 = new Phenotype(PT_NAME, PT_DESC);
        pt1.setStatus(Phenotype.Status.SUBMITTED);
        pt1.setIssueNumber(PT_NUM);
        Phenotype pt2 = new Phenotype("Mahler", PT_DESC);
        Phenotype pt3 = new Phenotype("Stravinsky", PT_DESC);
        Phenotype pt4 = new Phenotype(PT_NAME, PT_DESC);
        Phenotype pt5 = new Phenotype("Schoenberg", PT_DESC);
        Phenotype pt6 = new Phenotype("Rachmaninoff", PT_DESC);
        pt2.addSynonym(pt1.getName());
        pt1.addSynonym(pt3.getName());
        client.savePhenotype(pt1);
        pt5.setId(pt1.getId().get());
        pt6.setIssueNumber(pt1.getIssueNumber().get());
        pt6.setStatus(pt1.getStatus());
        Phenotype result = client.getPhenotype(pt1);
        assertEquals(pt1, result);
        result = client.getPhenotype(pt2);
        assertEquals(pt1, result);
        result = client.getPhenotype(pt3);
        assertEquals(pt1, result);
        result = client.getPhenotype(pt4);
        assertEquals(pt1, result);
        result = client.getPhenotype(pt5);
        assertEquals(pt1, result);
        result = client.getPhenotype(pt6);
        assertEquals(pt1, result);
        /* This one isn't equal to the others */
        Phenotype pt7 = new Phenotype("Ravel", PT_DESC);
        result = client.getPhenotype(pt7);
        assertEquals(Phenotype.NULL, result);
    }

    /**
     * Test the searchPhenotypes function.
     */
    @Test
    public void testSearch() throws IOException
    {
        String s1 = "hooray",
               s2 = "string",
               s3 = "bam",
               s4 = "commas",
               s5 = "more variables";
        Phenotype pt1 = new Phenotype(PT_NAME, PT_DESC),
                  pt2 = new Phenotype(PT_NAME + PT_NAME, PT_DESC),
                  pt3 = new Phenotype(PT_NAME, s1),
                  pt4 = new Phenotype(s1, s1),
                  pt5 = new Phenotype(s1, s2),
                  pt6 = new Phenotype(s2, s2),
                  pt7 = new Phenotype(s3, s3),
                  pt8 = new Phenotype(s4, s4);
        client.savePhenotype(pt1);
        client.savePhenotype(pt2);
        client.savePhenotype(pt3);
        client.savePhenotype(pt4);
        client.savePhenotype(pt5);
        client.savePhenotype(pt6);
        client.savePhenotype(pt7);
        client.savePhenotype(pt8);
        List<Phenotype> results = client.searchPhenotypes(PT_NAME);
        assertSetEquals(results, pt1, pt2, pt3);
        results = client.searchPhenotypes(PT_DESC);
        assertSetEquals(results, pt1, pt2);
        results = client.searchPhenotypes(s2);
        assertSetEquals(results, pt6, pt5);
        results = client.searchPhenotypes(s3);
        assertSetEquals(results, pt7);
        results = client.searchPhenotypes(s5);
        assertSetEquals(results);
    }

    private void assertSetEquals(Collection<Phenotype> results, Phenotype... expected)
    {
        Set<Phenotype> resultSet = new HashSet<>(results);
        Set<Phenotype> expectedSet = new HashSet<>(Arrays.asList(expected));
        assertEquals(expectedSet, resultSet);
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
