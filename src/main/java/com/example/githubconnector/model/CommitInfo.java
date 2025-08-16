package com.example.githubconnector.model;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Builder
public class CommitInfo {
    private String sha;
    private String message;
    private String author;
    private ZonedDateTime timestamp;
}