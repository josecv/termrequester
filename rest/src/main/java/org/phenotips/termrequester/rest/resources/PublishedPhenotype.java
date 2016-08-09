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

import org.phenotips.termrequester.HPOPhenotype;
import org.phenotips.termrequester.Phenotype;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A small utility class to send back a truncated published phenotype to a client.
 * Should only be used at the very end, NEVER sent back into the backend, due to its
 * generous use of null values.
 *
 * @version $Id$
 */
final class PublishedPhenotype extends HPOPhenotype
{
    /**
     * A serial version uid.
     */
    private static final long serialVersionUID = 1648L;

    /**
     * Private ctor.
     */
    private PublishedPhenotype()
    {
        super();
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Set<String> getSynonyms()
    {
        return null;
    }

    @Override
    public Set<String> getParentIds()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String forceGetId()
    {
        return null;
    }

    @Override
    public String forceGetIssueNumber()
    {
        return null;
    }

    public static PublishedPhenotype from(Phenotype source)
    {
        checkArgument(source.getStatus().equals(Phenotype.Status.PUBLISHED));
        PublishedPhenotype pt = new PublishedPhenotype();
        pt.setHpoId(source.getHpoId().get());
        pt.setStatus(Phenotype.Status.PUBLISHED);
        return pt;
    }
}
