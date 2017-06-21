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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Utility class handling geometry related helper functions.
 * 
 * @author Chris.Smith
 *
 */
public enum GeometryUtils {
	;
	
	public static final GeometryFactory G = new GeometryFactory();

	public static Geometry createBoundingBox(Coordinate topLeft, Coordinate bottomRight) {
		GeometryFactory factory = new GeometryFactory();
		double top = topLeft.y;
		double left = topLeft.x;
		double bottom = bottomRight.y;
		double right = bottomRight.x;
		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(left, bottom);
		coordinates[1] = new Coordinate(left, top);
		coordinates[2] = new Coordinate(right, top);
		coordinates[3] = new Coordinate(right, bottom);
		coordinates[4] = coordinates[0];
		LinearRing linear = factory.createLinearRing(coordinates);
		return new Polygon(linear, null, factory);
	}

}
