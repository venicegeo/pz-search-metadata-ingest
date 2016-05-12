#!/bin/bash
# e.g. http://ec2-54-175-229-24.compute-1.amazonaws.com:9200
ELASTICSEARCH=$1

#Instantiate pzmetadata for data
curl -XPUT "$ELASTICSEARCH/pzmetadata" -d @mappings/elasticsearch_pzsearchdata.json

#Instantiate pzmetadata for data
curl -XPUT "$ELASTICSEARCH/pzservices" -d @mappings/elasticsearch_pzsearchservices.json

