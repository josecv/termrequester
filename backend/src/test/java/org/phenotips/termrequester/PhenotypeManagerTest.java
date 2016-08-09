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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.phenotips.termrequester.TermRequesterBackendModule;
import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.github.GithubAPIFactory;
import org.phenotips.termrequester.testutils.TestModule;
import org.phenotips.termrequester.utils.IdUtils;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the PhenotypeManager implementation defined in TermRequesterBackendModule.
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
     * The id of the test phenotype.
     */
    private static final String PT_ID = String.format(IdUtils.ID_FORMAT, 45);

    /**
     * The HPO id of the test phenotype.
     */
    private static final String PT_HPO_ID = "HPO_001234";

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Set up the test case.
     */
    @Before
    public void setUp() throws Exception
    {
        databaseService = mock(DatabaseService.class);
        githubApi = mock(GithubAPI.class);
        injector = Guice.createInjector(Modules.override(new TermRequesterBackendModule()).
                with(new TestModule(databaseService, githubApi)));
        client = injector.getInstance(PhenotypeManager.class);
        client.init(new GithubAPI.Repository(OWNER, REPOSITORY, TOKEN), folder.getRoot().toPath());
        pt = new Phenotype(PT_NAME, PT_DESC);
        when(databaseService.getPhenotypeById(any(String.class))).thenReturn(Phenotype.NULL);
        when(databaseService.getPhenotype(any(Phenotype.class))).thenReturn(Phenotype.NULL);
        when(githubApi.searchForIssue(any(Phenotype.class))).thenReturn(Optional.<String>absent());
    }

    @After
    public void tearDown() throws Exception
    {
        client.shutdown();
    }

    /**
     * Test that a new phenotype can be created.
     * TODO So very many cases are missing. Need more test cases.
     */
    @Test
    public void testCreation() throws Exception
    {
        when(databaseService.savePhenotype(refEq(pt))).thenReturn(pt);
        doNothing().when(githubApi).openIssue(refEq(pt));
        PhenotypeManager.PhenotypeCreation created = client.createRequest(pt);
        Phenotype pt2 = created.phenotype;
        assertNotNull(pt2);
        assertEquals(PT_NAME, pt2.getName());
        assertEquals(PT_DESC, pt2.getDescription());
        assertTrue(created.isNew);
        verify(databaseService).savePhenotype(refEq(pt));
        verify(githubApi).openIssue(refEq(pt));
    }

    /**
     * Test that a new phenotype can be "created" when it's already in the database and github.
     */
    @Test
    public void testAlreadyExisting() throws Exception
    {
        Phenotype pt2 = new Phenotype("Another", "another!");
        pt2.setId(PT_ID);
        pt2.setIssueNumber(PT_NUM);
        pt2.setStatus(Phenotype.Status.SUBMITTED);
        pt2 = spy(pt2);
        pt = spy(pt);
        when(databaseService.getPhenotype(refEq(pt))).thenReturn(pt2);
        PhenotypeManager.PhenotypeCreation created = client.createRequest(pt);
        assertFalse(created.isNew);
        assertTrue(pt2 == created.phenotype);
        verify(pt2).mergeWith(refEq(pt));
        verify(databaseService).savePhenotype(refEq(pt2));
        /* The issue is already submitted to github so nothing should've been opened */
        verify(githubApi, never()).openIssue(any(Phenotype.class));
        verify(githubApi).patchIssue(refEq(pt2));
    }

    /**
     * Test that a new phenotype can be created when it's already in the database, but not
     * github.
     */
    @Test
    public void testAlreadyInDb() throws Exception
    {
        Phenotype pt2 = new Phenotype("Another", "another");
        pt2.setId(PT_ID);
        pt2 = spy(pt2);
        pt = spy(pt);
        when(databaseService.getPhenotype(refEq(pt))).thenReturn(pt2);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Phenotype arg = (Phenotype) invocation.getArguments()[0];
                arg.setIssueNumber(PT_NUM);
                arg.setStatus(Phenotype.Status.SUBMITTED);
                return null;
            }
        }).when(githubApi).openIssue(refEq(pt2));
        PhenotypeManager.PhenotypeCreation created = client.createRequest(pt);
        assertTrue(pt2 == created.phenotype);
        assertFalse(created.isNew);
        verify(pt2).mergeWith(refEq(pt));
        verify(databaseService).savePhenotype(refEq(pt2));
        verify(githubApi).openIssue(refEq(pt2));
    }

    /**
     * Test the getPhenotypeById method.
     */
    @Test
    public void testGetById() throws Exception
    {
        pt = mock(Phenotype.class);
        when(databaseService.getPhenotypeById(PT_ID)).thenReturn(pt);
        when(pt.getIssueNumber()).thenReturn(Optional.of(PT_NUM));
        when(pt.getStatus()).thenReturn(Phenotype.Status.SUBMITTED);
        Phenotype pt2 = client.getPhenotypeById(PT_ID);
        assertEquals(pt, pt2);
        verify(githubApi).readPhenotype(same(pt));
        verify(databaseService).getPhenotypeById(PT_ID);
        verify(databaseService).savePhenotype(same(pt));
    }

    /**
     * Test the getPhenotypeById method with an hpo id.
     */
    @Test
    public void testGetByHpoId() throws Exception
    {
        pt = mock(Phenotype.class);
        when(databaseService.getPhenotypeByHpoId(PT_HPO_ID)).thenReturn(pt);
        when(pt.getIssueNumber()).thenReturn(Optional.of(PT_NUM));
        when(pt.getStatus()).thenReturn(Phenotype.Status.ACCEPTED);
        when(pt.getHpoId()).thenReturn(Optional.of(PT_HPO_ID));
        when(pt.getId()).thenReturn(Optional.of(PT_ID));
        Phenotype pt2 = client.getPhenotypeById(PT_HPO_ID);
        assertEquals(pt, pt2);
        verify(githubApi).readPhenotype(same(pt));
        verify(databaseService, never()).getPhenotypeById(PT_ID);
        verify(databaseService).getPhenotypeByHpoId(PT_HPO_ID);
        verify(databaseService).savePhenotype(same(pt));
    }

    /**
     * Test that things work properly when the phenotype is accepted as a
     * synonym.
     */
    @Test
    public void testAsSynonym() throws Exception
    {
        pt.setId(PT_ID);
        pt.setStatus(Phenotype.Status.SUBMITTED);
        pt.setIssueNumber(PT_NUM);
        Phenotype existing = spy(new HPOPhenotype("Already there", "yes"));
        existing.setStatus(Phenotype.Status.ACCEPTED);
        existing.setHpoId(PT_HPO_ID);
        when(databaseService.getPhenotypeByHpoId(PT_HPO_ID)).thenReturn(existing);
        when(databaseService.getPhenotypeById(PT_ID)).thenReturn(pt);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Phenotype arg = (Phenotype) invocation.getArguments()[0];
                arg.setStatus(Phenotype.Status.SYNONYM);
                arg.setHpoId(PT_HPO_ID);
                return null;
            }
        }).when(githubApi).readPhenotype(same(pt));
        Phenotype pt2 = client.getPhenotypeById(PT_ID);
        assertTrue("Wrong value returned", pt2 == existing);
        verify(databaseService, times(2)).getPhenotypeByHpoId(PT_HPO_ID);
        verify(existing).mergeWith(same(pt));
        verify(databaseService).savePhenotype(same(existing));
    }

    /**
     * Test that things work out when the phenotype is accepted as a previously
     * published synoynm that is not in the database.
     */
    @Test
    public void testAsPublishedSynonym() throws Exception
    {
        pt.setId(PT_ID);
        pt.setStatus(Phenotype.Status.SUBMITTED);
        pt.setIssueNumber(PT_NUM);
        when(databaseService.getPhenotypeByHpoId(PT_HPO_ID)).thenReturn(Phenotype.NULL);
        when(databaseService.getPhenotypeById(PT_ID)).thenReturn(pt);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Phenotype arg = (Phenotype) invocation.getArguments()[0];
                arg.setStatus(Phenotype.Status.SYNONYM);
                arg.setHpoId(PT_HPO_ID);
                return null;
            }
        }).when(githubApi).readPhenotype(same(pt));
        Phenotype pt2 = client.getPhenotypeById(PT_ID);
        verify(databaseService, times(2)).getPhenotypeByHpoId(PT_HPO_ID);
        assertEquals(Phenotype.Status.PUBLISHED, pt2.getStatus());
        assertEquals(PT_HPO_ID, pt2.getHpoId().get());
    }

    /**
     * Test the search method.
     */
    @Test
    public void testSearch() throws Exception
    {
        String text = "text search!";
        List<Phenotype> phenotypes = new ArrayList<>();
        when(databaseService.searchPhenotypes(text)).thenReturn(phenotypes);
        List<Phenotype> results = client.search(text);
        verify(databaseService).searchPhenotypes(text);
        assertEquals(phenotypes, results);
    }

    /**
     * Test the sync method.
     */
    @Test
    public void testSync() throws Exception
    {
        Phenotype pt2 = new Phenotype("test2", "test2");
        Phenotype pt3 = new Phenotype("test3", "test3");
        Phenotype pt4 = new Phenotype("test4", "test4");
        pt.setStatus(Phenotype.Status.SUBMITTED);
        pt2.setStatus(Phenotype.Status.SUBMITTED);
        pt3.setStatus(Phenotype.Status.SUBMITTED);
        pt4.setStatus(Phenotype.Status.SUBMITTED);
        List<Phenotype> submitted = new ArrayList<>();
        submitted.add(pt);
        submitted.add(pt2);
        submitted.add(pt3);
        submitted.add(pt4);
        when(databaseService.getPhenotypesByStatus(Phenotype.Status.SUBMITTED)).thenReturn(submitted);
        client.syncPhenotypes();
        verify(githubApi, times(4)).readPhenotype(any(Phenotype.class));
        verify(githubApi).readPhenotype(same(pt));
        verify(githubApi).readPhenotype(same(pt2));
        verify(githubApi).readPhenotype(same(pt3));
        verify(githubApi).readPhenotype(same(pt4));
        verify(databaseService, times(4)).savePhenotype(any(Phenotype.class));
        verify(databaseService).savePhenotype(same(pt));
        verify(databaseService).savePhenotype(same(pt2));
        verify(databaseService).savePhenotype(same(pt3));
        verify(databaseService).savePhenotype(same(pt4));
        verify(databaseService).commit();
    }
}
