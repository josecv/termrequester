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
package org.phenotips.termrequester;

import com.google.common.base.Optional;


/**
 * A phenotype that has been added to the HPO.
 * TODO Is this still a thing we need.
 *
 * @version $Id$
 */
public class HPOPhenotype extends Phenotype
{
    /**
     * The serial version uid.
     */
    public static final long serialVersionUID = 1848L;

    /**
     * No-arg constructor. Sets name and description to empty string.
     */
    public HPOPhenotype()
    {
        this("", "");
    }

    /**
     * CTOR.
     * @param name the name
     * @param description the description
     */
    public HPOPhenotype(String name, String description)
    {
        super(name, description);
    }

    @Override
    public String asParent()
    {
        return getHpoId().get();
    }

    @Override
    public boolean submittable()
    {
        /* Forbid submission of something already there, for obvious reasons */
        return false;
    }
}
