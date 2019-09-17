## RELEASE INSTRUCTIONS

For a manual release:

1. Make sure you have pulled the latest changes with `git pull`
2. Make sure you have no uncommitted changes with `git status`
3. Create a git tag with the version from `gradle.properties`. e.g. `git tag 1.0.1`
4. Push the tag. i.e. `git push --tags`
5. Build and publish the artifacts: `MAVEN_REPO_URL=<url> ./gradlew publish -Dsnapshot=false`
6. Open a PR to increment `version` in `gradle.properties` to the next version
