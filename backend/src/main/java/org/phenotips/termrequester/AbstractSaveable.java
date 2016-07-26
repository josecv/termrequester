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

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Any object that can be saved to both a database and github.
 *
 * @version $Id$
 */
public abstract class AbstractSaveable
{
    /**
     * A hash to uniquely identify this version of the object.
     */
    private String versionHash = "";

    /**
     * The internal id of this object.
     */
    private String id;

    /**
     * This object's last returned etag.
     */
    private String etag;

    /**
     * Figure out whether this object is dirty and should be written.
     * @return whether this is dirty
     */
    @JsonIgnore
    public boolean isDirty()
    {
        return (!getId().isPresent()) || (!versionHash.equals(calculateVersionHash()));
    }

    /**
     * Mark this object as clean; database updates depend on this, so don't do it willy-nilly.
     */
    public void setClean()
    {
        checkState(getId().isPresent(), "Phenotype %s cannot be setClean without id", this);
        versionHash = calculateVersionHash();
    }

    /**
     * Get id.
     *
     * @return id as String.
     */
    public Optional<String> getId()
    {
        return Optional.fromNullable(id);
    }

    /**
     * Set id.
     *
     * @param id the value to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Get etag.
     *
     * @return etag as String.
     */
    @JsonIgnore
    public String getEtag()
    {
        return etag;
    }

    /**
     * Set etag.
     *
     * @param etag the value to set.
     */
    public void setEtag(String etag)
    {
        this.etag = etag;
    }

    /**
     * Calculate (but do not set) the current versionHash.
     * @return the version hash
     */
    protected abstract String calculateVersionHash();
}
