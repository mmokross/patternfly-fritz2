#!/bin/bash

# Script to build, deploy and release PatternFly Fritz2.
#
# Prerequisites
#   - Clean git status (no uncommitted changes in branch 'master')
#   - No tag for the specified version
#
# Parameters
#   1. New semantic version number
#
# What it does
#   1. Build branch 'master'
#   2. Bump version to '<version>'
#   3. Commit version change
#   4. Create and push tag 'v<version>'
#   5. GitHub workflow defined in 'release.yml' kicks in
#        - Publish API documentation
#        - Publish packages



VERSION=$1
VERSION_TAG=v$1


if [[ "$#" -ne 1 ]]; then
    echo "Illegal number of parameters. Please use $0 <version>"
    exit 1
fi

# See https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
# and https://gist.github.com/rverst/1f0b97da3cbeb7d93f4986df6e8e5695
if ! [[ "$VERSION" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-((0|[1-9][0-9]*|[0-9]*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9][0-9]*|[0-9]*[a-zA-Z-][0-9a-zA-Z-]*))*))?(\+([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*))?$ ]]; then
    echo "Illegal semantic version number. Version must apply to https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string"
    exit 1
fi

if git rev-parse -q --verify "refs/tags/$VERSION_TAG" >/dev/null; then
    echo "A tag for '$VERSION_TAG' already exists."
    exit 1
fi

printf "\n# Check clean status\n\n"
git checkout master
if ! git diff --no-ext-diff --quiet --exit-code; then
    echo "Unable to release. You have uncommitted changes in the branch 'master'."
    exit 1
fi

printf "\n\n\n# Build master\n\n"
git pull origin master
./gradlew build || { echo "Build failed" ; exit 1; }

printf "\n\n\n# Bump to %s\n\n" "$VERSION"
./versionBump.sh "$VERSION"
git commit -am "Bump to $VERSION"
git push origin master

printf "\n\n\n# Tag and push %s\n\n" "$VERSION_TAG"
git tag "$VERSION_TAG"
git push --tags origin

printf "\n\n\n<<--==  PatternFly Fritz2 %s successfully released  ==-->>\n\n" "$VERSION_TAG"
