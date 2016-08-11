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

import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

/**
 * The term requester restlet resource to manage the sum total of phenotypes.
 * In other words, provides for adding new phenotypes and searching existing ones.
 *
 * @version $Id$
 */
public interface PhenotypesResource
{
    /**
     * Create a new phenotype matching the specification given and return it; if one already
     * exists that is identical to the one being requested, return that one instead.
     *
     * @param phenotype the phenotype we want added
     * @return the new (or existing) phenotype.
     */
    @Post("json")
    Phenotype create(Phenotype phenotype);

    /**
     * Search phenotypes matching the text given (a GET param).
     *
     * @return the phenotypes
     */
    @Get("json")
    List<Phenotype> search();
}
