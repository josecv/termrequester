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
package org.phenotips.hporequest.github;

import org.phenotips.hporequest.Phenotype;
import java.io.IOException;


/**
 * Connects to github to get the status of a given phenotype, etc.
 * 
 * @version $Id$
 */
public interface GithubAPI
{
    /**
     * Get the status of the phenotype given
     * @param phenotype the phenotype
     * @return the status
     * @throws NoSuchElementException if the issue number refers to a non-existing issue
     * @throws IOException in event of network failure (or hacker error)
     */
    public Phenotype.Status getStatus(Phenotype phenotype) throws IOException;

    /**
     * Open a new issue for the phenotype given.
     * If phenotype has an issueNumber or the issue already exists, an error will
     * be thrown.
     * @param phenotype the phenotype to create the issue for
     * @throws IllegalArgumentException if the phenotype given has an open issue already
     * @throws IOException on network failure
     */
    public void openIssue(Phenotype phenotype) throws IOException;

    /**
     * Return whether the phenotype given has an issue.
     * @param phenotype the phenotype to check
     * @return whether it has an issue
     * @throws IOException on network failure
     */
    public boolean hasIssue(Phenotype phenotype) throws IOException;

    /**
     * Get the repository that this instance connects to.
     */
    public Repository getRepository();

    /**
     * A representation of a github repository.
     *
     * @version $Id$
     */
    public static class Repository
    {
        /**
         * The repository's owner.
         */
        private String owner;

        /**
         * The repository.
         */
        private String repository;

        /**
         * The auth token.
         */
        private String token;

        /**
         * CTOR.
         * @param owner the repo's owner
         * @param repository the repository
         * @param token the oauthToken to log into the repository
         */
        public Repository(String owner, String repository, String token)
        {
            this.owner = owner;
            this.repository = repository;
            this.token = token;
        }

        /**
         * Get owner.
         *
         * @return owner as String.
         */
        public String getOwner()
        {
            return owner;
        }
        
        /**
         * Get repository.
         *
         * @return repository as String.
         */
        public String getRepository()
        {
            return repository;
        }

        /**
         * Get token.
         *
         * @return token as String.
         */
        public String getToken()
        {
            return token;
        }
        
        /**
         * Set token.
         *
         * @param token the value to set.
         */
        public void setToken(String token)
        {
            this.token = token;
        }
    }
}

