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
import java.text.DecimalFormat;

import org.springframework.beans.factory.annotation.Autowired;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import model.logger.Severity;
import util.PiazzaLogger;

public class GeoJsonSerializer extends JsonSerializer<Geometry> {
	
	//private final Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PiazzaLogger logger;
	
    @Override
    public Class<Geometry> handledType() {
        return Geometry.class;
    }
    
    @Override
    public void serialize(Geometry value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException
    {

        writeGeometry(jgen, value);
    }

    public void writeGeometry(JsonGenerator jgen, Geometry value)
            throws IOException {
        if (value instanceof Polygon) {
            writePolygon(jgen, (Polygon) value);
        } else if(value instanceof Point) {
            writePoint(jgen, (Point) value);
        } else if (value instanceof MultiPoint) {
            writeMultiPoint(jgen, (MultiPoint) value);
        } else if (value instanceof MultiPolygon) {
            writeMultiPolygon(jgen, (MultiPolygon) value);
        } else if (value instanceof LineString) {
            writeLineString(jgen, (LineString) value);
        } else if (value instanceof MultiLineString) {
            writeMultiLineString(jgen, (MultiLineString) value);
        } else if (value instanceof GeometryCollection) {
            writeGeometryCollection(jgen, (GeometryCollection) value);
        } else {
			String message = "Failed to serialize Geometry to GeoJSON, unsupported type.";
			logger.log(message, Severity.ERROR);
            throw new UnsupportedOperationException("not implemented: " + value.getClass().getName());
        }
    }

    private void writeGeometryCollection(JsonGenerator jgen,
            GeometryCollection value) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "GeometryCollection");
        jgen.writeArrayFieldStart("geometries");

        for (int i = 0; i != value.getNumGeometries(); ++i) {
            writeGeometry(jgen, value.getGeometryN(i));
        }

        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    private void writeMultiPoint(JsonGenerator jgen, MultiPoint value)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "MultiPoint");
        jgen.writeArrayFieldStart("coordinates");

        for (int i = 0; i != value.getNumGeometries(); ++i) {
            writePointCoords(jgen, (Point) value.getGeometryN(i));
        }

        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    private void writeMultiLineString(JsonGenerator jgen, MultiLineString value)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "MultiLineString");
        jgen.writeArrayFieldStart("coordinates");

        for (int i = 0; i != value.getNumGeometries(); ++i) {
            writeLineStringCoords(jgen, (LineString) value.getGeometryN(i));
        }

        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    private void writeMultiPolygon(JsonGenerator jgen, MultiPolygon value)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "MultiPolygon");
        jgen.writeArrayFieldStart("coordinates");

        for (int i = 0; i != value.getNumGeometries(); ++i) {
            writePolygonCoordinates(jgen, (Polygon) value.getGeometryN(i));
        }

        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    private void writePolygon(JsonGenerator jgen, Polygon value)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "Polygon");
        jgen.writeFieldName("coordinates");
        writePolygonCoordinates(jgen, value);

        jgen.writeEndObject();
    }

    private void writePolygonCoordinates(JsonGenerator jgen, Polygon value)
            throws IOException {
        jgen.writeStartArray();
        writeLineStringCoords(jgen, value.getExteriorRing());

        for (int i = 0; i != value.getNumInteriorRing(); ++i) {
            writeLineStringCoords(jgen, value.getInteriorRingN(i));
        }
        jgen.writeEndArray();
    }

    private void writeLineStringCoords(JsonGenerator jgen, LineString ring)
            throws IOException {
        jgen.writeStartArray();
        for (int i = 0; i != ring.getNumPoints(); ++i) {
            Point p = ring.getPointN(i);
            writePointCoords(jgen, p);
        }
        jgen.writeEndArray();
    }

    private void writeLineString(JsonGenerator jgen, LineString lineString)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "LineString");
        jgen.writeFieldName("coordinates");
        writeLineStringCoords(jgen, lineString);
        jgen.writeEndObject();
    }

    private void writePoint(JsonGenerator jgen, Point p)
            throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "Point");
        jgen.writeFieldName("coordinates");
        writePointCoords(jgen, p);
        jgen.writeEndObject();
    }

    private void writePointCoords(JsonGenerator jgen, Point p)
            throws IOException {
        jgen.writeStartArray();
        DecimalFormat df = new DecimalFormat("#.#");
        df.setMaximumFractionDigits(8);
        jgen.writeNumber(df.format(p.getX()));
        jgen.writeNumber(df.format(p.getY()));
        jgen.writeEndArray();
    }

}
