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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import org.restlet.data.Status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/* To get the nice HTTP status code constants */
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
        } catch (IOException e) {
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
    public void openIssue(Phenotype phenotype) throws IOException, GithubException
    {
        if (!phenotype.submittable()) {
            throw new IllegalArgumentException("Phenotype " + phenotype + " is not submittable");
        }
        if (searchForIssue(phenotype).isPresent()) {
            throw new IllegalArgumentException("Issue for " + phenotype + " already exists");
        }
        String method = getRepoMethod("issues");
        byte[] body = buildRequest(phenotype);
        HttpResponse response = execute(Request.
                Post(getURI(method)).
                bodyByteArray(body, ContentType.APPLICATION_JSON));
        checkCode(response, Status.SUCCESS_CREATED);
        InputStream is = getStream(response);
        Issue result = mapper.readValue(is, Issue.class);
        phenotype.setIssueNumber(Integer.toString(result.getNumber()));
        phenotype.setStatus(Phenotype.Status.SUBMITTED);
        Issue.readEtag(phenotype, response);
    }

    @Override
    public void patchIssue(Phenotype pt) throws IOException, GithubException
    {
        checkArgument(pt.getIssueNumber().isPresent(), "Phenotype %s has no issueNumber", pt);
        byte[] body = buildRequest(pt);

        String method = getIssueEndpoint(pt.getIssueNumber().get());
        HttpResponse response = execute(Request.
                Patch(getURI(method)).
                bodyByteArray(body, ContentType.APPLICATION_JSON)
                );
        checkCode(response, Status.SUCCESS_OK);
        Issue.readEtag(pt, response);
    }

    @Override
    public Phenotype readPhenotype(Phenotype pt) throws IOException, GithubException
    {
        checkArgument(pt.getIssueNumber().isPresent(), "Phenotype %s has no issue number", pt);
        String method = getIssueEndpoint(pt.getIssueNumber().get());
        Request request = Request.Get(getURI(method)).addHeader(IF_NONE_MATCH, pt.getEtag());
        HttpResponse response = execute(request);
        if (response.getStatusLine().getStatusCode() == Status.REDIRECTION_NOT_MODIFIED.getCode()) {
            return pt;
        }
        InputStream is = response.getEntity().getContent();
        Issue issue = mapper.readValue(is, Issue.class);
        Issue.readEtag(pt, response);
        /* Github and the HPO are the ultimate authorities, so take everything from there */
        pt.replaceBy(issue.asPhenotype());
        return pt;
    }

    @Override
    public Optional<String> searchForIssue(Phenotype candidate) throws IOException, GithubException
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
            is = getStream(execute(Request.Get(builder.build())));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        DataTypes.SearchResults<Issue> results = mapper.readValue(is,
                new TypeReference<DataTypes.SearchResults<Issue>>() { });
        for (Issue issue : results) {
            Phenotype other = issue.asPhenotype();
            if (other.equals(candidate)) {
                return other.getIssueNumber();
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
     * Build a github search (the q= parameter) to find issues related
     * to the phenotype given.
     * @param pt the phenotype
     * @return the search string
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
    private HttpResponse execute(Request request) throws IOException
    {
        request.addHeader("Authorization", "token " + repository.getToken());
        Response response = request.execute();
        return response.returnResponse();
    }

    /**
     * Check that the response given has one of the status codes given.
     *
     * @param resp the response
     * @param codes the acceptable codes
     * @throws GithubException if it does not.
     */
    private void checkCode(HttpResponse resp, Status... codes) throws GithubException
    {
        Status actual = new Status(resp.getStatusLine().getStatusCode());
        Set<Status> set = Sets.newHashSet(codes);
        if (!set.contains(actual)) {
            throw new GithubException(String.format("Response code %d was not in %s",
                        actual.getCode(), set.toString()));
        }
    }

    /**
     * Get a content stream for the response given.
     * @param response the http response
     * @return the content stream
     * @throws IOException if getting the stream fails
     */
    private InputStream getStream(HttpResponse response) throws IOException
    {
        return response.getEntity().getContent();
    }

    /**
     * Get the issue endpoint (/issues/:issueNumber) for the issue given.
     * @param issueNumber the issue number
     * @return the endpoint.
     */
    private String getIssueEndpoint(String issueNumber)
    {
        return getRepoMethod("issues/" + issueNumber);
    }

    /**
     * Build a method bound to our repository.
     * @param method the name of the method, starting with a /
     * @return the bound method, i.e. /repos/:owner/:repo/:method
     */
    private String getRepoMethod(String method)
    {
        String retval = String.format("/repos/%s/%s/%s", repository.getOwner(),
                repository.getRepository(), method);
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
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build a request body out of the phenotype given.
     * @param pt the phenotype
     * @return the request body as a byte array
     */
    private byte[] buildRequest(Phenotype pt) throws IOException
    {
        byte[] body = mapper.writeValueAsBytes(Issue.getRequestParams(pt));
        return body;
    }
}

