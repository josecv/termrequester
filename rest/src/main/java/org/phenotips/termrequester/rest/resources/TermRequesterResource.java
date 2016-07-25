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

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.PhenotypeManager;
import org.phenotips.termrequester.TermRequesterBackendException;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.resources.annotations.HomeDir;
import org.phenotips.termrequester.rest.resources.annotations.OAuthToken;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryName;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryOwner;

import java.nio.file.Paths;

import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * The core term requester restlet resource.
 *
 * @version $Id$
 */
public class TermRequesterResource extends ServerResource
{
    /**
     * The phenotype manager we're using.
     */
    private PhenotypeManager ptManager;

    /**
     * The directory to store permanent files in.
     */
    private String homeDir;

    /**
     * The github repo.
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
    public TermRequesterResource(PhenotypeManager ptManager, @HomeDir String homeDir,
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

    /**
     * Create a new phenotype matching the request given and return it; if one already
     * exists that is identical to the one being requested, return that one instead.
     *
     * @param request the request
     * @return the new (or existing) phenotype.
     */
    @Post("json")
    public Phenotype create(DataTypes.CreateRequest request)
    {
        try {
            PhenotypeManager.PhenotypeCreation creation = ptManager.createRequest(request.name,
                    request.synonyms, request.parents,
                    Optional.of(request.description));
            if (creation.isNew) {
                getResponse().setStatus(Status.SUCCESS_CREATED);
            } else {
                getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
            }
            return creation.phenotype;
        } catch (TermRequesterBackendException e) {
            throw new ResourceException(e);
        }
    }
}
