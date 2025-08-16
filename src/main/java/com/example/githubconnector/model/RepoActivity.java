package com.example.githubconnector.model;

import lombok.Data;
import java.util.List;

@Data
public class RepoActivity {
    private String repositoryName;
    private String owner;
    private List<CommitInfo> recentCommits;
}