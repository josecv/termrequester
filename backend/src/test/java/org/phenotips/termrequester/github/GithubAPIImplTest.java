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
package org.phenotips.termrequester.github;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.phenotips.termrequester.Phenotype;

import com.fasterxml.jackson.core.type.TypeReference;

import com.google.common.base.Optional;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the GithubAPIImpl class.
 * Agonizingly, this class sometimes fails, mostly because github produces an imperfect
 * response or doesn't update fast enough for our test. Ideally we should be able to
 * automatically re-run once it right after failing out, so that a false negative
 * doesn't fail the entire build.
 *
 * @version $Id$
 */
public class GithubAPIImplTest extends AbstractGithubTest
{
    /* You'll notice there's a fair few sleep()s here. Unfortunately, github takes a little
     * while to sync changes so we have to pause to make sure we don't get a false negative
     * just because something wasn't updated on their end */


    /**
     * The factory to build clients.
     */
    private static GithubAPIFactory factory;

    /**
     * The github repository we're hitting.
     */
    private static String repo;

    /**
     * The client under test.
     */
    private GithubAPI client;

    /**
     * A test phenotype.
     */
    private Phenotype pt;

    /**
     * The issues to cleanup at the end.
     */
    private List<String> cleanupIssues;

    /**
     * Set up the suite, including creation of a new github repository for the test.
     */
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        repo = initialize("githubapi");
        factory = injector.getInstance(GithubAPIFactoryImpl.class);
    }

    /**
     * Delete the github repository the test was using.
     */
    @AfterClass
    public static void afterClass() throws Exception
    {
        shutdown(repo);
    }

    /**
     * Set up the test.
     */
    @Before
    public void setUp() throws Exception
    {
        client = factory.create(new GithubAPI.Repository(USER, repo, TEST_TOKEN));
        pt = new Phenotype("unittest", "Issue autogenerated by unit test in org.phenotips.termrequester.github.GithubAPIImplTest");
        pt.setId("id");
        pt.addSynonym("one");
        pt.addSynonym("two");
        cleanupIssues = new ArrayList<>();
    }

    /**
     * Tear down the test, closing any issues we opened.
     */
    @After
    public void tearDown() throws Exception
    {
        for (String issue : cleanupIssues) {
            closeIssue(repo, issue, true);
        }
    }

    /**
     * Test that a new issue can be created for a simple phenotype.
     */
    @Test
    public void testOpenIssue() throws Exception
    {
        String expectedDescription = Issue.describe(pt);
        client.openIssue(pt);
        assertTrue(pt.getIssueNumber().isPresent());
        String number = pt.getIssueNumber().get();
        cleanupIssues.add(number);
        Thread.sleep(2000);
        String endpoint = String.format("https://api.github.com/repos/%s/%s/issues/%s", USER, repo, number);
        System.out.println(endpoint);
        InputStream is = Request.Get(endpoint).
            addHeader("Authorization", "token " + TEST_TOKEN).
            execute().returnContent().asStream();
        Issue issue = mapper.readValue(is, Issue.class);
        assertEquals(expectedDescription, issue.getBody());
        assertEquals("open", issue.getState());
    }

    /**
     * Test that we can't open two issues with the same name.
     */
    @Test
    public void testNoReopen() throws Exception
    {
        client.openIssue(pt);
        cleanupIssues.add(pt.getIssueNumber().get());
        Thread.sleep(2000);
        Phenotype pt2 = new Phenotype(pt.getName(), "");
        try {
            client.openIssue(pt2);
            fail("Did not throw on repeat issue");
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Test the patch issue method.
     */
    @Test
    public void testPatchIssue() throws Exception
    {
        client.openIssue(pt);
        String number = pt.getIssueNumber().get();
        cleanupIssues.add(number);
        Thread.sleep(2000);
        pt.setName("different name!");
        String expectedDescription = Issue.describe(pt);
        String etag = pt.getEtag();
        client.patchIssue(pt);
        Thread.sleep(2000);
        String endpoint = String.format("https://api.github.com/repos/%s/%s/issues/%s", USER, repo, number);
        InputStream is = Request.Get(endpoint).
            addHeader("Authorization", "token " + TEST_TOKEN).
            execute().returnContent().asStream();
        Issue issue = mapper.readValue(is, Issue.class);
        assertEquals(expectedDescription, issue.getBody());
        assertNotNull(etag, pt.getEtag());
    }

    /**
     * Test that the searchForIssue method works.
     */
    @Test
    public void testSearchForIssue() throws Exception
    {
        assertFalse(client.searchForIssue(pt).isPresent());
        client.openIssue(pt);
        cleanupIssues.add(pt.getIssueNumber().get());
        Optional<String> number = client.searchForIssue(pt);
        assertTrue(number.isPresent());
        assertEquals(pt.getIssueNumber().get(), number.get());
        Phenotype pt2 = new Phenotype(pt.getName(), pt.getDescription());
        Thread.sleep(3000);
        number = client.searchForIssue(pt2);
        assertTrue(number.isPresent());
        assertEquals(pt.getIssueNumber().get(), number.get());
    }

    /**
     * Test we can't create an issue twice.
     */
    @Test
    public void testNoIssueRecreation() throws Exception
    {
        client.openIssue(pt);
        cleanupIssues.add(pt.getIssueNumber().get());
        try {
            client.openIssue(pt);
            fail("Did not fail on issue re-open");
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Test that the status is properly updated.
     */
    @Test
    public void testReadPhenotype() throws Exception
    {
        Phenotype original = new Phenotype(pt.getName(), pt.getDescription());
        client.openIssue(pt);
        String number = pt.getIssueNumber().get();
        assertEquals(Phenotype.Status.SUBMITTED, pt.getStatus());
        closeIssue(repo, pt.getIssueNumber().get(), false);
        client.readPhenotype(pt);
        assertEquals(original.getName(), pt.getName());
        assertEquals(original.getDescription(), pt.getDescription());
        assertEquals(Phenotype.Status.ACCEPTED, pt.getStatus());
        assertEquals(number, pt.getIssueNumber().get());
        closeIssue(repo, pt.getIssueNumber().get(), true);
    }

    /**
     * Test that we won't re-read the phenotype if it's not necessary.
     */
    @Test
    public void testNoReRead() throws Exception
    {
        client.openIssue(pt);
        cleanupIssues.add(pt.getIssueNumber().get());
        Phenotype ptSpy = spy(pt);
        client.readPhenotype(ptSpy);
        verify(ptSpy, never()).setStatus(any(Phenotype.Status.class));
        verify(ptSpy, never()).setEtag(any(String.class));
    }

    /**
     * Test that rewriting the phenotype doesn't cause trouble.
     */
    @Test
    public void testNoReWrite() throws Exception
    {
        client.openIssue(pt);
        cleanupIssues.add(pt.getIssueNumber().get());
        String etag = pt.getEtag();
        client.patchIssue(pt);
        System.out.println(pt.getEtag());
        assertEquals(etag, pt.getEtag());
    }

    /**
     * Test that an exception gets thrown if the repository doesn't exist.
     */
    @Test
    public void testExceptionThrownOn404() throws Exception
    {
        GithubAPI other = factory.create(new GithubAPI.Repository(USER, "imaginaryRepo", TEST_TOKEN));
        try {
            other.openIssue(pt);
            fail("Did not throw on non-existing repo");
        } catch (GithubException e) {

        }
    }
}
