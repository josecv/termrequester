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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.PhenotypeManager;
import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.di.TermRequesterRESTModule;
import org.phenotips.termrequester.testutils.TestModule;
import org.phenotips.termrequester.utils.IdUtils;

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
import static org.junit.Assert.assertFalse;
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
 * Test the PhenotypeResource server resource.
 *
 * @version $Id$
 */
public class PhenotypeResourceTest extends AbstractResourceTest
{
    /**
     * A test hpo id.
     */
    private static final String HPO_ID = "HPO_000563";

    /**
     * A test issue number.
     */
    private static final String ISSUE_NUMBER = "123";

    @Override
    public void doSetUp() throws Exception
    {
        router.attach("/phenotype/{id}", finder.finder(PhenotypeResource.class));
    }

    /**
     * Test getting by id when there is a phenotype to get.
     */
    @Test
    public void testGetById() throws Exception
    {
        saveAndInit(pt);
        Request request = new Request(Method.GET, "/phenotype/" + pt.getId().get());
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        Phenotype result = mapper.readValue(response.getEntity().getStream(), Phenotype.class);
        assertEquals(pt, result);
    }

    /**
     * Test that getting by id with the wrong id doesn't work.
     */
    @Test
    public void testGetById404() throws Exception
    {
        saveAndInit(pt);
        databaseService.commit();
        String badId = IdUtils.incrementId(pt.getId().get());
        Request request = new Request(Method.GET, "/phenotype/" + badId);
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(404, response.getStatus().getCode());
        assertFalse(response.isEntityAvailable());
    }

    /**
     * Test that we can get a phenotype by hpo id.
     */
    @Test
    public void testGetByHpoId() throws Exception
    {
        saveAndInit(pt);
        databaseService.commit();
        pt.setStatus(Phenotype.Status.ACCEPTED);
        pt.setIssueNumber(ISSUE_NUMBER);
        pt.setHpoId(HPO_ID);
        databaseService.savePhenotype(pt);
        databaseService.commit();
        Request request = new Request(Method.GET, "/phenotype/" + HPO_ID);
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        Phenotype result = mapper.readValue(response.getEntity().getStream(), Phenotype.class);
        assertEquals(pt, result);
    }

    /**
     * Test that synonyms return a working redirect.
     */
    @Test
    public void testRedirect() throws Exception
    {
        saveAndInit(pt);
        databaseService.commit();
        pt.setStatus(Phenotype.Status.ACCEPTED);
        pt.setHpoId(HPO_ID);
        pt.setIssueNumber(ISSUE_NUMBER);
        databaseService.savePhenotype(pt);

        Phenotype pt2 = new Phenotype("Sibelius", "hooray");
        pt2.setStatus(Phenotype.Status.SYNONYM);
        pt2.setHpoId(HPO_ID);
        pt2.setIssueNumber(ISSUE_NUMBER + "1");
        manager.createRequest(pt2);
        databaseService.commit();

        Request request = new Request(Method.GET, "/phenotype/" + pt2.getId().get());
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(301, response.getStatus().getCode());

        String reference = response.getLocationRef().toString();
        request = new Request(Method.GET, reference);
        response = new Response(request);
        router.handle(request, response);
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.isEntityAvailable());
        Phenotype result = mapper.readValue(response.getEntity().getStream(), Phenotype.class);
        assertEquals(pt, result);
    }

    /**
     * Test that we get a 400 on a malformed id.
     */
    @Test
    public void testMalformedId() throws Exception
    {
        Request request = new Request(Method.GET, "/phenotype/yes_lad");
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(400, response.getStatus().getCode());
        assertFalse(response.isEntityAvailable());
    }
}
