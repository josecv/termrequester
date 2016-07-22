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
package org.phenotips.termrequester.db.solr;


/**
 * Contains definitions for the phenotype schema.
 *
 * @version $Id$
 */
final class Schema
{
    /**
     * The unique id of a document.
     */
    public static final String ID = "id";

    /**
     * The HPO id of a document (if there's one).
     */
    public static final String HPO_ID = "hpoId";

    /**
     * The name of a phenotype.
     */
    public static final String NAME = "name";

    /**
     * The phenotype's synonyms.
     */
    public static final String SYNONYM = "synonym";

    /**
     * The definition of the phenotype.
     */
    public static final String DEFINITION = "def";

    /**
     * The phenotype's parent.
     */
    public static final String PARENT = "is_a";

    /**
     * The phenotype's status.
     */
    public static final String STATUS = "status";

    /**
     * The phenotype issue number.
     */
    public static final String ISSUE_NUMBER = "issueNumber";

    /**
     * The time that the document was added.
     */
    public static final String TIME_CREATED = "time_created";

    /**
     * The time that this document was last changed.
     */
    public static final String TIME_MODIFIED = "time_modified";

    /**
     * The field for exact matches on the name.
     */
    public static final String NAME_EXACT = "nameExact";

    /**
     * The field for exact matches on the synonym.
     */
    public static final String SYNONYM_EXACT = "synonymExact";

    /**
     * A spell checked name field.
     */
    public static final String NAME_SPELL = "nameSpell";

    /**
     * A name prefix field.
     */
    public static final String NAME_PREFIX = "namePrefix";

    /**
     * A spell checked synonym field.
     */
    public static final String SYNONYM_SPELL = "synonymSpell";

    /**
     * A synonym prefix field.
     */
    public static final String SYNONYM_PREFIX = "synonymPrefix";

    /**
     * A full text field.
     */
    public static final String TEXT = "text";

    /**
     * A spell checked text field.
     */
    public static final String TEXT_SPELL = "textSpell";

    /**
     * A stubbed text field.
     */
    public static final String TEXT_STUB = "textStub";

    /**
     * A stubbed name field.
     */
    public static final String NAME_STUB = "nameStub";

    /**
     * A stubbed synonym field.
     */
    public static final String SYNONYM_STUB = "synonymStub";

    /**
     * Private CTOR.
     */
    private Schema() {

    }
}
