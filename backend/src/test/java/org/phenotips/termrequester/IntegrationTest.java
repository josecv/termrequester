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
package org.phenotips.termrequester;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.github.AbstractGithubTest;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.db.DatabaseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test of the phenotype manager that doesn't mock any components and just runs
 * everything.
 *
 * @version $Id$
 */
public class IntegrationTest extends AbstractGithubTest
{
    /**
     * The repository.
     */
    private static String repo;

    /**
     * The manager under test.
     */
    private PhenotypeManager client;

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Set up the suite.
     */
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        repo = initialize("integration");
    }

    /**
     * Tear down the suite.
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
        client = injector.getInstance(PhenotypeManager.class);
        client.init(new GithubAPI.Repository(USER, repo, TEST_TOKEN), folder.getRoot().toPath());
    }

    /**
     * Tear down the test.
     */
    @After
    public void tearDown() throws Exception
    {
        client.shutdown();
    }

    /**
     * Test that get by id works after closing the issue.
     */
    @Test
    public void testGetByIdAfterClose() throws Exception
    {
        Phenotype pt = new Phenotype("wow", "yay");
        client.createRequest(pt);
        injector.getInstance(DatabaseService.class).commit();
        assertTrue(pt.getIssueNumber().isPresent());
        closeIssue(repo, pt.getIssueNumber().get(), false);
        Phenotype pt2 = client.getPhenotypeById(pt.getId().get());
        assertTrue(pt.getIssueNumber().isPresent());
        assertTrue("No issue number", pt2.getIssueNumber().isPresent());
        assertEquals(pt.getIssueNumber().get(), pt2.getIssueNumber().get());
        assertEquals(Phenotype.Status.ACCEPTED, pt2.getStatus());

        pt2 = client.getPhenotypeById(pt.getId().get());
        assertTrue(pt.getIssueNumber().isPresent());
        assertTrue("No issue number", pt2.getIssueNumber().isPresent());
        assertEquals(pt.getIssueNumber().get(), pt2.getIssueNumber().get());
        assertEquals(Phenotype.Status.ACCEPTED, pt2.getStatus());
        closeIssue(repo, pt.getIssueNumber().get(), true);
    }
}
