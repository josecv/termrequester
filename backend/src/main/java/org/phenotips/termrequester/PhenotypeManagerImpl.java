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

import org.phenotips.termrequester.db.DatabaseService;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.github.GithubAPIFactory;
import org.phenotips.termrequester.github.GithubException;
import org.phenotips.termrequester.utils.IdUtils;

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;


/**
 * Implements a phenotype manager, tying together the various backend components of the term requester.
 *
 * @version $Id$
 */
@Singleton
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
     * Whether this service is up.
     */
    private boolean up;

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
    public synchronized void init(GithubAPI.Repository repo, Path home) throws TermRequesterBackendException
    {
        if (!up) {
            github = factory.create(repo);
            try {
                db.init(home);
            } catch (IOException e) {
                throw new TermRequesterBackendException(e);
            }
            up = true;
        }
    }

    @Override
    public synchronized void shutdown() throws TermRequesterBackendException
    {
        if (up) {
            try {
                db.shutdown();
            } catch (IOException e) {
                throw new TermRequesterBackendException(e);
            }
            up = false;
        }
    }

    @Override
    public PhenotypeCreation createRequest(Phenotype pt) throws TermRequesterBackendException
    {
        try {
            /* TODO: Maybe set default parent if there's no parent? */
            Phenotype existing = checkInDb(pt);
            if (!Phenotype.NULL.equals(existing)) {
                return new PhenotypeCreation(updatePhenotype(existing), false);
            }
            existing = checkInGithub(pt);
            if (!Phenotype.NULL.equals(existing)) {
                return new PhenotypeCreation(updatePhenotype(existing), false);
            }
            /* It wasn't anywhere */
            github.openIssue(pt);
            db.savePhenotype(pt);
        } catch (IOException | GithubException e) {
            throw new TermRequesterBackendException(e);
        }
        return new PhenotypeCreation(pt, true);
    }

    /**
     * Check if the given phenotype exists in the db; if so merge it and return it.
     * If the phenotype is present in the db but not in github, will create it in github.
     * @param pt the phenotype to check for
     * @return the existing phenotype or Phenotype.NULL if none existed.
     */
    private Phenotype checkInDb(Phenotype pt) throws IOException, GithubException
    {

        Phenotype existing = db.getPhenotype(pt);
        if (!Phenotype.NULL.equals(existing)) {
            existing.mergeWith(pt);
            if (existing.submittable() && !(github.searchForIssue(existing).isPresent())) {
                /* We're out of sync, so submit this issue to github */
                github.openIssue(existing);
            } else {
                github.readPhenotype(existing);
            }
            return existing;
        }
        return Phenotype.NULL;
    }

    /**
     * Check if the given phenotype exists in github; if so, merge it and return it.
     * If the phenotype is in github but not in the database, data will most likely have been lost,
     * so throws.
     * @param pt the phenotype to check for
     * @return the existing phenotype or Phenotype.NULL if none existed.
     * @throws IllegalStateException if the phenotype is in github but not in the database.
     * @throws IOException if the database throws
     * @throws GithubException if github throws
     */
    private Phenotype checkInGithub(Phenotype pt) throws IOException, GithubException
    {

        Optional<String> number = github.searchForIssue(pt);
        if (number.isPresent()) {
            Phenotype existing = db.getPhenotypeByIssueNumber(number.get());
            checkState(!Phenotype.NULL.equals(existing),
                    "Phenotype with issue number %s is not in db", number.get());
            if (Phenotype.NULL.equals(existing)) {
                throw new IllegalStateException("Phenotype with issue number " + number.get()
                        + " is in github but not database.");
            }
            existing.mergeWith(pt);
            return existing;
        }
        return Phenotype.NULL;
    }

    /**
     * Persist the phenotype given and return it - does not create it.
     * @param pt the phenotype
     * @return the phenotype
     */
    private Phenotype updatePhenotype(Phenotype pt) throws IOException, GithubException
    {
        String msg = "Trying to update not yet saved phenotype";
        checkArgument(pt.getId().isPresent(), msg);
        checkArgument(pt.getIssueNumber().isPresent(), msg);
        db.savePhenotype(pt);
        github.patchIssue(pt);
        return pt;
    }

    @Override
    public Phenotype getPhenotypeById(String id) throws TermRequesterBackendException
    {
        Phenotype pt = Phenotype.NULL;
        try {
            if (IdUtils.isId(id)) {
                pt = db.getPhenotypeById(id);
            } else if (IdUtils.isHpoId(id)) {
                pt = db.getPhenotypeByHpoId(id);
            } else {
                throw new IllegalArgumentException(String.format("Id %s is malformed", id));
            }
            if (pt.getIssueNumber().isPresent()) {
                syncPhenotype(pt);
            }
            return pt;
        } catch (IOException | GithubException e) {
            throw new TermRequesterBackendException(e);
        }
    }

    @Override
    public List<Phenotype> search(String text) throws TermRequesterBackendException
    {
        try {
            List<Phenotype> results = db.searchPhenotypes(text);
            return results;
        } catch (IOException e) {
            throw new TermRequesterBackendException(e);
        }
    }

    @Override
    public void syncPhenotypes() throws TermRequesterBackendException
    {
        try {
            synchronized (db) {
                boolean autocommit = db.getAutocommit();
                db.setAutocommit(false);
                /* TODO: Is this a good idea, or should we just get them all? */
                List<Phenotype> phenotypes = db.getPhenotypesByStatus(Phenotype.Status.SUBMITTED);
                for (Phenotype pt : phenotypes) {
                    syncPhenotype(pt);
                }
                db.commit();
                db.setAutocommit(autocommit);
            }
        } catch (IOException | GithubException e) {
            throw new TermRequesterBackendException(e);
        }
    }

    /**
     * Sync the phenotype given via github and save it to the db.
     * @param pt the phenotype
     * @throws IOException if the database throws
     * @throws GithubException if github throws
     */
    private void syncPhenotype(Phenotype pt) throws IOException, GithubException
    {
        Phenotype.Status oldStatus = pt.getStatus();
        github.readPhenotype(pt);
        Phenotype.Status newStatus = pt.getStatus();
        if (newStatus.equals(Phenotype.Status.SYNONYM) && !newStatus.equals(oldStatus)) {
            String hpoId = pt.getHpoId().get();
            /* Check if it's been added as a synonym : because it's just been accepted, and we
             * haven't synced yet, we can be sure this is ok */
            Phenotype existing = db.getPhenotypeByHpoId(hpoId);
            existing.mergeWith(pt);
            db.savePhenotype(existing);
        }
        db.savePhenotype(pt);
    }
}
