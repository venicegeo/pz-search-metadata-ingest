# pz-search-metadata-ingest
Web Service for Piazza metadata ingest

5/14/16 NOTE- migration to ES 2.x API; mid-stream migration build exceptions (temporary) partitionManager not functional

Service to accept JSON structure for metadata ingest to Piazza Elasticsearch cluster.  
Endpoint accepts POST of DataResource object https://github.com/venicegeo/pz-jobcommon/blob/master/src/main/java/model/data/DataResource.java
URL: https://pz-search-metadata-ingest.stage.geointservices.io/api/v1/data - returns/echoes ingested document 

Please reference Wiki page at:
https://github.com/venicegeo/venice/wiki/Pz-Search-Services

