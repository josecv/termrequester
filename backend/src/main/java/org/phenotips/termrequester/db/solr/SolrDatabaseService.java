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

import java.nio.file.Path;

import java.util.concurrent.Future;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

import org.phenotips.termrequester.Phenotype;
import org.phenotips.termrequester.db.DatabaseService;
import com.google.inject.Singleton;
import com.google.common.base.Optional;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Manages a solr instance as a database.
 *
 * @version $Id : $
 */
@Singleton
public class SolrDatabaseService implements DatabaseService
{
    /* Lots of this came from org.phenotips.variantstore.db.solr.SolrController
     * TODO Consider whether it's possible to pull both into the same hierarchy */
    /**
     * The path where the database is.
     */
    private Path path;

    private CoreContainer cores;

    private SolrClient server;

    /**
     * Whether we've been initialized.
     */
    private boolean up = false;

    /**
     * Whether we've been shutdown.
     */
    private boolean down = false;

    private ExecutorService executor;

    @Override
    public void init(Path path)
    {
        if (down) {
            throw new RuntimeException("Trying to initialize shutdown component");
        }
        /* Make sure initialization is idempotent */
        if (!up) {
            up = true;
            this.path = path;
            cores = new CoreContainer(path.toString());
            cores.load();
            server = new EmbeddedSolrServer(cores, "termrequester");
            executor = Executors.newFixedThreadPool(1);
        }
    }

    @Override
    public void shutdown()
    {
        down = true;
        cores.shutdown();
        executor.shutdown();
    }

    @Override
    public Future<Phenotype> savePhenotype(Phenotype pt)
    {
        return null;
    }

    @Override
    public Future<Boolean> deletePhenotype(Phenotype pt)
    {
        return null;
    }

    @Override
    public Optional<Phenotype> getPhenotypeById(String id)
    {
        return null;
    }

    @Override
    public Optional<Phenotype> getPhenotype(Phenotype other)
    {
        return null;
    }

    @Override
    public List<Phenotype> searchPhenotypes(String text)
    {
        return null;
    }
}
