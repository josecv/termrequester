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

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.commonSetUp();
        router.attach("/phenotype/{id}", finder.finder(PhenotypeResource.class));
    }


    /**
     * Test getting by id when there is a phenotype to get.
     */
    @Test
    public void testGetById() throws Exception
    {
        /* TODO Yay for code repetition. */
        PhenotypeManager manager = injector.getInstance(PhenotypeManager.class);
        manager.init(new GithubAPI.Repository("", "", ""), folder.getRoot().toPath());
        manager.createRequest(pt);
        databaseService.commit();
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
        /* TODO Yay for code repetition. */
        PhenotypeManager manager = injector.getInstance(PhenotypeManager.class);
        manager.init(new GithubAPI.Repository("", "", ""), folder.getRoot().toPath());
        manager.createRequest(pt);
        databaseService.commit();
        Request request = new Request(Method.GET, "/phenotype/" + pt.getId().get() + "1");
        Response response = new Response(request);
        router.handle(request, response);
        assertEquals(404, response.getStatus().getCode());
        assertFalse(response.isEntityAvailable());
    }
}
