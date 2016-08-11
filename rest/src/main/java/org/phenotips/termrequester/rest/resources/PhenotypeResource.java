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

import org.restlet.resource.Get;

/**
 * Manages one single phenotype, as dictated by an id encoded in the uri.
 *
 * @version $Id$
 */
public interface PhenotypeResource
{
    /**
     * Get the phenotype that matches the id at the end of the requested uri.
     * Will return an empty 404 if nothing is found.
     *
     * @return the phenotype
     */
    @Get("json")
    Phenotype getById();
}
