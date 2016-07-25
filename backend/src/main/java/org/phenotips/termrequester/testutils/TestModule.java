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
package org.phenotips.termrequester.testutils;

import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.github.GithubAPIFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;

/**
 * Sets up mocked objects and binds them to interfaces.
 * If any of the services it mocks are given as null, will not do any binding on that service.
 * This should allow you to flexibly decide which services you want and do not want mocked
 *
 * @version $Id$
 */
public final class TestModule extends AbstractModule
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
        if (githubApi != null) {
            GithubAPIFactory factory = mock(GithubAPIFactory.class);
            when(factory.create(any(GithubAPI.Repository.class))).thenReturn(githubApi);
            bind(GithubAPIFactory.class).toInstance(factory);
        }
        if (databaseService != null) {
            bind(DatabaseService.class).toInstance(databaseService);
        }
    }
}
