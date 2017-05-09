To run the pz-search-metadata-ingest service locally, perhaps through Eclipse or through CLI, navigate to the project directory and run

mvn clean install -U spring-boot:run

This will run a Tomcat server locally with the pz-search-metadata-ingest service running on port 8580.

To run local Elasticsearch cluster, you may navigate to pz-search-metadata-ingest/config and run:

vagrant up search

This will bring up a vagrant box with running Elasticsearch. Localhost can access this instance via ports 9200 or 9300.
