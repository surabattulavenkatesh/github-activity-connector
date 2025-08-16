package com.example.githubconnector.controller;

import com.example.githubconnector.model.RepoActivity;
import com.example.githubconnector.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final GitHubService gitHubService;

    @GetMapping("/{username}")
    public ResponseEntity<List<RepoActivity>> getRepositoryActivity(@PathVariable String username) {
        List<RepoActivity> activities = gitHubService.getRepositoryActivity(username);
        return ResponseEntity.ok(activities);
    }
}