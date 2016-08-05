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

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Constructs job instances by going through our guice di.
 * @version $Id$
 */
public class PTJobFactory implements JobFactory
{
    /**
     * The guice injector in use.
     */
    private Injector injector;

    /**
     * CTOR.
     * @param injector the guice injector.
     */
    @Inject
    public PTJobFactory(Injector injector)
    {
        this.injector = injector;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
    {
        return injector.getInstance(bundle.getJobDetail().getJobClass());
    }
}
