# pz-search-metadata-ingest
Web Service for Piazza metadata ingest

Service to accept JSON structure for metadat ingest to Piazza Elasticsearch cluster.  
Endpoint accepts POST of DataResource object https://github.com/venicegeo/pz-jobcommon/blob/master/src/main/java/model/data/DataResource.java
URL: http://pz-search-metadata-ingest.cf.piazzageo.io/api/v1/data - returns/echoes ingested document 

