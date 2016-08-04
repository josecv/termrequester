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
package org.phenotips.termrequester.github;

import org.phenotips.termrequester.Phenotype;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * A github issue.
 *
 * @version $Id$
 */
public class Issue
{
    /**
     * The format for the github issue body for a phenotype.
     */
    public static final String BODY_FORMAT = "TERM: %s"
        + "\nSYNONYMS: %s"
        + "\nPARENTS: %s"
        + "\nDESCRIPTION: %s";

    /**
     * A pattern to parse out the issue description.
     */
    public static final Pattern BODY_PATTERN = Pattern.compile(String.format(
                BODY_FORMAT,
                "(?<name>.*)", "(?<synonyms>.*)", "(?<parents>.*)", "(?<description>.*)"));

    /**
     * Joins stuff with commas.
     */
    private static final Joiner JOINER = Joiner.on(", ").skipNulls();

    /**
     * Splits strings joined by the JOINER.
     */
    private static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();


    /**
     * The issue number within the tracker.
     */
    private int number;

    /**
     * The reported state of the issue.
     */
    private String state;

    /**
     * The labels.
     */
    private List<String> labels;

    /**
     * This issue's description.
     */
    private String body;

    /**
     * This issue's title.
     */
    private String title;

    /**
     * Get an issue body for the phenotype given.
     * @param pt the phenotype
     * @return the issue body
     */
    public static String describe(Phenotype pt)
    {
        return String.format(BODY_FORMAT, pt.getName(), JOINER.join(pt.getSynonyms()),
                JOINER.join(pt.getParentIds()), pt.getDescription().replace("\n", ". "));
    }

    /**
     * Get a phenotype that contains all the data from this issue.
     *
     * @return the phenotype
     */
    public Phenotype asPhenotype()
    {
        Matcher m = BODY_PATTERN.matcher(body);
        m.find();
        if (!m.matches()) {
            return Phenotype.NULL;
        }
        Phenotype pt = new Phenotype();
        pt.addAllSynonyms(SPLITTER.splitToList(m.group("synonyms")));
        pt.setName(m.group("name"));
        pt.addAllParentIds(SPLITTER.splitToList(m.group("parents")));
        pt.setDescription(m.group("description"));
        pt.setIssueNumber(Integer.toString(number));
        pt.setStatus(getPTStatus());
        return pt;
    }

    /**
     * Get the phenotype status for this issue.
     * @return the status
     */
    public Phenotype.Status getPTStatus()
    {
        /* TODO We're assuming no rejection here! Un-assume that */
        if ("closed".equals(state)) {
            return Phenotype.Status.ACCEPTED;
        } else {
            return Phenotype.Status.SUBMITTED;
        }
    }

    /**
     * Get number.
     *
     * @return number as int.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * Set number.
     *
     * @param number the value to set.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }

    /**
     * Get state.
     *
     * @return state as String.
     */
    public String getState()
    {
        return state;
    }

    /**
     * Set state.
     *
     * @param state the value to set.
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Get body.
     *
     * @return body as String.
     */
    public String getBody()
    {
        return body;
    }

    /**
     * Set body.
     *
     * @param body the value to set.
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * Get title.
     *
     * @return title as String.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set title.
     *
     * @param title the value to set.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Get the labels.
     *
     * @return the labels
     */
    public List<String> getLabels()
    {
        return labels;
    }

    /**
     * Set the labels.
     *
     * @param labels the labels
     */
    public void setLabels(List<String> labels)
    {
        this.labels = labels;
    }
}
