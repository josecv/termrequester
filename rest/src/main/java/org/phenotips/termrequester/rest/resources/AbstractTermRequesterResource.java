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

import org.phenotips.termrequester.PhenotypeManager;
import org.phenotips.termrequester.TermRequesterBackendException;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.resources.annotations.HomeDir;
import org.phenotips.termrequester.rest.resources.annotations.OAuthToken;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryName;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryOwner;

import java.nio.file.Paths;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

/**
 * An abstract resource, encapsulates backend stuff and provides common initialization and
 * shutdown routines.
 *
 * @version $Id :$
 */
public abstract class AbstractTermRequesterResource extends ServerResource
{
    /**
     * The phenotype manager.
     */
    protected PhenotypeManager ptManager;

    /**
     * The home directory of this resource.
     */
    private String homeDir;

    /**
     * The github repository we'll be hitting.
     */
    private GithubAPI.Repository repo;

    /**
     * CTOR.
     *
     * @param ptManager the injected phenotype manager.
     * @param homeDir the directory to store files in
     * @param token the oauth token
     * @param repoName the name of the repo
     * @param repoOwner the owner of the repo
     */
    @Inject
    public AbstractTermRequesterResource(PhenotypeManager ptManager, @HomeDir String homeDir,
            @OAuthToken String token, @RepositoryName String repoName,
            @RepositoryOwner String repoOwner)
    {
        this.ptManager = ptManager;
        this.homeDir = homeDir;
        repo = new GithubAPI.Repository(repoOwner, repoName, token);
    }

    @Override
    protected void doInit()
    {
        try {
            ptManager.init(repo, Paths.get(homeDir));
        } catch (TermRequesterBackendException e) {
            throw new ResourceException(e);
        }
    }

    @Override
    protected void doRelease()
    {
        try {
            ptManager.shutdown();
        } catch (TermRequesterBackendException e) {
            throw new ResourceException(e);
        }
    }
}
