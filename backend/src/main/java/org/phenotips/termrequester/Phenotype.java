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

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a given phenotype request.
 * Uses a null-object pattern, so instead of using the value "null", use Phenotype.NULL to denote
 * absence.
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
     * A null phenotype.
     * To be used in the absence of data, instead of just using null.
     */
    public static final Phenotype NULL = new NullPhenotype();

    /**
     * The internal id of this phenotype.
     */
    private String id;

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
    private Status status;

    /**
     * This phenotype's parent.
     */
    private Phenotype parent = NULL;

    /**
     * CTOR.
     */
    public Phenotype()
    {
        synonyms = new HashSet<>();
    }
    
    /**
     * Represents the status of a new phenotype issue in the HPO.
     */
    public static enum Status {
        SUBMITTED,
        REJECTED,
        ACCEPTED
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
        synonyms.add(synonym);
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
    public String getId()
    {
        return id;
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
    
    /** * Get issueNumber.
     *
     * @return issueNumber as String.
     */
    public String getIssueNumber()
    {
        return issueNumber;
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
     * Get a long form description of this phenotype, suitable for an issue tracker.
     * @return a description.
     */
    public String issueDescribe()
    {
        return "Term: " + this.name +
            "\nSynonyms: " + String.join(",", this.synonyms) +
            "\nParent" + parent.asParent() +
            "\nDescription: " + this.description
            ;
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
        return other.id.equals(id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    /**
     * Return this phenotype's description as a parent of another.
     * Thus, for an HPO phenotype HPO_WHATEVER
     * @return the parent representation
     */
    public String asParent()
    {
        return "#" + issueNumber;
    }

    /**
     * A null phenotype object that does nothing and returns sensible defaults.
     *
     * @version $Id : $
     */
    private static final class NullPhenotype extends Phenotype
    {
        private static final long serialVersionUID = 1450L;

        @Override
        public String getName()
        {
            return "NULL";
        }

        @Override
        public String getDescription()
        {
            return "NULL";
        }

        @Override
        public String getIssueNumber()
        {
            return "NULL";
        }

        @Override
        public String getId()
        {
            return "NULL";
        }

        @Override
        public String asParent()
        {
            /* TODO IS THIS REASONABLE */
            return "NO PARENT";
        }

        @Override
        public String toString()
        {
            return "NULL PHENOTYPE";
        }

        @Override
        public boolean equals(Object o)
        {
            /* This class is finals and there should only ever be one instance,
             * so this is probably okay */
            return this == o;
        }

        @Override
        public int hashCode()
        {
            return "NULL PHENOTYPE".hashCode();
        }

        @Override
        public Set<String> getSynonyms()
        {
            return new HashSet<>();
        }

        @Override
        public String issueDescribe()
        {
            return "NULL PHENOTYPE. YOU SHOULD NOT BE SEEING THIS IN YOUR ISSUE TRACKER. PLEASE REPORT BUG TO PHENOTIPS";
        }

        @Override
        public Status getStatus()
        {
            return Status.REJECTED;
        }

        @Override
        public Phenotype getParent()
        {
            return this;
        }
    }
}
