/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package piazza.services.ingest.util;

import java.io.IOException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import model.logger.Severity;
import util.PiazzaLogger;

public class GeoJsonDeserializer extends JsonDeserializer<Geometry> {
	
	private GeometryFactory gf = new GeometryFactory();
//	private final Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PiazzaLogger logger;

    @Override
    public Geometry deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
    {
        ObjectCodec oc = jp.getCodec();
        JsonNode root = oc.readTree(jp);
        return parseGeometry(root);
    }

    private Geometry parseGeometry(JsonNode root) {
        String typeName = root.get("type").asText();
        if ("Point".equals(typeName)) {
            return gf.createPoint(parseCoordinate((ArrayNode) root
                    .get("coordinates")));
        } else if("MultiPoint".equals(typeName)) {
            return gf.createMultiPoint(parseLineString(root.get("coordinates")));
        } else if("LineString".equals(typeName)) {
            return gf.createLineString(parseLineString(root.get("coordinates")));
        } else if ("MultiLineString".equals(typeName)) {
            return gf.createMultiLineString(parseLineStrings(root
                    .get("coordinates")));
        } else if("Polygon".equals(typeName)) {
            JsonNode arrayOfRings = root.get("coordinates");
            return parsePolygonCoordinates(arrayOfRings);
        } else if ("MultiPolygon".equals(typeName)) {
            JsonNode arrayOfPolygons = root.get("coordinates");
            return gf.createMultiPolygon(parsePolygons(arrayOfPolygons));
        } else if ("GeometryCollection".equals(typeName)) {
            return gf.createGeometryCollection(parseGeometries(root
                    .get("geometries")));
        } else {
        	//log.error("Failed to deserialize GeoJSON, unsupported type.");
			logger.log("Failed to deserialize GeoJSON, unsupported type.", Severity.ERROR);
            throw new UnsupportedOperationException();
        }
    }

    private Geometry[] parseGeometries(JsonNode arrayOfGeoms) {
        Geometry[] items = new Geometry[arrayOfGeoms.size()];
        for(int i=0;i!=arrayOfGeoms.size();++i) {
            items[i] = parseGeometry(arrayOfGeoms.get(i));
        }
        return items;
    }

    private Polygon parsePolygonCoordinates(JsonNode arrayOfRings) {
        return gf.createPolygon(parseExteriorRing(arrayOfRings),
                parseInteriorRings(arrayOfRings));
    }

    private Polygon[] parsePolygons(JsonNode arrayOfPolygons) {
        Polygon[] polygons = new Polygon[arrayOfPolygons.size()];
        for (int i = 0; i != arrayOfPolygons.size(); ++i) {
            polygons[i] = parsePolygonCoordinates(arrayOfPolygons.get(i));
        }
        return polygons;
    }

    private LinearRing parseExteriorRing(JsonNode arrayOfRings) {
            return gf.createLinearRing(parseLineString(arrayOfRings.get(0)));
    }

    private LinearRing[] parseInteriorRings(JsonNode arrayOfRings) {
        LinearRing[] rings = new LinearRing[arrayOfRings.size() - 1];
        for (int i = 1; i < arrayOfRings.size(); ++i) {
            rings[i - 1] = gf.createLinearRing(parseLineString(arrayOfRings
                    .get(i)));
        }
        return rings;
    }

    private Coordinate parseCoordinate(JsonNode array) {
        return new Coordinate(array.get(0).asDouble(), array.get(1).asDouble());
    }

    private Coordinate[] parseLineString(JsonNode array) {
        Coordinate[] points = new Coordinate[array.size()];
        for (int i = 0; i != array.size(); ++i) {
            points[i] = parseCoordinate(array.get(i));
        }
        return points;
    }

    private LineString[] parseLineStrings(JsonNode array) {
        LineString[] strings = new LineString[array.size()];
        for (int i = 0; i != array.size(); ++i) {
            strings[i] = gf.createLineString(parseLineString(array.get(i)));
        }
        return strings;
    }

}
