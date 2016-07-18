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

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import org.phenotips.termrequester.Phenotype;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Connects to the github rest api.
 * TODO Implement conditional requests: https://developer.github.com/v3/#conditional-requests
 * 
 * @version $Id$
 */
class GithubAPIImpl implements GithubAPI
{
    /**
     * The repository to bind to.
     */
    private Repository repository;

    /**
     * The URL where the github api is.
     */
    public static final String GITHUB = "https://api.github.com/";

    /**
     * The URL for the github api, as a java.net.URL instance.
     */
    private static final URL GITHUB_URL;

    /**
     * Our object mapper to deserialize from JSON.
     * TODO This may be better off elsewhere. Consider dependency injection here.
     */
    private ObjectMapper mapper;

    static {
        try {
            GITHUB_URL = new URL(GITHUB);
        } catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * CTOR.
     * @param mapper the object mapper in use
     * @param repository the repo to use
     */
    public GithubAPIImpl(ObjectMapper mapper, Repository repository)
    {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void openIssue(Phenotype phenotype) throws IOException
    {
        if (hasIssue(phenotype)) {
            throw new IllegalArgumentException("Issue for " + phenotype + " already exists");
        }
        Map<String, String> params = new HashMap<>();
        params.put("title", issueTitle(phenotype));
        params.put("body", phenotype.issueDescribe());
        String method = getRepoMethod("/issues");
        byte[] body = mapper.writeValueAsBytes(params);
        InputStream is = execute(Request.
                Post(getURI(method)).
                bodyByteArray(body, ContentType.APPLICATION_JSON)).
            returnContent().asStream();
        DataTypes.Issue result = mapper.readValue(is, DataTypes.Issue.class);
        phenotype.setIssueNumber(Integer.toString(result.number));
    }

    @Override
    public Phenotype.Status getStatus(Phenotype pt) throws IOException
    {
        /* TODO We're assuming no rejection here! Un-assume that */
        InputStream is = issueEndpoint(pt).returnContent().asStream();
        DataTypes.Issue issue = mapper.readValue(is, DataTypes.Issue.class);
        if (issue.state.equals("closed")) {
            return Phenotype.Status.ACCEPTED;
        }
        return Phenotype.Status.SUBMITTED;
    }

    @Override
    public boolean hasIssue(Phenotype phenotype) throws IOException
    {
        String issueNumber = phenotype.getIssueNumber();
        /* TODO THIS SHOULDN'T BE A NULL CHECK... */
        if (issueNumber != null) {
            if (issueEndpoint(phenotype).returnResponse().getStatusLine().getStatusCode() == 200) {
                return true;
            }
        }
        String q = buildSearch(phenotype);
        URIBuilder builder = new URIBuilder(getURI("/search/issues"));
        builder.addParameter("q", q);
        InputStream is;
        try {
            is = execute(Request.Get(builder.build())).returnContent().asStream();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        DataTypes.SearchResults<DataTypes.Issue> results = mapper.readValue(is,
                new TypeReference<DataTypes.SearchResults<DataTypes.Issue>> () { });
        for (DataTypes.Issue issue : results) {
            /* TODO This needs to check the synonyms as well. */
            if (issue.title.equals(issueTitle(phenotype))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Repository getRepository()
    {
        return repository;
    }

    /**
     * GET the github issue for the phenotype given.
     * @param phenotype the phenotype
     * @return the HTTP response object for the request
     * @throws IOException on network error
     */
    private Response issueEndpoint(Phenotype phenotype) throws IOException
    {
        String method = getRepoMethod("/issues/" + phenotype.getIssueNumber());
        return execute(Request.Get(getURI(method)));
    }

    /**
     * Get an issue title for the phenotype given.
     * @param pt the phenotype
     * @return the issue title
     */
    private String issueTitle(Phenotype pt)
    {
        return "Add term " + pt.getName();
    }

    /**
     * Build a github search (the q= parameter) to find issues related
     * to the phenotype given.
     */
    private String buildSearch(Phenotype pt)
    {
        String repo = String.format("user:%s repo:%s", repository.getOwner(), repository.getRepository());
        return String.format("%s %s", pt.getName(), repo);
    }

    /**
     * Execute an authenticated request and return the response.
     * @param request the request
     * @return the response.
     */
    private Response execute(Request request) throws IOException
    {
        request.addHeader("Authorization", "token " + repository.getToken());
        return request.execute();
    }

    /**
     * Build a method bound to our repository.
     * @param method the name of the method, starting with a /
     * @return the bound method, i.e. /repos/:owner/:repo/:method
     */
    private String getRepoMethod(String method)
    {
        /* TODO THERE HAS TO BE A PATH BUILDER OF SOME KIND, THIS IS INSANE! */
        String retval = "/repos/" + repository.getOwner() + "/" + repository.getRepository() + method;
        return retval;
    }

    /**
     * Get the URI for the method given.
     * @param method the method
     * @return the URI
     */
    private URI getURI(String method)
    {
        try {
            return new URL(GITHUB_URL, method).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

