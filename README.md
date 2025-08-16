# GitHub Repository Activity Connector

A production-grade Java Spring Boot application that connects to the GitHub API to fetch public repository activity for a given user or organization.

## Features ✨

- **Authentication**: Securely connects to the GitHub API using a Personal Access Token (PAT).
- **Fetches Repositories**: Retrieves all public repositories for a specified user or organization, with full pagination support.
- **Fetches Commits**: For each repository, it fetches the 20 most recent commits.
- **Structured Output**: Transforms the API data into clean, structured Java objects (POJOs).
- **Dual-Mode Operation**:
    - **REST API**: Exposes a simple REST endpoint to trigger the data fetch.
    - **CLI Mode**: Can be run as a command-line tool to fetch data and print it to the console.
- **Robust Error Handling**: Gracefully handles API errors like `404 Not Found` (user not found) and `403 Forbidden` (rate limiting).

***

## Setup and Configuration

### Prerequisites

- Java 21 or later
- Apache Maven 3.8+
- A GitHub Personal Access Token (PAT)

### 1. Create a GitHub Personal Access Token

You need a PAT to authenticate with the GitHub API.
1.  Go to **GitHub Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)**.
2.  Click **Generate new token**.
3.  Give it a descriptive name (e.g., `repo-activity-connector`).
4.  Under **Select scopes**, check the `public_repo` scope.
5.  Click **Generate token** and copy the token immediately.

### 2. Configure the Application (Recommended Method)

The application is configured via an environment variable. **This is the most secure and recommended method.**

Set the environment variable `GITHUB_TOKEN`:
```sh
# On macOS/Linux
export GITHUB_TOKEN="your_copied_github_pat"

# On Windows (Command Prompt)
set GITHUB_TOKEN="your_copied_github_pat"
```
You can also set this within your IDE's run configuration for easier local development.

### 3. Alternative for Local Testing (Not Recommended)

For quick local testing, you can temporarily place your token in the `application.properties` file.

1.  Open `src/main/resources/application.properties`.
2.  Add or modify the `github.api.token` line:

```properties
github.api.token=ghp_YOUR_PERSONAL_ACCESS_TOKEN_HERE
```

**⚠️ Security Warning**: This method is **not recommended** for any environment beyond temporary local testing. Hardcoding secrets is a major security risk. **Never commit this file with a real token to a version control system like Git.**

***

## Build and Run

1.  Clone the repository:
    ```sh
    git clone <your-repo-url>
    cd github-activity-connector
    ```

2.  Build the project using Maven:
    ```sh
    mvn clean install
    ```
***
## Usage

You can run the connector in two modes: REST API server or CLI tool.

### 1. REST API Mode

This is the default mode. Simply run the application JAR.

```sh
java -jar target/github-activity-connector-0.0.1-SNAPSHOT.jar
```
The server will start on port `8080`.

**Example API Call:**
Use `curl` or any API client to make a GET request to the endpoint.

```sh
curl -X GET http://localhost:8080/api/activity/surabattulavenkatesh
```
- **Success Response (`200 OK`)**:
```json
[
    {
        "repositoryName": ".github",
        "owner": "surabattulavenkatesh",
        "recentCommits": [
            {
                "sha": "bf2d2a...19e5de",
                "message": "Update dependabot.yml",
                "author": "dependabot[bot]",
                "timestamp": "2024-03-20T17:15:35Z"
            }
        ]
    }
]
```
- **Error Response (`404 Not Found`)**:
```json
{
    "timestamp": "2025-08-16T13:00:00.123",
    "status": 404,
    "error": "Not Found",
    "message": "Failed to fetch repositories for 'nonexistentuser'. Status: 404 NOT_FOUND...",
    "path": "/api/activity/nonexistentuser"
}
```

### 2. CLI Mode

To run in CLI mode, provide the GitHub username or organization as a command-line argument. The application will fetch the data, print the JSON result to the console, and then exit.

```sh
java -jar target/github-activity-connector-0.0.1-SNAPSHOT.jar microsoft
```

***

## Design Decisions and Assumptions

- **`RestTemplate` vs. `WebClient`**: `RestTemplate` was chosen for its simplicity and synchronous nature, which is sufficient for this application's requirements. For fully non-blocking, reactive use cases, `WebClient` would be the preferred choice.
- **Separation of DTOs and Models**: The `dto` package contains objects that are an exact mirror of the external API's contract. The `model` package contains the application's internal representation. This separation ensures that our application is not tightly coupled to the GitHub API's structure.
- **Error Handling**: A custom `GitHubServiceException` and a global `@ControllerAdvice` handler are used to provide consistent, meaningful error responses for the REST API, improving the client experience.
- **Endpoint Assumption**: The connector is configured to first query the `/users/{username}/repos` endpoint. A more complex implementation could be designed to automatically fall back to `/orgs/{org}/repos` if the first call fails with a 404.
