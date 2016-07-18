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

import java.util.List;
import java.io.IOException;

import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.github.GithubAPIFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;


/**
 * Implements a phenotype manager, tying together the various backend components of the term requester.
 *
 * @version $Id$
 */
public class PhenotypeManagerImpl implements PhenotypeManager
{
    /**
     * The github api factory.
     */
    private GithubAPIFactory factory;

    /**
     * The database service.
     */
    private DatabaseService db;

    /**
     * The github connection.
     */
    private GithubAPI github;

    /**
     * CTOR.
     * @param factory the injected github api factory
     * @param db the database service
     */
    @Inject
    public PhenotypeManagerImpl(GithubAPIFactory factory, DatabaseService db)
    {
        this.factory = factory;
        this.db = db;
    }

    @Override
    public void init(GithubAPI.Repository repo)
    {
        github = factory.create(repo);
    }

    @Override
    public Phenotype createRequest(String name, List<String> synonyms, Optional<String> parentId,
                                   Optional<String> description) throws TermRequesterBackendException
    {
        Phenotype pt = new Phenotype();
        pt.setName(name);
        for (String synonym : synonyms) {
            pt.addSynonym(synonym);
        }
        pt.setDescription(description.or(""));
        /* TODO FIX FIX FIX */
        Phenotype parent = db.getPhenotypeById(parentId.or("wat")).or(new Phenotype());
        pt.setParent(parent);
        Optional<Phenotype> existing = db.getPhenotype(pt);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            if (github.hasIssue(pt)) {
                /* Somehow we're out of sync... Fix it */
                db.savePhenotype(pt);
                return pt;
            }
            github.openIssue(pt);
        } catch (IOException e) {
            throw new TermRequesterBackendException("Github API threw exception", e);
        }
        db.savePhenotype(pt);
        return pt;
    }

    @Override
    public Optional<Phenotype> getPhenotypeById(String id) throws TermRequesterBackendException
    {
        return null;
    }
}