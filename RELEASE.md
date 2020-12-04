# Release

The processs of creating a release is as follows.

## Schedule

Releases are made on an ad-hoc basis currently, rather than on an
elapsed time basis.  Typically once there are significant changes.

## Invariants / rules

- Pull Requests should not change the version number in `project.clj`.
- Each Pull Request should update the `CHANGELOG.md` file as
  appropriate.
- In the normal state of this repository, the version in `project.clj`
  should be of the form `x.y.z-SNAPSHOT`, denoting that the repo is in
  between versions.

## Release preconditions

- Your local repo should be clean.
- All tests pass.

## Release process

Note this uses `0.1.12` as an example new release number.

1. Create a release branch on your local repo of your fork of this
  repo on Github.
  ```
  git checkout master
  git pull
  git checkout -b release-0.1.12
  git push -u origin release-0.1.12
  ```
2. Update [CHANGELOG.md](CHANGELOG.md)
  * Write the release highlights in the `Unreleased` section.
  * Add a section for the new release just under the `Unreleased` section.
  * Update the links section at the end of the file to include links
    for the new release and update the link for the Unreleased
    version.
3. Update the version in [project.clj](project.clj).
4. Update any version numbers in [README.md](README.md).
5. Push changes to your fork on Github.
  ```
  git add .
  git commit -m "Prepare release 0.1.12"
  git push origin
  ```
6. Create a pull request. 
7. Merge the pull request into the primary repo on Github.
8. Tag the new commit on Github with the new release number (e.g.,
   `0.1.12`).
9. Publish the release to Clojars.
10. Update the version in [project.clj](project.clj) to next snapshot
    version (e.g., `0.1.13-SNAPSHOT`).
