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
package org.phenotips.termrequester.rest;

import org.phenotips.termrequester.PhenotypeManager;
import org.phenotips.termrequester.TermRequesterBackendException;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.resources.annotations.HomeDir;
import org.phenotips.termrequester.rest.resources.annotations.OAuthToken;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryName;
import org.phenotips.termrequester.rest.resources.annotations.RepositoryOwner;

import java.nio.file.Paths;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.inject.Inject;

/**
 * A quartz job that polls github for any changes to issues.
 *
 * @version $Id$
 */
public class PollJob implements Job
{
    /**
     * The phenotype manager for this poll job.
     */
    private PhenotypeManager manager;

    /**
     * CTOR.
     * @param manager the phenotype manager
     * @param token the oauth token
     * @param repoName the repository name
     * @param repoOwner the repository owner
     * @param homeDir the home directory of the app
     */
    @Inject
    public PollJob(PhenotypeManager manager, @OAuthToken String token, @RepositoryName String repoName,
            @RepositoryOwner String repoOwner, @HomeDir String homeDir)
    {
        this.manager = manager;
        GithubAPI.Repository repo = new GithubAPI.Repository(repoOwner, repoName, token);
        try {
            manager.init(repo, Paths.get(homeDir));
        } catch (TermRequesterBackendException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException
    {
        try {
            manager.syncPhenotypes();
        } catch (TermRequesterBackendException e) {
            throw new JobExecutionException(e);
        }
    }
}
