## Contributing

Verify changes by running tests and building locally with the following command:

```
mvn verify
```

## Creating a New Release

Familiarize yourself with [Jenkins Plugin Release Using GitHub Actions](https://www.jenkins.io/doc/developer/publishing/releasing-cd/).

Changes should be made on a new branch. The new branch should be merged to the main branch via a pull request. Ensure that all of the CI pipeline checks and tests have passed for your changes.

After the pull request has been approved and merged to main, run the [cd.yaml](https://github.com/jenkinsci/matlab-plugin/actions/workflows/cd.yaml) file to create a new release. This workflow will create a release under [Releases](https://github.com/jenkinsci/matlab-plugin/releases). Review and update the auto-generated release notes if necessary.
