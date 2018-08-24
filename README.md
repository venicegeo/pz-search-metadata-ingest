# pz-search-metadata-ingest

The pz-search-metadata-ingest project is responsible for ingesting metadata about user services and data.

***
## Requirements
Before building and/or running the pz-search-query service, please ensure that the following components are available and/or installed, as necessary:
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JDK for building/developing, otherwise JRE is fine)
- [Maven (v3 or later)](https://maven.apache.org/install.html)
- [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [Eclipse](https://www.eclipse.org/downloads/), or any maven-supported IDE
- [ElasticSearch](https://www.elastic.co/)
- [Vagrant](https://www.vagrantup.com/docs/installation/) (for running local ElasticSearch)
- Access to Nexus is required to build

Ensure that the nexus url environment variable `ARTIFACT_STORAGE_URL` is set:

	$ export ARTIFACT_STORAGE_URL={Artifact Storage URL}
	
For additional details on prerequisites, please refer to the Piazza Developer's Guide [Core Overview](https://github.com/venicegeo/pz-docs/blob/master/documents/devguide/02-pz-core.md) or the [prerequisites for using Piazza](https://github.com/venicegeo/pz-docs/blob/master/documents/devguide/03-jobs.md) section for additional details.

***
## Setup, Configuring, & Running
### Setup
Create the directory the repository must live in, and clone the git repository:

    $ mkdir -p {PROJECT_DIR}/src/github.com/venicegeo	
	$ cd {PROJECT_DIR}/src/github.com/venicegeo
    $ git clone git@github.com:venicegeo/pz-search-metadata-ingest.git
    $ cd pz-search-metadata-ingest

>__Note:__ In the above commands, replace {PROJECT_DIR} with the local directory path for where the project source is to be installed.

### Configuring
As noted in the Requirements section, to build and run this project, ElasticSearch is required. The `src/main/resources/application.properties` file controls URL information for ElasticSearch connection configurations.

To edit the port that the service is running on, edit the `server.port` property. <br/>
To edit the api basepath that the service endpoints are hosted on, edit the `api.basepath` property.

### Running
#### Running Local Service
To build and run the search metadata ingest service locally, pz-search-metadata-ingest can be run using Eclipse any maven-supported IDE. Alternatively, pz-search-metadata-ingest can be run through command line interface (CLI), by navigating to the project directory and run:

	$ mvn clean install -U spring-boot:run

This will run a Tomcat server locally with the pz-search-metadata-ingest service running on port 8580.

#### Running Local ElasticSearch Cluster
To run local Elasticsearch cluster, you may navigate to pz-search-metadata-ingest/config and run:

	$ vagrant up search

This will bring up a vagrant box with running Elasticsearch. Localhost can access this instance via ports 9200 or 9300.

### Running Unit Tests

To run the ServiceController unit tests from the main directory, run the following command:

	$ mvn test
