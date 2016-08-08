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
package org.phenotips.termrequester.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for handling Phenotype ids.
 *
 * @version $Id$
 */
public final class IdUtils
{
    /**
     * The prefix for the id.
     */
    public static final String ID_PREFIX = "TEMPHPO_";

    /**
     * A pattern to parse our IDs.
     */
    public static final Pattern ID_PATTERN = Pattern.compile(ID_PREFIX + "(\\d{6})");

    /**
     * The format of our ids.
     */
    public static final String ID_FORMAT = ID_PREFIX + "%06d";

    /**
     * The very fist id to use.
     */
    public static final String INITIAL_ID = String.format(ID_FORMAT, 1);

    /**
     * A pattern to parse hpo ids.
     */
    public static final Pattern HPO_ID_PATTERN = Pattern.compile("HPO_(\\d{6})");

    /**
     * CTOR.
     */
    private IdUtils() {
        throw new AssertionError();
    }

    /**
     * Get an id that is "one-higher" than the one given.
     *
     * @param id the id to increment
     * @return the incremented id.
     */
    public static String incrementId(String id)
    {
        Matcher m = ID_PATTERN.matcher(id);
        m.find();
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("%s is not a well-formed id", id));
        }
        int number = Integer.parseInt(m.group(1));
        number++;
        String newId = String.format(ID_FORMAT, number);
        return newId;
    }

    /**
     * Return whether the string given is a termrequester id.
     *
     * @param candidate the string to check
     * @return whether it is one of our ids.
     */
    public static boolean isId(String candidate)
    {
        Matcher m = ID_PATTERN.matcher(candidate);
        m.find();
        return m.matches();
    }

    /**
     * Return whether the strign given is an hpo id.
     *
     * @param candidate the string to check
     * @return whether it is an hpo id.
     */
    public static boolean isHpoId(String candidate)
    {
        Matcher m = HPO_ID_PATTERN.matcher(candidate);
        m.find();
        return m.matches();
    }
}
