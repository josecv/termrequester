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
package org.phenotips.termrequester.rest;

import org.phenotips.termrequester.rest.di.TermRequesterRESTModule;
import org.phenotips.termrequester.rest.resources.PhenotypeResource;
import org.phenotips.termrequester.rest.resources.PhenotypesResource;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.ext.guice.FinderFactory;
import org.restlet.ext.guice.RestletGuice;
import org.restlet.routing.Router;


/**
 * The main restlet application for the term requester.
 *
 * @version $Id$
 */
public class TermRequesterApplication extends Application
{
    @Override
    public Restlet createInboundRoot()
    {
        Router router = new Router(getContext());
        /* TODO Proper values */
        FinderFactory finder = new RestletGuice.Module(new TermRequesterRESTModule("", "", "", ""));
        router.attach("/phenotypes", finder.finder(PhenotypesResource.class));
        router.attach("/phenotype/{id}", finder.finder(PhenotypeResource.class));
        return router;
    }
}
