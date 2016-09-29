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
package piazza.services.ingest.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;

import piazza.services.ingest.util.GeoJsonDeserializer;
import piazza.services.ingest.util.GeoJsonSerializer;

/**
 * Tests the serialization objects, deserializing and serializing GeoJSON geometries.
 * 
 * @author Patrick.Doody
 *
 */
public class SerializationTests {
	GeoJsonDeserializer deserializer = new GeoJsonDeserializer();
	GeoJsonSerializer serializer = new GeoJsonSerializer();
	String resourceBasePath;
	ObjectMapper objectMapper = new ObjectMapper();
	JsonFactory jsonFactory = new JsonFactory();

	/**
	 * Setup the tests
	 */
	@Before
	public void setup() {
		jsonFactory.setCodec(objectMapper);
		// The path to the test files. Use this as the base to loading each file for test.
		resourceBasePath = "src" + File.separator + "test" + File.separator + "resources" + File.separator;
	}

	/**
	 * Tests serialization and deserialization of GeoJSON Features
	 */
	@Test
	public void testSerialization() throws Exception {
		List<String> geometries = new ArrayList<String>(
				Arrays.asList("Point", "Polygon", "LineString", "MultiPoint", "MultiPolygon", "MultiLineString", "GeometryCollection"));
		JsonGenerator generator = jsonFactory.createGenerator(System.out);
		File file;
		for (String geometryType : geometries) {
			// Load test file
			file = new File(String.format("%s%s.%s", resourceBasePath, geometryType, "geojson"));
			JsonParser parser = jsonFactory.createParser(file);
			// Read
			Geometry geometry = deserializer.deserialize(parser, null);
			// Write
			serializer.serialize(geometry, generator, null);
		}
		file = null;
		generator.close();
	}

	/**
	 * Test error handling for a serialization.
	 */
	@Test(expected = Exception.class)
	public void testSerializationError() throws Exception {
		File file = new File(String.format("%s%s.%s", resourceBasePath, "Unsupported", "geojson"));
		JsonParser parser = jsonFactory.createParser(file);
		Geometry geometry = deserializer.deserialize(parser, null);
	}
}
