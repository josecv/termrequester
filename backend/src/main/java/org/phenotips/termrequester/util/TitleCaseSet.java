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
package org.phenotips.termrequester.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.collect.ForwardingSet;


/**
 * A set of strings where every element is converted to title case.
 *
 * @version $Id$
 */
public class TitleCaseSet extends ForwardingSet<String>
{
    /**
     * The decorated string.
     */
    private final Set<String> inner;

    /**
     * Construct a new lower case set.
     */
    public TitleCaseSet()
    {
        super();
        inner = new HashSet<>();
    }

    /**
     * Construct a new lower case set, taking all elements of the initial
     * set given.
     *
     * @param initial the set to gobble up
     */
    public TitleCaseSet(Set<String> initial)
    {
        super();
        /* We can't just HashSet<>(initial), since that wouldn't decorate the elements */
        inner = new HashSet<>(initial.size());
        addAll(initial);
    }

    /**
     * Construct a new lower case set with the size given.
     *
     * @param size the initial size of the set.
     */
    public TitleCaseSet(int size)
    {
        super();
        inner = new HashSet<>(size);
    }

    @Override
    protected Set<String> delegate()
    {
        return inner;
    }

    @Override
    public boolean add(String e)
    {
        String title = WordUtils.capitalizeFully(e);
        return super.add(title);
    }

    @Override
    public boolean addAll(Collection<? extends String> other)
    {
        return standardAddAll(other);
    }

    @Override
    public boolean contains(Object o)
    {
        if (!(o instanceof String)) {
            return false;
        }
        String s = (String) o;
        return super.contains(WordUtils.capitalizeFully(s));
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return standardContainsAll(c);
    }

    @Override
    public boolean remove(Object o)
    {
        if (!(o instanceof String)) {
            return false;
        }
        String s = (String) o;
        return super.remove(WordUtils.capitalizeFully(s));
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return standardRemoveAll(titleCaseColletion(c));
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return standardRetainAll(titleCaseColletion(c));
    }

    /**
     * Title case the collection of objects given.
     *
     * @param c the collection
     * @return the strings given title cased.
     */
    private Collection<String> titleCaseColletion(Collection<?> c)
    {
        List<String> retval = new ArrayList<>(c.size());
        for (Object o : c) {
            if (o instanceof String) {
                retval.add(WordUtils.capitalizeFully((String) o));
            }
        }
        return retval;
    }
}
