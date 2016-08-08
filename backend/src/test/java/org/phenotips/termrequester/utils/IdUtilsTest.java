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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.phenotips.termrequester.github.AbstractGithubTest;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.db.DatabaseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the IdUtils class.
 *
 * @version $Id$
 */
public class IdUtilsTest
{
    /**
     * A test id.
     */
    private static final String ID = IdUtils.ID_PREFIX + "000052";

    /**
     * A test hpo id.
     */
    private static final String HPO_ID = "HPO_000004";

    /**
     * Test the incrementId method.
     */
    @Test
    public void testIncrement()
    {
        String expect = IdUtils.ID_PREFIX + "000053";
        String actual = IdUtils.incrementId(ID);
        assertEquals(expect, actual);
    }

    /**
     * Test the isId method.
     */
    @Test
    public void testIsId()
    {
        assertTrue(IdUtils.isId(ID));
        assertFalse(IdUtils.isId(HPO_ID));
        assertFalse(IdUtils.isId("whatever"));
    }

    /**
     * Test the isHpoId method.
     */
    @Test
    public void testIsHpoId()
    {
        assertTrue(IdUtils.isHpoId(HPO_ID));
        assertFalse(IdUtils.isHpoId(ID));
        assertFalse(IdUtils.isHpoId("whatever"));
    }
}
