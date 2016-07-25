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
package org.phenotips.termrequester.resources;

import java.util.List;


/**
 * Contains data types respresenting requests and responses for the resources.
 *
 * @version $Id$
 */
final class DataTypes
{
    /**
     * Private CTOR.
     */
    private DataTypes() { }

    /**
     * The request object to the create() method.
     */
    public static class CreateRequest
    {
        /**
         * The name of the new phenotype.
         */
        public String name;

        /**
         * The list of synonyms for the new phenotype.
         */
        public List<String> synonyms;

        /**
         * The description for the new phenotype.
         */
        public String description;

        /**
         * The id of the parent.
         */
        public List<String> parents;
    }
}
