# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build the plugin
mvn clean package

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ExcludeMessageBranchBuildStrategyTest

# Run the Jenkins test harness (integration tests) — slow, spins up a Jenkins instance
mvn verify

# Run a single integration test
mvn verify -Dtest=ExcludeMessageBranchBuildStrategyIntegrationTest

# Start a local Jenkins instance with the plugin installed for manual testing
mvn hpi:run
```

JUnit 4 imports are banned — the build will fail if you add them. Use JUnit 5 (`org.junit.jupiter`) and Mockito JUnit 5 extension (`@ExtendWith(MockitoExtension.class)`) instead.

## Architecture

This is a Jenkins HPI plugin that extends `BranchBuildStrategy` from the `branch-api` plugin. Its sole responsibility is deciding whether a SCM change should trigger a multibranch build, based on changed file paths or the latest commit message.

**Class hierarchy:**

```
BranchBuildStrategy (Jenkins branch-api)
└── AbstractBranchBuildStrategy          — core flow: builds SCMFileSystem, fetches git changeset,
    │                                       delegates to subclass via getPatterns/getExpressions/shouldRunBuild
    ├── AbstractRegionBranchBuildStrategy — getExpressions returns all affected file paths from changesets
    │   ├── ExcludeRegionBranchBuildStrategy — shouldRunBuild: build if any path is NOT matched
    │   │   ├── ExcludeRegionByFieldBranchBuildStrategy  — patterns from inline textarea
    │   │   └── ExcludeRegionByFileBranchBuildStrategy   — patterns from a file in the repo
    │   └── IncludeRegionBranchBuildStrategy — shouldRunBuild: build if any path IS matched
    │       ├── IncludeRegionByFieldBranchBuildStrategy  — patterns from inline textarea
    │       └── IncludeRegionByFileBranchBuildStrategy   — patterns from a file in the repo
    └── AbstractMessageBranchBuildStrategy — getExpressions returns the latest commit message only
        └── ExcludeMessageBranchBuildStrategy — shouldRunBuild: skip build if message matches a regex pattern;
                                                 configurable separately for PRs and branches
```

There is also `ExcludeByIgnoreFileBranchBuildStrategy` which reads ignore patterns from a repo file and uses
`ExcludeRegionBranchBuildStrategy` logic, but registered as a separate descriptor.

**Key collaborator — `BranchBuildStrategyHelper`:** static utility that:
- Builds a `GitSCMFileSystem` (handles both regular `SCMRevisionImpl` and PR `ChangeRequestSCMRevision` by truncating the hash to 40 chars).
- Retrieves the git changelog via `fileSystem.changesSince(...)` and parses it with `GitChangeLogParser`.
- Reads a file from the SCM root and converts its lines to a `Set<String>` of patterns (strips comments starting with `#`, strips leading `/`).

**Pattern matching:**
- Region strategies use Ant-style path matching via `SelectorUtils.matchPath`.
- Message strategies use Java regex via `Pattern.compile` / `Matcher.find`.

**PR handling:** When `currRevision instanceof ChangeRequestSCMRevision` and there is no prior build (`lastBuiltRevision == null`), the target revision of the PR is used as the base for diff computation.

**Error handling convention:** on any exception, `isAutomaticBuild` returns `true` (allow the build) to avoid silently suppressing builds due to infrastructure failures.

## Test structure

- `*Test.java` — unit tests using Mockito, fast, no Jenkins.
- `*IntegrationTest.java` — Jenkins integration tests using `@WithJenkins` + `@WithGitSampleRepo`, slow.
- `BaseBuildStrategyIntegrationTest` — shared base for integration tests; initialises a real git repo and a `WorkflowMultiBranchProject`.
- `AbstractBranchBuildStrategyExtensionTest` — uses a `TestBranchBuildStrategy` inner class to test the abstract base class flow in isolation.

## UI resources

Each concrete strategy that exposes a `@DataBoundConstructor` has a matching Jelly config view and HTML help files under `src/main/resources/<FullyQualifiedClassName>/`. Changes to configuration fields need corresponding updates in these files.