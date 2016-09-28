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

import org.junit.Assert;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import piazza.services.ingest.util.GeometryUtils;

/**
 * Tests the Geometry Util class.
 * 
 * @author Patrick.Doody
 *
 */
public class GeometryUtilTests {

	/**
	 * Tests the handling of bounding box creation from upper/lower coordinate bounds
	 */
	@Test
	public void testBoundingBox() throws Exception {
		// Sample Inputs
		Coordinate northWest = new Coordinate(25, 11);
		Coordinate southEast = new Coordinate(16, 20);

		// Test
		Geometry boundingBox = GeometryUtils.createBoundingBox(northWest, southEast);

		// Assertions
		Assert.assertTrue(boundingBox instanceof Polygon);
	}
}
