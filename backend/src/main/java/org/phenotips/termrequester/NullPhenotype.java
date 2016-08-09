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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;

/**
 * A null phenotype object that does nothing and returns sensible defaults.
 *
 * @version $Id : $
 */
final class NullPhenotype extends Phenotype
{
    /**
     * The null instance.
     */
    public static final Phenotype INSTANCE = new NullPhenotype();

    /**
     * The serial version uid.
     */
    private static final long serialVersionUID = 1450L;

    /**
     * The null phenotype's name.
     */
    private static final String NAME = "NULL PHENOTYPE";

    /**
     * CTOR.
     */
    private NullPhenotype()
    {
        super(NAME, NAME);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDescription()
    {
        return NAME;
    }

    @Override
    public Optional<String> getIssueNumber()
    {
        return Optional.<String>absent();
    }

    @Override
    public Optional<String> getId()
    {
        return Optional.<String>absent();
    }

    public Optional<String> getHpoId()
    {
        return Optional.<String>absent();
    }

    @Override
    public String toString()
    {
        return NAME;
    }

    @Override
    public boolean equals(Object o)
    {
        /* This class is final and there should only ever be one instance,
         * so this is probably okay */
        return this == o;
    }

    @Override
    public int hashCode()
    {
        return NAME.hashCode();
    }

    @Override
    public Set<String> getSynonyms()
    {
        return new HashSet<>();
    }

    @Override
    public Optional<Date> getTimeCreated()
    {
        return Optional.<Date>absent();
    }

    @Override
    public Optional<Date> getTimeModified()
    {
        return Optional.<Date>absent();
    }

    @Override
    public Status getStatus()
    {
        return Status.REJECTED;
    }

    @Override
    public Set<String> getParentIds()
    {
        return new HashSet<>();
    }

    @Override
    public boolean submittable()
    {
        return false;
    }

    @Override
    public boolean isDirty()
    {
        /* Always clean: don't persist this thing */
        return false;
    }
}
