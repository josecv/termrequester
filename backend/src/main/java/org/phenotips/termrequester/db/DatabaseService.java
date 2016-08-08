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
package org.phenotips.termrequester.db;

import org.phenotips.termrequester.Phenotype;

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;

/**
 * Connects to a database of some kind, keeps track of phenotypes, etc.
 * @version $Id$
 */
public interface DatabaseService
{
    /* A lot of the nice patterns in here came from org.phenotips.variantstore.db.DatabaseController */

    /**
     * Initialize this service.
     *
     * @param path the path to run under
     * @throws IOException on io/solr failure
     */
    void init(Path path) throws IOException;

    /**
     * Shut the service down.
     *
     * @throws IOException on io/solr failure
     */
    void shutdown() throws IOException;

    /**
     * Write any changes made since the last commit. Will block while doing so.
     *
     * @throws IOException on solr failure
     */
    void commit() throws IOException;

    /**
     * Save the phenotype given, whether by creating a new one or updating an existing
     * record, iff phenotype.isDirty().
     *
     * @param phenotype the phenotype
     * @return the saved phenotype
     * @throws IOException on solr failure
     */
    Phenotype savePhenotype(Phenotype phenotype) throws IOException;

    /**
     * Delete a phenotype from the db.
     *
     * @param phenotype the phenotype to delete
     * @return whether it worked
     * @throws IOException on solr failure
     */
    boolean deletePhenotype(Phenotype phenotype) throws IOException;

    /**
     * Get a phenotype matching the id given.
     *
     * @param id the id
     * @return the phenotype - might be the null phenotype if nothing matches
     * @throws IOException on solr failure
     */
    Phenotype getPhenotypeById(String id) throws IOException;

    /**
     * Get a phenotype that's equivalent to the one given (this may include ids, names or synonyms).
     *
     * @param phenotype the phenotype to look for
     * @return the existing phenotype, if it's there, or the null phenotype if not
     * @throws IOException on solr failure
     */
    Phenotype getPhenotype(Phenotype phenotype) throws IOException;

    /**
     * Get a phenotype by issue number.
     *
     * @param number the issue number
     * @return the phenotype, or null if there's nothing there.
     * @throws IOException on solr failure
     */
    Phenotype getPhenotypeByIssueNumber(String number) throws IOException;

    /**
     * Get all phenotypes with the status given.
     *
     * @param status the desired phenotype status
     * @return the list of phenotypes with the status given
     * @throws IOException on solr failure
     */
    List<Phenotype> getPhenotypesByStatus(Phenotype.Status status) throws IOException;

    /**
     * Get the phenotype with the hpo id given.
     *
     * @param hpoId the hpoid
     * @return the phenotype, or Phenotype.NULL if there isn't one
     * @throws IOException on solr failure
     */
    Phenotype getPhenotypeByHpoId(String hpoId) throws IOException;

    /**
     * Search the database for the text given.
     *
     * @param text the text to search for.
     * @return the list of results.
     * @throws IOException on solr failure
     */
    List<Phenotype> searchPhenotypes(String text) throws IOException;

    /**
     * Set whether the service ought to commit at the end of every write.
     *
     * @param autocommit whether to autocommit.
     */
    void setAutocommit(boolean autocommit);

    /**
     * Get whether autocommit is turned on.
     *
     * @return whether we're committing at the end of every write.
     */
    boolean getAutocommit();
}
