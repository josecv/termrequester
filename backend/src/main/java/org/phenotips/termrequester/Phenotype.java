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

import java.io.Serializable;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a given phenotype request.
 * Uses a null-object pattern, so instead of using the value "null", use Phenotype.NULL to denote
 * absence.
 * Once stored in the database, will have an id (accessible via getId()). If accepted into the HPO,
 * an hpo id will be set (accessible via getHpoId())
 *
 * TODO NEED TO SUPPORT MULTIPLE PARENTS
 *
 * @version $Id$
 */
public class Phenotype implements Serializable
{
    /**
     * The serial version uid.
     */
    private static final long serialVersionUID = 1789L;

    /**
     * A sensible default empty id.
     * Will NOT be returned from getId() but can instead serve for consistency.
     */
    public static final String EMPTY_ID = "NOID";

    /**
     * A sensible default empty issue number.
     * Will NOT be returned from getIssueNumber() but can instead serve for consistency.
     */
    public static final String EMPTY_ISSUE = "NOISSUE";

    /**
     * A null phenotype.
     * To be used in the absence of data, instead of just using null.
     */
    public static final Phenotype NULL = NullPhenotype.INSTANCE;

    /**
     * The internal id of this phenotype.
     */
    private String id = null;

    /**
     * The hpo id of this phenotype.
     */
    private String hpoId = null;

    /**
     * The time when this was created.
     */
    private Date timeCreated = null;

    /**
     * The time when this was modified.
     */
    private Date timeModified = null;

    /**
     * The github issue number.
     */
    private String issueNumber;

    /**
     * The phenotype's name
     */
    private String name;

    /**
     * The phenotype's description.
     */
    private String description;

    /**
     * A set of the phenotype's synonyms.
     */
    private Set<String> synonyms;

    /**
     * The issue status.
     */
    private Status status = Status.UNSUBMITTED;

    /**
     * This phenotype's parent.
     */
    private Phenotype parent = NULL;

    /**
     * A hash to uniquely identify this version of a phenotype.
     */
    private int versionHash = 0;

    /**
     * CTOR.
     *
     * @param name the name
     * @param description the description
     */
    public Phenotype(String name, String description)
    {
        this.name = name;
        this.description = description;
        synonyms = new HashSet<>();
    }
    
    /**
     * Get the list of synonyms for this phenotype.
     *
     * @return the list.
     */
    public Set<String> getSynonyms()
    {
        return new HashSet<>(synonyms);
    }

    /**
     * Add a new synonym to this phenotype.
     *
     * @param synonym the new synonym
     */
    public void addSynonym(String synonym)
    {
        /* We need to make sure we don't define something as a synonym of itself */
        if (!name.equals(synonym)) {
            synonyms.add(synonym);
        }
    }

    /**
     * Add all the synonyms given to this phenotype's set of synonyms.
     * @param synonyms the new syonyms to add
     */
    public void addAllSynonyms(Collection<String> synonyms)
    {
        synonyms.addAll(synonyms);
        synonyms.remove(name);
    }

    /**
     * Remove the snyonym given from this phenotype, if present.
     * @param synonym the synonym to remove.
     * @return whether it was there.
     */
    public boolean removeSynonym(String synonym)
    {
        return synonyms.remove(synonym);
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
     * Get the hpoId.
     *
     * @return the hpo id.
     */
    public Optional<String> getHpoId()
    {
        return Optional.fromNullable(hpoId);
    }

    /**
     * Set the hpoId.
     *
     * @param hpoId the value to set.
     */
    public void setHpoId(String hpoId)
    {
        this.hpoId = hpoId;
    }
    
    /**
     * Get issueNumber.
     *
     * @return issueNumber as String.
     */
    public Optional<String> getIssueNumber()
    {
        if (status == Status.UNSUBMITTED) {
            return Optional.<String>absent();
        } else {
            return Optional.of(issueNumber);
        }
    }

    /**
     * Set issueNumber.
     *
     * @param issueNumber the value to set.
     */
    public void setIssueNumber(String issueNumber)
    {
        this.issueNumber = issueNumber;
    }

    /**
     * Get status.
     *
     * @return status as Status.
     */
    public Status getStatus()
    {
        return status;
    }
    
    /**
     * Set status.
     *
     * @param status the value to set.
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
    
    /**
     * Get name.
     *
     * @return name as String.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Set name.
     *
     * @param name the value to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /**
     * Get description.
     *
     * @return description as String.
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * Set description.
     *
     * @param description the value to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get parent.
     *
     * @return parent as Phenotype.
     */
    public Phenotype getParent()
    {
        return parent;
    }

    /**
     * Set parent.
     *
     * @param parent the value to set.
     */
    public void setParent(Phenotype parent)
    {
        this.parent = parent;
    }

    /**
     * Get timeCreated.
     *
     * @return timeCreated as Date.
     */
    public Optional<Date> getTimeCreated()
    {
        return Optional.fromNullable(timeCreated);
    }

    /**
     * Set timeCreated.
     *
     * @param timeCreated the value to set.
     */
    public void setTimeCreated(Date timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    /**
     * Get timeModified.
     *
     * @return timeModified as Date.
     */
    public Optional<Date> getTimeModified()
    {
        return Optional.fromNullable(timeModified);
    }

    /**
     * Set timeModified.
     *
     * @param timeModified the value to set.
     */
    public void setTimeModified(Date timeModified)
    {
        this.timeModified = timeModified;
    }

    /**
     * Get a long form description of this phenotype, suitable for an issue tracker.
     * @return a description.
     */
    public String issueDescribe()
    {
        return "TERM: " + this.name +
            "\nSYNONYMS: " + String.join(",", this.synonyms) +
            "\nPARENT: " + parent.asParent() +
            "\nPT_INTERNAL_ID: " + this.id +
            "\nDESCRIPTION: " + this.description.replace("\n", ". ")
            ;
    }

    /**
     * Figure out whether this object is dirty and should be written.
     * @return if this is dirty
     */
    public boolean isDirty()
    {
        return (!getId().isPresent()) || (versionHash != hashCode());
    }

    /**
     * Mark this object as clean; database updates depend on this, so don't do it willy-nilly.
     */
    public void setClean()
    {
        checkState(getId().isPresent(), "Phenotype %s cannot be setClean without id", this);
        versionHash = hashCode();
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Phenotype)) {
            return false;
        }
        Phenotype other = (Phenotype) o;
        if (other.id == this.id) {
            return true;
        }
        /* If it's null, we aren't (or they would've been ==) so it's not us */
        if (other.id == null) {
            return false;
        }
        return other.id.equals(id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getId().or(EMPTY_ID), name, description, synonyms,
                parent, getIssueNumber().or(EMPTY_ISSUE), status, hpoId);
    }

    /**
     * Return this phenotype's description as a parent of another.
     * @return the parent representation
     */
    public String asParent()
    {
        if (getIssueNumber().isPresent()) {
            return "#" + issueNumber;
        } else if (getId().isPresent()) {
            return id;
        }
        return name;
    }

    /**
     * Return whether it's okay to submit this phenotype.
     * @return whether this phenotype should be a candidate for issue submission
     */
    public boolean submittable()
    {
        return status == Status.UNSUBMITTED;
    }

    /**
     * Consume the phenotype given, merging it with this one.
     * @param other the phenotype to merge this one to. Will be left unchanged
     */
    public void mergeWith(Phenotype other)
    {
        addAllSynonyms(other.getSynonyms());
        addSynonym(other.getName());
        /* TODO Merge description sensibly */
    }

    /**
     * Represents the status of a new phenotype issue in the HPO.
     */
    public static enum Status {
        UNSUBMITTED,
        SUBMITTED,
        REJECTED,
        ACCEPTED
    }
}
