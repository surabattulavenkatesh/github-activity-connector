package com.example.githubconnector.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class GitHubServiceException extends RuntimeException {

    private final HttpStatusCode status;

    public GitHubServiceException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
    }

    public GitHubServiceException(String message, HttpStatusCode status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}