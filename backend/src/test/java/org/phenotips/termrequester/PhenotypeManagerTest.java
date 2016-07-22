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

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.di.HPORequestModule;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.github.GithubAPIFactory;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the PhenotypeManager implementation defined in HPORequestModule.
 *
 * @version $Id$
 */
public class PhenotypeManagerTest
{
    /**
     * The owner of our mock repo
     */
    private static final String OWNER = "";

    /**
     * The mock repo.
     */
    private static final String REPOSITORY = "";

    /**
     * The token to access the mock repo.
     */
    private static final String TOKEN = "";

    /**
     * The dependency injector in use.
     */
    private Injector injector;

    /**
     * The mocked database service.
     */
    private DatabaseService databaseService;

    /**
     * The mocked github api.
     */
    private GithubAPI githubApi;

    /**
     * The component under test.
     */
    private PhenotypeManager client;

    /**
     * A small example phenotype.
     */
    private Phenotype pt;

    /**
     * The name of the test phenotype.
     */
    private static final String PT_NAME = "Test Phenotype";

    /**
     * The description of the test phenotype.
     */
    private static final String PT_DESC = "Description";

    /**
     * The issue number for the test phenotype.
     */
    private static final String PT_NUM = "123";

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Set up the test case.
     */
    @Before
    public void setUp() throws IOException, TermRequesterBackendException
    {
        databaseService = mock(DatabaseService.class);
        githubApi = mock(GithubAPI.class);
        injector = Guice.createInjector(Modules.override(new HPORequestModule()).
                with(new TestModule(databaseService, githubApi)));
        client = injector.getInstance(PhenotypeManager.class);
        client.init(new GithubAPI.Repository(OWNER, REPOSITORY, TOKEN), folder.getRoot().toPath());
        pt = new Phenotype(PT_NAME, PT_DESC);
        when(databaseService.getPhenotypeById(any(String.class))).thenReturn(Phenotype.NULL);
        when(databaseService.getPhenotype(any(Phenotype.class))).thenReturn(Phenotype.NULL);
    }

    /**
     * Test that a new phenotype can be created.
     * TODO So very many cases are missing. Need more test cases.
     */
    @Test
    public void testCreation() throws InterruptedException, ExecutionException, IOException, TermRequesterBackendException
    {
        when(databaseService.savePhenotype(refEq(pt))).thenReturn(pt);
        when(githubApi.searchForIssue(refEq(pt))).thenReturn(Optional.<String>absent());
        doNothing().when(githubApi).openIssue(refEq(pt));
        Phenotype pt2 = client.createRequest(PT_NAME, new ArrayList<String>(), Optional.<String>absent(),
                                             Optional.of(PT_DESC));
        assertNotNull(pt2);
        assertEquals(PT_NAME, pt2.getName());
        assertEquals(PT_DESC, pt2.getDescription());
        verify(databaseService).savePhenotype(refEq(pt, "parent"));
        verify(githubApi).openIssue(refEq(pt, "parent"));
    }

    /**
     * Test the getPhenotypeById method.
     */
    @Test
    public void testGetById() throws TermRequesterBackendException, IOException
    {
        String id = "NONHPO_123";
        pt = mock(Phenotype.class);
        when(databaseService.getPhenotypeById(id)).thenReturn(pt);
        when(pt.getIssueNumber()).thenReturn(Optional.of(PT_NUM));
        Phenotype pt2 = client.getPhenotypeById(id);
        assertEquals(pt, pt2);
        verify(githubApi).readPhenotype(same(pt));
        verify(databaseService).getPhenotypeById(id);
        verify(databaseService).savePhenotype(same(pt));
    }

    /**
     * Test the search method.
     */
    @Test
    public void testSearch() throws TermRequesterBackendException, IOException
    {
        String text = "text search!";
        List<Phenotype> phenotypes = new ArrayList<>();
        when(databaseService.searchPhenotypes(text)).thenReturn(phenotypes);
        List<Phenotype> results = client.search(text);
        verify(databaseService).searchPhenotypes(text);
        assertEquals(phenotypes, results);
    }

    /**
     * Sets up all of our mocked objects, binds them to their corresponding interfaces.
     */
    private static final class TestModule extends AbstractModule
    {
        /**
         * The mocked database service.
         */
        private DatabaseService databaseService;

        /**
         * The mocked github api.
         */
        private GithubAPI githubApi;

        /**
         * Create a new TestModule instance.
         * @param dbs the database service to inject
         * @param github the github service to inject
         */
        public TestModule(DatabaseService dbs, GithubAPI github)
        {
            this.databaseService = dbs;
            this.githubApi = github;
        }

        @Override
        public void configure()
        {
            GithubAPIFactory factory = mock(GithubAPIFactory.class);
            when(factory.create(any(GithubAPI.Repository.class))).thenReturn(githubApi);
            bind(GithubAPIFactory.class).toInstance(factory);
            bind(DatabaseService.class).toInstance(databaseService);
        }
    }
}
