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
 * Test any given resource.
 * Contains some useful stuff such as a dependency injector, a PhenotypeManager, sample phenotype, etc.
 * To use, override doSetUp() with your own routes, etc.
 *
 * @version $Id$
 */
public abstract class AbstractResourceTest
{
    /* TODO This copies PhenotypeManagerTest in its set up. Not brilliant */

    /**
     * The phenotype name.
     */
    protected static final String PT_NAME = "Franz Liszt";

    /**
     * The phenotype description.
     */
    protected static final String PT_DESC = "Penguin";

    /**
     * The mocked database service.
     */
    protected DatabaseService databaseService;

    /**
     * The mocked github api.
     */
    protected GithubAPI githubApi;

    /**
     * The dependency injector in use.
     */
    protected Injector injector;

    /**
     * A test phenotype.
     */
    protected Phenotype pt;

    /**
     * The router.
     */
    protected Router router;

    /**
     * An object mapper to deserialize from json.
     */
    protected ObjectMapper mapper;

    /**
     * The finder factory to get resources.
     */
    protected FinderFactory finder;

    /**
     * A temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Do your own set up. Should prepare any routes you need.
     */
    protected abstract void doSetUp() throws Exception;

    /**
     * Sets up the test.
     */
    @Before
    public void setUp() throws Exception
    {
        githubApi = mock(GithubAPI.class);
        System.out.println(folder.getRoot().toString());
        injector = RestletGuice.createInjector(Modules.override(
                    new TermRequesterRESTModule("", "", "", folder.getRoot().toString())).
                with(new TestModule(null, githubApi)));
        finder = injector.getInstance(FinderFactory.class);
        router = new Router();
        pt = new Phenotype(PT_NAME, PT_DESC);
        mapper = injector.getInstance(ObjectMapper.class);
        databaseService = injector.getInstance(DatabaseService.class);
        databaseService.setAutocommit(true);
        when(githubApi.searchForIssue(refEq(pt))).thenReturn(Optional.<String>absent());
        doSetUp();
    }

    /**
     * Store the phenotype given into the db as part of set up.
     * @param pt the phenotype
     */
    protected void savePhenotype(Phenotype pt) throws Exception
    {
        PhenotypeManager manager = injector.getInstance(PhenotypeManager.class);
        manager.init(new GithubAPI.Repository("", "", ""), folder.getRoot().toPath());
        manager.createRequest(pt);
    }
}

