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

import java.util.concurrent.Future;

import org.phenotips.termrequester.Phenotype;

import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Optional;


/**
 * Connects to a database of some kind, keeps track of phenotypes, etc.
 * @version $Id$
 */
public interface DatabaseService
{
    /* A lot of the nice patterns in here came from org.phenotips.variantstore.db.DatabaseController */

    /**
     * Initialize this service.
     */
    void init(Path path);

    /**
     * Shut the service down.
     */
    void shutdown();

    /**
     * Save the phenotype given, whether by creating a new one or updating an existing
     * record.
     *
     * @param phenotype the phenotype
     * @return a future containing the saved phenotype.
     */
    Future<Phenotype> savePhenotype(Phenotype phenotype);

    /**
     * Delete a phenotype from the db.
     * @param phenotype the phenotype to delete
     * @return whether it worked
     */
    Future<Boolean> deletePhenotype(Phenotype phenotype);

    /**
     * Get a phenotype matching the id given.
     * @param id
     * @return the phenotype - might be the null phenotype if nothing matches
     */
    Phenotype getPhenotypeById(String id);

    /**
     * Get a phenotype that's equivalent to the one given (this may include ids, names or synonyms).
     * @param phenotype the phenotype to look for
     * @return the existing phenotype, if it's there, or the null phenotype if not
     */
    Phenotype getPhenotype(Phenotype phenotype);

    /**
     * Get a phenotype by issue number.
     * @param number the issue number
     * @return the phenotype, or null if there's nothing there.
     */
    Phenotype getPhenotypeByIssueNumber(String number);

    /**
     * Search the database for the text given.
     * @param text the text to search for.
     * @return the list of results.
     */
    List<Phenotype> searchPhenotypes(String text);
}
