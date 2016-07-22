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


/**
 * Creates connections to the github api at a certain repository.
 * This is useful for dependency injection purposes: we can inject onto this class
 * and then not have to worry about injecting into the actual githubapi objects.
 *
 * @version $Id$
 */
public interface GithubAPIFactory
{
    /**
     * Construct a new connection to github for the repository given.
     * @param owner the owner of the repo
     * @param repository the name of the repo
     * @param oauthToken the auth token in use
     * @return a new GithubAPI instance.
     */
    GithubAPI create(String owner, String repository, String oauthToken);

    /**
     * Construct a new connection to github for the repository given.
     * @param repository the repo
     * @return a new GithubAPI instance
     */
    GithubAPI create(GithubAPI.Repository repository);
}
