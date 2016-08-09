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

import org.phenotips.termrequester.PhenotypeManager;
import org.phenotips.termrequester.TermRequesterBackendException;
import org.phenotips.termrequester.github.GithubAPI;
import org.phenotips.termrequester.rest.resources.PhenotypeResource;
import org.phenotips.termrequester.rest.resources.PhenotypesResource;

import java.nio.file.Paths;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.ext.guice.FinderFactory;
import org.restlet.ext.guice.RestletGuice;
import org.restlet.routing.Router;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import com.google.inject.Injector;

/**
 * The main restlet application for the term requester.
 *
 * @version $Id$
 */
public class TermRequesterApplication extends Application
{
    /**
     * The parameter for the repository owner.
     */
    public static final String REPO_OWNER_PARAM = "org.phenotips.termrequester.repositoryOwner";

    /**
     * The parameter for the repository name.
     */
    public static final String REPO_NAME_PARAM = "org.phenotips.termrequester.repositoryName";

    /**
     * The parameter for the oauth token.
     */
    public static final String OAUTH_TOKEN_PARAM = "org.phenotips.termrequester.oauthToken";

    /**
     * The parameter for the home directory.
     */
    public static final String HOME_DIR_PARAM = "org.phenotips.termrequester.homeDir";

    /**
     * The parameter for how often to sync, in hours.
     */
    public static final String SYNC_INTERVAL_PARAM = "org.phenotips.termrequester.syncInterval";

    /**
     * The quartz scheduler.
     * TODO This is probably a bad place for the Scheduler, partly because this
     * is tied down to the rest api
     */
    private Scheduler sched;

    /**
     * The guice injector.
     */
    private Injector injector;

    /**
     * The phenotype manager.
     */
    private PhenotypeManager manager;

    /**
     * CTOR.
     * @param parentContext the context
     */
    public TermRequesterApplication(Context parentContext)
    {
        super(parentContext.createChildContext());
    }

    /**
     * CTOR.
     */
    public TermRequesterApplication()
    {
        super();
    }

    @Override
    public Restlet createInboundRoot()
    {
        FinderFactory finder = injector.getInstance(FinderFactory.class);
        Router router = new Router(getContext());
        router.attach("/phenotypes", finder.finder(PhenotypesResource.class));
        router.attach("/phenotype/{id}", finder.finder(PhenotypeResource.class));
        return router;
    }

    @Override
    public void start() throws Exception
    {
        String repoOwner = getContext().getParameters().getFirstValue(REPO_OWNER_PARAM);
        String repoName = getContext().getParameters().getFirstValue(REPO_NAME_PARAM);
        String token = getContext().getParameters().getFirstValue(OAUTH_TOKEN_PARAM);
        String homeDir = getContext().getParameters().getFirstValue(HOME_DIR_PARAM);
        double interval = Double.parseDouble(getContext().getParameters().
                getFirstValue(SYNC_INTERVAL_PARAM));
        /* The phenotype manager is a singleton, because stateful (or at least transitively stateful,
         * since the database is for sure stateful), so we're gonna initialize it ourselves and
         * ensure the server resources don't do anything to it by passing @OwnResources as false
         */
        injector = RestletGuice.createInjector(new TermRequesterRESTModule(repoOwner, repoName,
                    token, homeDir, false));
        startPhenotypeManager(repoOwner, repoName, token, homeDir);
        super.start();
        sched = StdSchedulerFactory.getDefaultScheduler();
        sched.setJobFactory(injector.getInstance(PTJobFactory.class));
        sched.start();
        schedulePoll(interval);
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
        sched.shutdown(true);
        manager.shutdown();
    }

    /**
     * initialize the phenotype manager.
     * @param repoOwner the owner of the repository
     * @param repoName the name of the repository
     * @param token the oauth token
     * @param homeDir the home directory
     */
    private void startPhenotypeManager(String repoOwner, String repoName, String token, String homeDir)
        throws TermRequesterBackendException
    {
        GithubAPI.Repository repo = new GithubAPI.Repository(repoOwner, repoName, token);
        manager = injector.getInstance(PhenotypeManager.class);
        manager.init(repo, Paths.get(homeDir));
    }

    /**
     * Schedule the poll job.
     *
     * @param interval how often to update, in hours
     * @throws SchedulerException on scheduler error
     */
    private void schedulePoll(double interval) throws SchedulerException
    {
        String groupName = "termrequester";
        JobDetail job = newJob(PollJob.class).
            withIdentity("githubPoll", groupName).
            build();
        Trigger trigger = newTrigger().
            withIdentity("githubPollTrigger", groupName).
            startNow().
            withSchedule(simpleSchedule().
                    withIntervalInSeconds((int) Math.floor(3600 * interval)).
                    repeatForever()).
            build();
        sched.scheduleJob(job, trigger);
    }
}
