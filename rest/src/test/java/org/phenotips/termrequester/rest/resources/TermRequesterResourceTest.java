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
package org.phenotips.termrequester.rest.resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.di.TermRequesterRESTModule;
import org.phenotips.termrequester.testutils.TestModule;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.ext.guice.FinderFactory;
import org.restlet.ext.guice.RestletGuice;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Router;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the TermRequesterResource server resource.
 *
 * @version $Id$
 */
public class TermRequesterResourceTest
{
    /* TODO This copies PhenotypeManagerTest in its set up. Not brilliant */

    /**
     * The phenotype name.
     */
    private static final String PT_NAME = "Franz Liszt";

    /**
     * The phenotype description.
     */
    private static final String PT_DESC = "Penguin";

    /**
     * The mocked database service.
     */
    private DatabaseService databaseService;

    /**
     * The mocked github api.
     */
    private GithubAPI githubApi;

    /**
     * The dependency injector in use.
     */
    private Injector injector;

    /**
     * A test phenotype.
     */
    private Phenotype pt;

    /**
     * The router.
     */
    private Router router;

    /**
     * An object mapper to deserialize from json.
     */
    private ObjectMapper mapper;

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception
    {
        githubApi = mock(GithubAPI.class);
        System.out.println(folder.getRoot().toString());
        injector = RestletGuice.createInjector(Modules.override(
                    new TermRequesterRESTModule("", "", "", folder.getRoot().toString())).
                with(new TestModule(null, githubApi)));
        FinderFactory finder = injector.getInstance(FinderFactory.class);
        router = new Router();
        //router.attach("/phenotypes", finder.finder(TermRequesterResource.class));
        router.attachDefault(finder.finder(TermRequesterResource.class));
        pt = new Phenotype(PT_NAME, PT_DESC);
        mapper = injector.getInstance(ObjectMapper.class);
        databaseService = injector.getInstance(DatabaseService.class);
    }

    @Test
    public void testCreate() throws Exception
    {
        when(githubApi.searchForIssue(refEq(pt))).thenReturn(Optional.<String>absent());
        doNothing().when(githubApi).openIssue(refEq(pt));
        String requestUri = "/phenotypes/create";
        String createJson = String.format("{ \"name\": \"%s\", " +
                "\"description\": \"%s\", " +
                "\"synonyms\": [], " +
                "\"parents\": [] }", PT_NAME, PT_DESC);
        StringRepresentation entity = new StringRepresentation(createJson, MediaType.APPLICATION_JSON);
        Request request = new Request(Method.POST, requestUri, entity);
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(201, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        assertEquals(MediaType.APPLICATION_JSON, response.getEntity().getMediaType());
        Phenotype result = mapper.readValue(response.getEntity().getStream(), Phenotype.class);
        System.out.println(response.getEntity().getText());
        verify(githubApi).openIssue(eq(pt));
        assertTrue(result.getId().isPresent());
        assertEquals(pt, result);
    }
}
