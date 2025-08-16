package com.example.githubconnector.service;

import com.example.githubconnector.dto.CommitNode;
import com.example.githubconnector.dto.Repository;
import com.example.githubconnector.exception.GitHubServiceException;
import com.example.githubconnector.model.CommitInfo;
import com.example.githubconnector.model.RepoActivity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<(.+?)>; rel=\"next\"");
    private static final int COMMITS_PER_REPO = 20;
    private static final String USERS_REPOS_URL_TEMPLATE = "/users/{name}/repos";
    private static final String ORGS_REPOS_URL_TEMPLATE = "/orgs/{name}/repos";
    private static final String COMMITS_URL_TEMPLATE = "/repos/{owner}/{repo}/commits";

    private final RestTemplate restTemplate;

    @Value("${github.api.base-url}")
    private String apiBaseUrl;

    public List<RepoActivity> getRepositoryActivity(String name) {
        log.info("Starting repository activity fetch for user/org: {}", name);
        List<Repository> repositories = fetchAllRepositoriesForUserOrOrg(name);

        return repositories.stream()
                .filter(repo -> !repo.isPrivate())
                .map(this::createRepoActivity).toList();
    }

    private List<Repository> fetchAllRepositoriesForUserOrOrg(String name) {
        try {
            log.debug("Attempting to fetch repositories for user '{}'", name);
            return fetchAllRepositories(name, USERS_REPOS_URL_TEMPLATE);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User '{}' not found, attempting to fetch as an organization.", name);
                return fetchAllRepositories(name, ORGS_REPOS_URL_TEMPLATE);
            }
            throw e;
        }
    }

    /**
     * Fetches all repositories for a given name and URL template, handling API pagination.
     */
    private List<Repository> fetchAllRepositories(String name, String urlTemplate) {
        List<Repository> allRepos = new ArrayList<>();
        String url = buildApiUri(urlTemplate, name);

        while (url != null) {
            log.debug("Fetching repositories from URL: {}", url);
            try {
                ResponseEntity<List<Repository>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

                logRateLimitStatus(response);
                Optional.ofNullable(response.getBody()).ifPresent(allRepos::addAll);

                url = parseNextLinkFromHeader(response.getHeaders().getFirst("Link"));
            } catch (HttpClientErrorException e) {
                String message = String.format("Failed to fetch repositories for '%s'. Status: %s. Message: %s",
                        name, e.getStatusCode(), e.getResponseBodyAsString());
                log.error(message);
                throw new GitHubServiceException(message, e.getStatusCode(), e);
            }
        }
        log.info("Found {} total repositories for '{}' using path '{}'", allRepos.size(), name, urlTemplate);
        return allRepos;
    }


    private RepoActivity createRepoActivity(Repository repo) {
        String ownerLogin = repo.getOwner().getLogin();
        String repoName = repo.getName();
        RepoActivity activity = new RepoActivity();
        activity.setRepositoryName(repoName);
        activity.setOwner(ownerLogin);
        activity.setRecentCommits(fetchRecentCommits(ownerLogin, repoName));
        return activity;
    }

    private List<CommitInfo> fetchRecentCommits(String owner, String repoName) {
        String url = buildApiUri(COMMITS_URL_TEMPLATE, owner, repoName);
        log.debug("Fetching commits for repository: {}/{}", owner, repoName);
        try {
            ResponseEntity<CommitNode[]> response = restTemplate.getForEntity(url, CommitNode[].class);
            logRateLimitStatus(response);
            return Optional.ofNullable(response.getBody())
                    .map(Arrays::stream)
                    .map(stream -> stream.map(this::transformToCommitInfo).toList())
                    .orElse(Collections.emptyList());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("Repository '{}/{}' is empty or has no commits. Skipping.", owner, repoName);
                return Collections.emptyList();
            }
            String message = String.format("Failed to fetch commits for '%s/%s'. Status: %s", owner, repoName, e.getStatusCode());
            log.error(message);
            throw new GitHubServiceException(message, e.getStatusCode(), e);
        }
    }

    private CommitInfo transformToCommitInfo(CommitNode node) {
        return CommitInfo.builder()
                .sha(node.getSha())
                .message(node.getCommit().getMessage().split("\n", 2)[0])
                .author(node.getCommit().getAuthor().getName())
                .timestamp(node.getCommit().getAuthor().getDate())
                .build();
    }

    private String parseNextLinkFromHeader(String linkHeader) {
        if (linkHeader == null) return null;
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String buildApiUri(String template, Object... uriVariables) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl).path(template);
        if (template.equals(USERS_REPOS_URL_TEMPLATE) || template.equals(ORGS_REPOS_URL_TEMPLATE)) {
            builder.queryParam("per_page", 100);
        }
        if (template.equals(COMMITS_URL_TEMPLATE)) {
            builder.queryParam("per_page", COMMITS_PER_REPO);
        }
        return builder.buildAndExpand(uriVariables).toUriString();
    }

    private void logRateLimitStatus(ResponseEntity<?> response) {
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
        String limit = response.getHeaders().getFirst("X-RateLimit-Limit");
        if (remaining != null && limit != null) {
            log.debug("GitHub API Rate Limit: {}/{} requests remaining.", remaining, limit);
        }
    }
}