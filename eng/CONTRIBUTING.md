IoTCore-SDK-Java Contributing Guide
-------------------------------------

Thank you for your interest in contributing to IoTCore-SDK-Java.

- For reporting bugs, requesting features, or asking for support, please file an issue in the [Issues](https://github.com/baidu/iotcore-sdk-java/issues) section of the project. And there are corresponding templates may help you describe the issues better.

- To make code changes, or contribute something new, please follow the [GitHub Forks / Pull requests model](https://help.github.com/articles/fork-a-repo/): Fork the repo, make the change and propose it back by submitting a pull request.

Pull Requests
-------------

* **DO** submit all code changes via pull requests (PRs) rather than through a direct commit. PRs will be reviewed and potentially merged by the repo maintainers after a peer review that includes at least one maintainer.
* **DO** give PRs short-but-concise names, different type pull requests should be claimed at the fist (e.g. "bugFix #1234, fix the issue mentioned in the ISSUE" and "newFeature, add a new feature").
* **DO** refer to any relevant issues, and include [keywords](https://help.github.com/articles/closing-issues-via-commit-messages/) that automatically close issues when the PR is merged.
* **DO** ensure each commit successfully builds.  The entire PR must pass all tests in the Continuous Integration (CI) system before it'll be merged.
* **DO** address PR feedback in an additional commit(s) rather than amending the existing commits, and only rebase/squash them when necessary.  This makes it easier for reviewers to track changes.
* **DO** assume that ["Squash and Merge"](https://github.com/blog/2141-squash-your-commits) will be used to merge your commit unless you request otherwise in the PR.
* **DO** separate unrelated fixes into separate PRs, especially if they are in different assemblies.
* **DO NOT** fix merge conflicts using a merge commit. Prefer `git rebase`.


## Developer Guide

### Pre-requisites
- Install Java Development Kit 8
- Install [Maven](https://maven.apache.org/download.cgi)

### Versioning
- For submodule version changes, its CHANGELOG.md has to be updated as well。
- All dependencies versions are managed in root POM.

### Testing for SpotBugs and CheckStyle issues
SpotBugs and CheckStyle are configured to break the build if there are any issues discovered by them. It is therefore strongly recommended to run the following maven goals locally before submitting a pull request:
```shell
mvn spotbugs:check checkstyle:checkstyle-aggregate -DskipTests -Dgpg.skip -pl groupId:artifactId -am
```

### Unit Test Coverage
check the unit test and its coverage before make a pull request
```shell
mvn clean install
```