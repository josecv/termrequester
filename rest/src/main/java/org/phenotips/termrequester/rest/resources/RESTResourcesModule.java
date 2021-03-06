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

import org.phenotips.termrequester.TermRequesterBackendModule;
import org.phenotips.termrequester.rest.resources.annotations.HomeDir;
import org.phenotips.termrequester.rest.resources.annotations.OAuthToken;
import org.phenotips.termrequester.rest.resources.annotations.OwnResources;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryName;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryOwner;

import com.google.inject.AbstractModule;

/**
 * A guice module for the term requester rest service.
 *
 * @version $Id$
 */
public class RESTResourcesModule extends AbstractModule
{
    /**
     * The repository owner.
     */
    private String repositoryOwner;

    /**
     * The repository name.
     */
    private String repositoryName;

    /**
     * The oauth token.
     */
    private String oauthToken;

    /**
     * The directory to store permanent files.
     */
    private String homeDir;

    /**
     * Whether individual Resource instances own their own resources,
     * such as databases, etc.
     */
    private Boolean ownResources;

    /**
     * CTOR.
     * @param repositoryOwner the onwer of the repository we'll post to
     * @param repositoryName the name of the repository we'll post to
     * @param oauthToken the oauth token we'll use to access github
     * @param homeDir the directory for permanent files
     * @param ownResources whether ServerResources own their own resources
     */
    public RESTResourcesModule(String repositoryOwner, String repositoryName,
            String oauthToken, String homeDir, boolean ownResources)
    {
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.oauthToken = oauthToken;
        this.homeDir = homeDir;
        this.ownResources = ownResources;
    }

    /**
     * CTOR.
     * @param repositoryOwner the onwer of the repository we'll post to
     * @param repositoryName the name of the repository we'll post to
     * @param oauthToken the oauth token we'll use to access github
     * @param homeDir the directory for permanent files
     */
    public RESTResourcesModule(String repositoryOwner, String repositoryName,
            String oauthToken, String homeDir)
    {
        this(repositoryOwner, repositoryName, oauthToken, homeDir, true);
    }

    @Override
    public void configure()
    {
        install(new TermRequesterBackendModule());
        bind(PhenotypeResource.class).to(PhenotypeResourceImpl.class);
        bind(PhenotypesResource.class).to(PhenotypesResourceImpl.class);
        bindConstant().annotatedWith(HomeDir.class).to(homeDir);
        bindConstant().annotatedWith(OAuthToken.class).to(oauthToken);
        bindConstant().annotatedWith(RepositoryName.class).to(repositoryName);
        bindConstant().annotatedWith(RepositoryOwner.class).to(repositoryOwner);
        bindConstant().annotatedWith(OwnResources.class).to(ownResources);
    }
}
