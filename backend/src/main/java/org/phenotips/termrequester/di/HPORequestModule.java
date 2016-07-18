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
package org.phenotips.termrequester.di;

import org.phenotips.termrequester.github.GithubAPIFactory;
import org.phenotips.termrequester.github.GithubAPIFactoryImpl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;


/**
 * A guice module for the HPORequest app's backend.
 *
 * @version $Id$
 */
public class HPORequestModule extends AbstractModule
{
    @Override
    public void configure()
    {
        bind(GithubAPIFactory.class).to(GithubAPIFactoryImpl.class);
    }

    /**
     * Build and return a new object mapper.
     * This is a singleton, so as to save a bit of memory and have the entire app use the same mapper.
     * @return the new object mapper.
     */
    @Provides
    @Singleton
    public ObjectMapper provideMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
