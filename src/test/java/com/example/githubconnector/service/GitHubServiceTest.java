package com.example.githubconnector.service;

import com.example.githubconnector.dto.CommitDetails;
import com.example.githubconnector.dto.CommitNode;
import com.example.githubconnector.dto.Owner;
import com.example.githubconnector.dto.Repository;
import com.example.githubconnector.exception.GitHubServiceException;
import com.example.githubconnector.model.RepoActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubService Tests")
class GitHubServiceTest {

    private static final String TEST_USER = "testuser";
    private static final String API_BASE_URL = "https://api.github.com";
    private static final String REPOS_URL = API_BASE_URL + "/users/testuser/repos?per_page=100";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gitHubService, "apiBaseUrl", API_BASE_URL);
    }

    @Test
    @DisplayName("Should return activity list on successful API calls")
    void shouldReturnActivityList_whenApiSucceeds() {
        // Arrange
        Repository repo = createRepo("public-repo", TEST_USER, false);
        mockRepoResponse(List.of(repo));

        CommitNode commit = createCommit("sha123", "feat: initial commit");
        mockCommitResponse("public-repo", new CommitNode[]{commit});

        // Act
        List<RepoActivity> activities = gitHubService.getRepositoryActivity(TEST_USER);

        // Assert
        assertAll("Validate successful activity fetch",
                () -> assertNotNull(activities),
                () -> assertEquals(1, activities.size()),
                () -> assertEquals("public-repo", activities.getFirst().getRepositoryName()),
                () -> assertEquals(1, activities.getFirst().getRecentCommits().size()),
                () -> assertEquals("feat: initial commit", activities.getFirst().getRecentCommits().getFirst().getMessage())
        );
    }

    @Test
    @DisplayName("Should skip private repositories")
    void shouldSkipPrivateRepositories() {
        // Arrange
        Repository publicRepo = createRepo("public-repo", TEST_USER, false);
        Repository privateRepo = createRepo("private-repo", TEST_USER, true);
        mockRepoResponse(List.of(publicRepo, privateRepo));
        mockCommitResponse("public-repo", new CommitNode[]{createCommit("sha123", "commit")});

        // Act
        List<RepoActivity> activities = gitHubService.getRepositoryActivity(TEST_USER);

        // Assert
        assertAll("Validate private repos are skipped",
                () -> assertNotNull(activities),
                () -> assertEquals(1, activities.size()),
                () -> assertEquals("public-repo", activities.getFirst().getRepositoryName())
        );
    }

    @Test
    @DisplayName("Should handle pagination for repositories")
    void shouldHandleRepositoryPagination() {
        // Arrange
        String page2Url = API_BASE_URL + "/user/repos?page=2";
        HttpHeaders page1Headers = new HttpHeaders();
        page1Headers.add("Link", "<" + page2Url + ">; rel=\"next\"");

        Repository repo1 = createRepo("repo-page1", TEST_USER, false);
        Repository repo2 = createRepo("repo-page2", TEST_USER, false);

        when(restTemplate.exchange(eq(REPOS_URL), any(), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(List.of(repo1), page1Headers, HttpStatus.OK));
        when(restTemplate.exchange(eq(page2Url), any(), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(List.of(repo2), HttpStatus.OK));

        mockCommitResponse("repo-page1", new CommitNode[0]);
        mockCommitResponse("repo-page2", new CommitNode[0]);

        // Act
        List<RepoActivity> activities = gitHubService.getRepositoryActivity(TEST_USER);

        // Assert
        assertEquals(2, activities.size(), "Should find repositories from both pages");
    }

    @Test
    @DisplayName("Should throw GitHubServiceException when user is not found")
    void shouldThrowException_whenUserNotFound() {
        // Arrange
        var notFoundException = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found");
        when(restTemplate.exchange(eq(REPOS_URL), any(), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(notFoundException);

        // Act & Assert
        GitHubServiceException thrown = assertThrows(GitHubServiceException.class,
                () -> gitHubService.getRepositoryActivity(TEST_USER));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }

    @Test
    @DisplayName("Should return empty commit list for an empty repository")
    void shouldReturnEmptyCommitList_forEmptyRepo() {
        // Arrange
        Repository emptyRepo = createRepo("empty-repo", TEST_USER, false);
        mockRepoResponse(List.of(emptyRepo));

        var conflictException = new HttpClientErrorException(HttpStatus.CONFLICT, "Git Repository is empty");
        when(restTemplate.getForEntity(commitsUrl("empty-repo"), CommitNode[].class))
                .thenThrow(conflictException);

        // Act
        List<RepoActivity> activities = gitHubService.getRepositoryActivity(TEST_USER);

        // Assert
        assertAll("Validate empty repo handling",
                () -> assertEquals(1, activities.size()),
                () -> assertEquals("empty-repo", activities.get(0).getRepositoryName()),
                () -> assertTrue(activities.get(0).getRecentCommits().isEmpty())
        );
    }

    // --- Helper Methods for Mocking ---

    private void mockRepoResponse(List<Repository> repos) {
        var responseEntity = new ResponseEntity<>(repos, HttpStatus.OK);
        when(restTemplate.exchange(eq(REPOS_URL), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
    }

    private void mockCommitResponse(String repoName, CommitNode[] commits) {
        var responseEntity = new ResponseEntity<>(commits, HttpStatus.OK);
        when(restTemplate.getForEntity(eq(commitsUrl(repoName)), eq(CommitNode[].class)))
                .thenReturn(responseEntity);
    }

    private String commitsUrl(String repoName) {
        return String.format("%s/repos/%s/%s/commits?per_page=20", API_BASE_URL, TEST_USER, repoName);
    }

    // --- Helper Methods for Creating DTOs ---

    private Repository createRepo(String name, String ownerLogin, boolean isPrivate) {
        Repository repo = new Repository();
        repo.setName(name);
        repo.setPrivate(isPrivate);
        Owner owner = new Owner();
        owner.setLogin(ownerLogin);
        repo.setOwner(owner);
        return repo;
    }

    private CommitNode createCommit(String sha, String message) {
        CommitNode node = new CommitNode();
        node.setSha(sha);
        CommitDetails details = new CommitDetails();
        details.setMessage(message);
        CommitDetails.Author author = new CommitDetails.Author();
        author.setName("Test Author");
        author.setDate(ZonedDateTime.now());
        details.setAuthor(author);
        node.setCommit(details);
        return node;
    }
}