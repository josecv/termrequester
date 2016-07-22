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

import java.util.Iterator;
import java.util.List;

/* This declares a bunch of internal datatypes, so I don't see the point in exposing it
 * to the wider world. For that same reason, it makes sense to place all these thin structs
 * in one single class instead of cluttering the package (or being forced to make a new
 * package and thus have to make them public).
 */

/**
 * Contains simple data types that get returned by the github api.
 * These often only contain those fields we care about, and have them as public.
 * Should only be used for (de)serialization purposes
 */
final class DataTypes
{

    private DataTypes()
    {
        throw new AssertionError();
    }

    /**
     * A github representation of an issue.
     */
    public static final class Issue
    {
        /**
         * The issue number within the tracker.
         */
        public int number;

        /**
         * The reported state of the issue.
         */
        public String state;

        /**
         * The labels.
         */
        public List<String> labels;

        /**
         * This issue's description.
         */
        public String body;

        /**
         * This issue's title.
         */
        public String title;
    }

    /**
     * A list of results from a github search.
     */
    public static final class SearchResults<T> implements Iterable<T>
    {
        /**
         * The total number of results.
         */
        public int total_count;

        /**
         * Whether all results were returned.
         */
        public boolean incomplete_results;

        /**
         * The results.
         */
        public List<T> items;

        @Override
        public Iterator<T> iterator() {
            return items.iterator();
        }
    }
}
