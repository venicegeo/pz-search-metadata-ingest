rem Initial index creation
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d "{\"settings\": {\"number_of_shards\": 1},\"mappings\": {\"%1\": {\"_all\": {\"enabled\": false},\"properties\": {\"locationCenterPoint\": {\"type\": \"geo_point\"},\"boundingArea\": {\"type\": \"geo_shape\"}}}}}" %2

rem Alias mapping to index
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d "{\"actions\" : [{ \"add\" : { \"index\" : \"%3\", \"alias\" : \"%4\" } }]}" %5/_aliases