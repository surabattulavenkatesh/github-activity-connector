package com.example.githubconnector.runner;

import com.example.githubconnector.model.RepoActivity;
import com.example.githubconnector.service.GitHubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);
    private final GitHubService gitHubService;

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String username = args[0];
        log.info("CLI mode: Fetching activity for '{}'", username);

        try {
            List<RepoActivity> activities = gitHubService.getRepositoryActivity(username);
            String jsonOutput = getObjectMapper().writeValueAsString(activities);
            log.info("Repository activity:\n{}", jsonOutput);
        } catch (Exception e) {
            log.error("CLI execution error: ", e);
        }
    }

    private void printUsage() {
        log.info("No command-line arguments provided. Running as web server.");
        log.info("To use CLI: java -jar app.jar <github-username-or-org>");
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}