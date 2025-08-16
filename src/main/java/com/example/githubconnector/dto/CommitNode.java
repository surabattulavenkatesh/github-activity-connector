package com.example.githubconnector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommitNode {
    private String sha;
    private CommitDetails commit;
}