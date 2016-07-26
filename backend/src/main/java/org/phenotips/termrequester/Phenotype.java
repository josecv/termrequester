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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/**
 * Represents a given phenotype request.
 * Uses a null-object pattern, so instead of using the value "null", use Phenotype.NULL to denote
 * absence.
 * Once stored in the database, will have an id (accessible via getId()). If accepted into the HPO,
 * an hpo id will be set (accessible via getHpoId())
 *
 * @version $Id$
 */
public class Phenotype extends AbstractSaveable implements Serializable
{
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
     * The serial version uid.
     */
    private static final long serialVersionUID = 1789L;

    /**
     * Joins all the parent ids with commas.
     */
    private static final Joiner PARENT_JOINER = Joiner.on(", ").skipNulls();

    /**
     * The hpo id of this phenotype.
     */
    private String hpoId;

    /**
     * The time when this was created.
     */
    private Date timeCreated;

    /**
     * The time when this was modified.
     */
    private Date timeModified;

    /**
     * The github issue number.
     */
    private String issueNumber;

    /**
     * The phenotype's name.
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
     * This phenotype's parents.
     */
    private Set<Phenotype> parents;

    /**
     * No-arg constructor. Sets name and description to empty string.
     */
    public Phenotype()
    {
        this("", "");
    }

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
        parents = new HashSet<>();
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
     * Get the parents.
     *
     * @return parents
     */
    public Set<Phenotype> getParents()
    {
        return new HashSet<>(parents);
    }

    /**
     * Add a new parent.
     *
     * @param parent the value to add.
     */
    public void addParent(Phenotype parent)
    {
        if (!NULL.equals(parent)) {
            parents.add(parent);
        }
    }

    /**
     * Add all the parents given to this phenotype's set.
     *
     * @param parents the parents.
     */
    public void addAllParents(Collection<Phenotype> parents)
    {
        this.parents.addAll(parents);
        this.parents.remove(NULL);
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
        List<String> theParents = new ArrayList<>(parents.size());
        for (Phenotype parent : parents) {
            theParents.add(parent.asParent());
        }
        return "TERM: " + this.name
            + "\nSYNONYMS: " + String.join(",", this.synonyms)
            + "\nPARENT: " + PARENT_JOINER.join(theParents)
            + "\nPT_INTERNAL_ID: " + this.getId().or("NONE")
            + "\nDESCRIPTION: " + this.description.replace("\n", ". ");
    }

    @Override
    protected String calculateVersionHash()
    {
        return Integer.toString(hashCode());
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof Phenotype)) {
            return false;
        }
        Phenotype other = (Phenotype) o;
        if (other.getId().isPresent() && getId().isPresent()) {
            return other.getId().get().equals(getId().get());
        }
        /* We're equal if we share at least one name with them */
        Set<String> theirNames = other.getSynonyms();
        theirNames.add(other.getName());
        Set<String> ourNames = getSynonyms();
        ourNames.add(getName());
        return !Sets.intersection(theirNames, ourNames).isEmpty();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getId().or(EMPTY_ID), name, description, synonyms,
                parents, getIssueNumber().or(EMPTY_ISSUE), status, hpoId);
    }

    /**
     * Get the ids of the parents of this phenotype.
     * Any parents without an id will not be considered.
     * @return the parent ids.
     */
    public Collection<String> getParentIds()
    {
        List<String> parentIds = new ArrayList<>(parents.size());
        for (Phenotype parent : parents) {
            if (parent.getId().isPresent()) {
                parentIds.add(parent.getId().get());
            }
        }
        return parentIds;
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
            return getId().get();
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
    public enum Status {
        /**
         * The phenotype hasn't been submitted yet.
         */
        UNSUBMITTED,
        /**
         * The phenotype has been submitted and is pending review.
         */
        SUBMITTED,
        /**
         * The phenotype has been rejected.
         */
        REJECTED,
        /**
         * The phenotype has been accpted.
         */
        ACCEPTED
    }
}
