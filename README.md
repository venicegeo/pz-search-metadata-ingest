# pz-search-metadata-ingest
Web Service for Piazza metadata ingest

Service to accept JSON structure for metadat ingest to Piazza Elasticsearch cluster.  
Endpoint accepts POST of DataResource object https://github.com/venicegeo/pz-jobcommon/blob/master/src/main/java/model/data/DataResource.java
URL: http://pz-search-metadata-ingest.cf.piazzageo.io/api/v1/data - returns/echoes ingested document 

Example data for ingest:
{
  "dataId": "dc1787d7-7a5e-4d51-a86c-a171ee103723",
  "dataType": {
    "type": "shapefile",
    "location": {
      "type": "s3",
      "bucketName": "external-public-access-test",
      "fileName": "point-shapefile.zip",
      "domainName": "s3.amazonaws.com"
    }
  },
  "spatialMetadata": {
    "coordinateReferenceSystem": "GEOGCS[\"GCS_WGS_1984\", \n  DATUM[\"D_WGS_1984\", \n    SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]], \n  PRIMEM[\"Greenwich\", 0.0], \n  UNIT[\"degree\", 0.017453292519943295], \n  AXIS[\"Longitude\", EAST], \n  AXIS[\"Latitude\", NORTH]]",
    "epsgCode": 4326,
    "minX": -109.86515258861945,
    "minY": 32.47219946485272,
    "maxX": -100.63284388599796,
    "maxY": 40.6813399349672
  },
  "metadata": {
    "description": "Ingested automatically by FileWatcher.",
    "classType": {
      "classification": "unclassified"
    }
  }
}

