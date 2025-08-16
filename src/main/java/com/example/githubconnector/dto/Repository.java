package com.example.githubconnector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {
    private String name;
    @JsonProperty("full_name")
    private String fullName;
    private Owner owner;
    @JsonProperty("private")
    private boolean isPrivate;
}