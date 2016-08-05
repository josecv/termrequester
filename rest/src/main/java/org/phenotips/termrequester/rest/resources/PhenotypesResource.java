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
import org.phenotips.termrequester.rest.resources.annotations.HomeDir;
import org.phenotips.termrequester.rest.resources.annotations.OAuthToken;
import org.phenotips.termrequester.rest.resources.annotations.OwnResources;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryName;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryOwner;

import java.util.ArrayList;
import java.util.List;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

/**
 * The term requester restlet resource to manage the sum total of phenotypes.
 * In other words, provides for adding new phenotypes and searching existing ones.
 *
 * @version $Id$
 */
public class PhenotypesResource extends AbstractTermRequesterResource
{
    /**
     * The parameter for the text search.
     */
    private static final String TEXT_PARAM = "text";

    /**
     * CTOR.
     *
     * @param ptManager the injected phenotype manager.
     * @param homeDir the directory to store files in
     * @param token the oauth token
     * @param repoName the name of the repo
     * @param repoOwner the owner of the repo
     * @param owned whether we should own the resources needed
     */
    @Inject
    public PhenotypesResource(PhenotypeManager ptManager, @HomeDir String homeDir,
            @OAuthToken String token, @RepositoryName String repoName,
            @RepositoryOwner String repoOwner, @OwnResources Boolean owned)
    {
        super(ptManager, homeDir, token, repoName, repoOwner, owned);
    }

    /**
     * Create a new phenotype matching the request given and return it; if one already
     * exists that is identical to the one being requested, return that one instead.
     *
     * @param request the request
     * @return the new (or existing) phenotype.
     */
    @Post("json")
    public Phenotype create(Phenotype request)
    {
        try {
            PhenotypeManager.PhenotypeCreation creation = ptManager.createRequest(request);
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

    /**
     * Search phenotypes matching the text given (a GET param).
     *
     * @return the phenotypes
     */
    @Get("json")
    public List<Phenotype> search()
    {
        String text = getQuery().getValues(TEXT_PARAM);
        if (text == null) {
            return new ArrayList<>();
        }
        try {
            List<Phenotype> results = ptManager.search(text);
            getResponse().setStatus(Status.SUCCESS_OK);
            return results;
        } catch (TermRequesterBackendException e) {
            throw new ResourceException(e);
        }
    }
}
