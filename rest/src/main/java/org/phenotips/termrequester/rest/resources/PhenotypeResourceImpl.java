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
import org.phenotips.termrequester.utils.IdUtils;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;


/**
 * Implements the phenotype resource interface to provide access to a single phenotype.
 *
 * @version $Id$
 */
public class PhenotypeResourceImpl extends AbstractTermRequesterResource
    implements PhenotypeResource
{
    /**
     * The attribute that identifies the id in the URI.
     */
    private static final String ID_ATTRIBUTE = "id";

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
    PhenotypeResourceImpl(PhenotypeManager ptManager, @HomeDir String homeDir,
            @OAuthToken String token, @RepositoryName String repoName,
            @RepositoryOwner String repoOwner, @OwnResources Boolean owned)
    {
        super(ptManager, homeDir, token, repoName, repoOwner, owned);
    }

    @Override
    @Get("json")
    public Phenotype getById()
    {
        String id = (String) getRequest().getAttributes().get(ID_ATTRIBUTE);
        if (!(IdUtils.isId(id) || IdUtils.isHpoId(id))) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }
        try {
            Phenotype pt = ptManager.getPhenotypeById(id);
            if (pt.getStatus().equals(Phenotype.Status.PUBLISHED)) {
                pt = PublishedPhenotype.from(pt);
            }
            if (Phenotype.NULL.equals(pt)) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }
            return pt;
        } catch (TermRequesterBackendException e) {
            throw new ResourceException(e);
        }
    }
}
