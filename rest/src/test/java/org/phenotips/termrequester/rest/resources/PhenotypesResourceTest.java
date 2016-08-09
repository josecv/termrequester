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

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.PhenotypeManager;
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

import com.fasterxml.jackson.core.type.TypeReference;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the PhenotypesResource server resource.
 *
 * @version $Id$
 */
public class PhenotypesResourceTest extends AbstractResourceTest
{
    @Override
    public void doSetUp() throws Exception
    {
        router.attach("/phenotypes", finder.finder(PhenotypesResource.class));
    }

    @Test
    public void testCreate() throws Exception
    {
        doNothing().when(githubApi).openIssue(refEq(pt));
        String syn1 = "syn1";
        String syn2 = "syn2";
        String requestUri = "/phenotypes";
        String createJson = String.format("{ \"name\": \"%s\", " +
                "\"description\": \"%s\", " +
                "\"synonyms\": [\"%s\", \"%s\"], " +
                "\"parents\": [] }", PT_NAME, PT_DESC, syn1, syn2);
        StringRepresentation entity = new StringRepresentation(createJson, MediaType.APPLICATION_JSON);
        Request request = new Request(Method.POST, requestUri, entity);
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(201, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        assertEquals(MediaType.APPLICATION_JSON, response.getEntity().getMediaType());
        Phenotype result = mapper.readValue(response.getEntity().getStream(), Phenotype.class);
        verify(githubApi).openIssue(eq(pt));
        assertTrue(result.getId().isPresent());
        assertEquals(pt, result);
        assertEquals(PT_NAME, result.getName());
        assertEquals(PT_DESC, result.getDescription());
        assertTrue(result.getSynonyms().contains(syn1));
        assertTrue(result.getSynonyms().contains(syn2));
        assertEquals(2, result.getSynonyms().size());
    }

    @Test
    public void testSearch() throws Exception
    {
        saveAndInit(pt);
        Request request = new Request(Method.GET, "/phenotypes?text=liszt");
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        List<Phenotype> results = mapper.readValue(response.getEntity().getStream(),
                new TypeReference<List<Phenotype>>() { });
        assertEquals(1, results.size());
        assertEquals(pt, results.get(0));
    }

    @Test
    public void testEmptySearch() throws Exception
    {
        Request request = new Request(Method.GET, "/phenotypes");
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        List<Phenotype> results = mapper.readValue(response.getEntity().getStream(),
                new TypeReference<List<Phenotype>>() { });
        assertEquals(0, results.size());
    }
}
