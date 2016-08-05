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

import java.io.InputStream;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;


import org.phenotips.termrequester.di.TermRequesterBackendModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assume.assumeTrue;


/**
 * A test that makes use of a github repo.
 *
 * @version $Id$
 */
public abstract class AbstractGithubTest
{
    /**
     * The github user whose repo we're gonna hit.
     */
    protected static String USER;

    /**
     * The base of the name of the repository to hit.
     */
    protected static String REPO_BASE;

    /**
     * The OAuth Token we're using for auth - needs public_repo, repo, and delete_repo privileges.
     */
    protected static String TEST_TOKEN;

    /**
     * The guice injector to use.
     */
    protected static Injector injector;

    /**
     * An object mapper to deserialize from json.
     */
    protected static ObjectMapper mapper;

    /**
     * Set up the test creating a new repository.
     *
     * @param id an id for the testing class' repository
     * @return the name of the created repository
     */
    public static String initialize(String id) throws Exception
    {
        injector = Guice.createInjector(new TermRequesterBackendModule());
        mapper = injector.getInstance(ObjectMapper.class);
        Properties p = new Properties();
        InputStream stream = GithubAPIImplTest.class.getResourceAsStream("credentials.properties");
        assumeTrue(stream != null);
        p.load(stream);
        /* Skip if there's no token, since everything will fail */
        assumeTrue(!"replaceme".equals(p.getProperty("token")));
        USER = p.getProperty("user");
        REPO_BASE = p.getProperty("repoBase");
        TEST_TOKEN = p.getProperty("token");
        /* We now gotta create the repository we'll be using to test. */
        String ts = Long.toString((new Date()).getTime());
        String repo = REPO_BASE + id + ts;
        Map<String, String> params = new HashMap<>(1);
        params.put("name", repo);
        Request.Post("https://api.github.com/user/repos").
            bodyByteArray(mapper.writeValueAsBytes(params), ContentType.APPLICATION_JSON).
            addHeader("Authorization", "token " + TEST_TOKEN).
            execute().returnContent();
        Thread.sleep(2000);
        return repo;
    }

    public static void shutdown(String repo) throws Exception
    {
        if (TEST_TOKEN == null) {
            return;
        }
        Request.Delete(String.format("https://api.github.com/repos/%s/%s", USER, repo)).
            addHeader("Authorization", "token " + TEST_TOKEN).
            execute().returnContent();
    }
}
