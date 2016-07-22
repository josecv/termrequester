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

import org.phenotips.termrequester.Phenotype;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Connects to the github rest api.
 *
 * @version $Id$
 */
class GithubAPIImpl implements GithubAPI
{
    /**
     * The URL where the github api is.
     */
    public static final String GITHUB = "https://api.github.com/";

    /**
     * The URL for the github api, as a java.net.URL instance.
     */
    private static final URL GITHUB_URL;

    /**
     * The Etag header.
     */
    private static final String ETAG = "Etag";

    /**
     * The if-none-match header.
     */
    private static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * The repository to bind to.
     */
    private Repository repository;

    /**
     * Our object mapper to deserialize from JSON.
     */
    private ObjectMapper mapper;

    static {
        try {
            GITHUB_URL = new URL(GITHUB);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * CTOR.
     * @param mapper the object mapper in use
     * @param repository the repo to use
     */
    GithubAPIImpl(ObjectMapper mapper, Repository repository)
    {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void openIssue(Phenotype phenotype) throws IOException
    {
        if (!phenotype.submittable()) {
            throw new IllegalArgumentException("Phenotype " + phenotype + " is not submittable");
        }
        if (searchForIssue(phenotype).isPresent()) {
            throw new IllegalArgumentException("Issue for " + phenotype + " already exists");
        }
        Map<String, String> params = new HashMap<>();
        params.put("title", issueTitle(phenotype));
        params.put("body", phenotype.issueDescribe());
        String method = getRepoMethod("/issues");
        byte[] body = mapper.writeValueAsBytes(params);
        HttpResponse response = execute(Request.
                Post(getURI(method)).
                bodyByteArray(body, ContentType.APPLICATION_JSON)).returnResponse();
        phenotype.setEtag(response.getFirstHeader(ETAG).getValue());
        InputStream is = response.getEntity().getContent();
        DataTypes.Issue result = mapper.readValue(is, DataTypes.Issue.class);
        phenotype.setIssueNumber(Integer.toString(result.number));
        phenotype.setStatus(Phenotype.Status.SUBMITTED);
    }

    @Override
    public void patchIssue(Phenotype phenotype) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Phenotype readPhenotype(Phenotype pt) throws IOException
    {
        checkArgument(pt.getIssueNumber().isPresent(), "Phenotype %s has no issue number");
        Phenotype.Status status;
        String method = getRepoMethod("/issues/" + pt.getIssueNumber().get());
        Request request = Request.Get(getURI(method)).addHeader(IF_NONE_MATCH, pt.getEtag());
        HttpResponse response = execute(request).returnResponse();
        pt.setEtag(response.getFirstHeader(ETAG).getValue());
        InputStream is = response.getEntity().getContent();
        DataTypes.Issue issue = mapper.readValue(is, DataTypes.Issue.class);
        /* TODO We're assuming no rejection here! Un-assume that */
        if (issue.state.equals("closed")) {
            status = Phenotype.Status.ACCEPTED;
        } else {
            status = Phenotype.Status.SUBMITTED;
        }
        pt.setStatus(status);
        return pt;
    }

    @Override
    public Optional<String> searchForIssue(Phenotype candidate) throws IOException
    {
        /* Don't waste any time if it's null */
        if (Phenotype.NULL.equals(candidate)) {
            return Optional.<String>absent();
        }
        /* Short circuit. If there's a known issue number, return it */
        if (candidate.getIssueNumber().isPresent()) {
            return candidate.getIssueNumber();
        }
        String q = buildSearch(candidate);
        URIBuilder builder = new URIBuilder(getURI("/search/issues"));
        builder.addParameter("q", q);
        InputStream is;
        try {
            is = execute(Request.Get(builder.build())).returnContent().asStream();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        DataTypes.SearchResults<DataTypes.Issue> results = mapper.readValue(is,
                new TypeReference<DataTypes.SearchResults<DataTypes.Issue>>() { });
        for (DataTypes.Issue issue : results) {
            /* TODO This needs to check the synonyms as well. */
            if (issue.title.equals(issueTitle(candidate))) {
                return Optional.of(Integer.toString(issue.number));
            }
        }
        return Optional.<String>absent();
    }

    @Override
    public Repository getRepository()
    {
        return repository;
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

