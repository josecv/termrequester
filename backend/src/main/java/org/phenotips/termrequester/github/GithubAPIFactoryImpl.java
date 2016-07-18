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

import com.google.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Constructs GithubAPI objects.
 * 
 * @version $Id$
 */
public class GithubAPIFactoryImpl implements GithubAPIFactory
{
    private ObjectMapper mapper;

    @Inject
    public GithubAPIFactoryImpl(ObjectMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public GithubAPI create(GithubAPI.Repository repo)
    {
        return new GithubAPIImpl(mapper, repo);
    }

    @Override
    public GithubAPI create(String user, String repository, String oauthToken)
    {
        GithubAPI.Repository repo = new GithubAPI.Repository(user, repository, oauthToken);
        return create(repo);
    }
}
