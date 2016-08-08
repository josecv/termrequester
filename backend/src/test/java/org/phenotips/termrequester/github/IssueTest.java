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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.phenotips.termrequester.Phenotype;

import org.apache.commons.lang3.builder.EqualsBuilder;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Test the Issue class.
 * @version $Id$
 */
public class IssueTest extends AbstractGithubTest
{
    /**
     * A github repositry we're hitting.
     */
    private static String repo;

    /**
     * A test phenotype.
     */
    private Phenotype pt;

    /**
     * Set up the test.
     */
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        /* Nothing at the moment, since none of these tests need a connection to github yet.
         * If one is needed later, simply add the creation stuff here and the destruction in
         * afterClass */
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void afterClass() throws Exception
    {
    }

    @Before
    public void setUp() throws Exception
    {
        pt = new Phenotype("name", "description");
        pt.addSynonym("dog");
        pt.addSynonym("was spricht die tiefe Mitternacht?");
        pt.addParentId("1234");
        pt.addParentId("5132");
        pt.setStatus(Phenotype.Status.SUBMITTED);
        pt.setIssueNumber("123");
    }

    @Test
    public void testAsPhenotype() throws Exception
    {
        String body = Issue.describe(pt);
        Issue issue = new Issue();
        issue.setBody(body);
        issue.setNumber(Integer.parseInt(pt.getIssueNumber().get()));
        Phenotype pt2 = issue.asPhenotype();
        assertFalse("asPhenotype returned null", Phenotype.NULL.equals(pt2));
        assertEquals(pt, pt2);
        assertTrue("asPhenotype failed", EqualsBuilder.reflectionEquals(pt, pt2));
    }
}
