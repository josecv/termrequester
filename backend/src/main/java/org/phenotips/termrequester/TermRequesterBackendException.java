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


/**
 * An exception thrown from the term requester backend.
 * @version $Id$
 */
public class TermRequesterBackendException extends Exception
{
    /**
     * For serialization.
     */
    private static final long serialVersionUID = 1830L;

    /**
     * CTOR with message.
     * @param msg the exception message.
     */
    public TermRequesterBackendException(String msg)
    {
        super(msg);
    }

    /**
     * CTOR with cause.
     * @param cause the cause.
     */
    public TermRequesterBackendException(Exception cause)
    {
        super(cause);
    }

    /**
     * CTOR with cause and message.
     * @param msg the message
     * @param cause the cause
     */
    public TermRequesterBackendException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
