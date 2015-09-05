# figshare-plugin

This plug-in integrates [Jenkins](https://jenkins-ci.org) and
[figshare](http://figshare.com)

1. looks for files in the workspace using a pattern
2. creates an article in figshare using its API
3. uploads each file found to the article in figshare, also using its API

# Development

## Maven Tasks

Here is a list of maven tasks that I use on this project:

* mvn verify: runs all tests
* mvn package: creates the hpi plugin archive to be used with Jenkins
* mvn hpi:run -Djetty.port=8090: runs the Jenkins server (with the plugin pre-loaded) on port 8090
* mvn cobertura:cobertura: runs all the tests, gathering code coverage metrics
* mvn org.pitest:pitest-maven:mutationCoverage: runs Pitest mutation coverage
* mvn org.pitest:pitest-maven:scmMutationCoverage -Dinclude=ADDED,UNKNOWN,MODIFIED -DmutationThreshold=85: runs PITest mutation coverage only on modified files, failing if the threshold is below 85%

# License

This project is distributed under the MIT license
