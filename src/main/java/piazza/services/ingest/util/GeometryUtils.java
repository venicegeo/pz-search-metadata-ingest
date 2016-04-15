package piazza.services.ingest.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;


public class GeometryUtils
{
	public static final GeometryFactory G = new GeometryFactory();

	public static Geometry createCircle(
		Coordinate center,
		final double radius ) {
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(
			64);
		shapeFactory.setCentre(
			new Coordinate(
				center.x,
				center.y));
		shapeFactory.setSize(
			radius * 2);
		return shapeFactory.createCircle();
	}

	// returns a circle approximation using the curvature of the earth
	public static Geometry createGeoCircle(
		Coordinate center,
		final double radius )
			throws GeometryParseException {

		// check to see if the circle would reach either pole.
		Coordinate northpole = new Coordinate(
			center.x,
			90.0);
		Coordinate southpole = new Coordinate(
			center.x,
			-90.0);
		if (distanceBetweenPoints(
			center,
			northpole) <= radius) {
			GeometryParseException nfe = new GeometryParseException(
				"Latitude cannot be greater than 90");
			throw nfe;
		}
		if (distanceBetweenPoints(
			center,
			southpole) <= radius) {
			NumberFormatException nfe = new NumberFormatException(
				"Latitude cannot be less than -90");
			throw nfe;
		}
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[24]; // every 15 degrees
		for (int i = 0; i < 23; i++) {
			coordinates[i] = moveDistanceMetersHeadingDegrees(
				center,
				radius,
				(double) (15 * i));
		}
		coordinates[23] = coordinates[0];
		LinearRing linear = factory.createLinearRing(
			coordinates);
		return new Polygon(
			linear,
			null,
			factory);
	}

	// Algorithm taken from Williams Best Aviation Formulary
	// (http://williams.best.vwh.net/avform.htm)
	// lat =asin(sin(lat1)*cos(d)+cos(lat1)*sin(d)*cos(tc))
	// dlon=atan2(sin(tc)*sin(d)*cos(lat1),cos(d)-sin(lat1)*sin(lat))
	// lon=mod( lon1-dlon +pi,2*pi )-pi
	public static Coordinate moveDistanceMetersHeadingDegrees(
		Coordinate originDegrees,
		Double dist,
		Double rotation ) {

		// convert to Radians for calculations
		Double lonRad = Math.toRadians(
			originDegrees.x);
		Double latRad = Math.toRadians(
			originDegrees.y);
		Double distRad = dist / 6371000.0;
		Double rotRad = Math.toRadians(
			rotation);

		// Cache values for reuse
		Double cosD = Math.cos(
			distRad);
		Double sinD = Math.sin(
			distRad);
		Double sinLat = Math.sin(
			latRad);
		Double cosLat = Math.cos(
			latRad);

		Double newLat = Math.asin(
			(sinLat * cosD) + (cosLat * sinD * Math.cos(
				rotRad)));
		Double newLon = lonRad - Math.atan2(
			(Math.sin(
				rotRad) * sinD * cosLat),
			(cosD - (sinLat * Math.sin(
				newLat))));

		// Normalize longitude
		// while (newLon > Math.PI) {
		// newLon -= 2.0 * Math.PI;
		// }
		// while (newLon < -1.0 * Math.PI) {
		// newLon += 2.0 * Math.PI;
		// }
		return new Coordinate(
			Math.toDegrees(
				newLon),
			Math.toDegrees(
				newLat));
	}

	// Algorithm taken from Williams Best Aviation Formulary
	// (http://williams.best.vwh.net/avform.htm)
	// d=2*asin(sqrt((sin((lat1-lat2)/2))^2 +
	// cos(lat1)*cos(lat2)*(sin((lon1-lon2)/2))^2))
	// d=2*asin(sqrt((sinlatdiff)^2 +
	// cos(lat1)*cos(lat2)*(sinlondiff)^2))
	public static double distanceBetweenPoints(
		Coordinate originDegrees,
		Coordinate destinationDegrees ) {
		Double lon1 = Math.toRadians(
			originDegrees.x);
		Double lat1 = Math.toRadians(
			originDegrees.y);
		Double lon2 = Math.toRadians(
			destinationDegrees.x);
		Double lat2 = Math.toRadians(
			destinationDegrees.y);
		Double sinlatdiff = Math.sin(
			(lat1 - lat2) / 2.0);
		Double sinlondiff = Math.sin(
			(lon1 - lon2) / 2.0);
		Double d = 2.0 * Math.asin(
			Math.sqrt(
				(sinlatdiff * sinlatdiff) + Math.cos(
					lat1)
					* Math.cos(
						lat2)
					* (sinlondiff * sinlondiff)));

		// convert back to meters
		d *= 6371000.0;

		return d;
	}

	public static Geometry createBoundingBox(
		Coordinate topLeft,
		Coordinate bottomRight ) {
		GeometryFactory factory = new GeometryFactory();
		double top = topLeft.y;
		double left = topLeft.x;
		double bottom = bottomRight.y;
		double right = bottomRight.x;
		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(
			left,
			bottom);
		coordinates[1] = new Coordinate(
			left,
			top);
		coordinates[2] = new Coordinate(
			right,
			top);
		coordinates[3] = new Coordinate(
			right,
			bottom);
		coordinates[4] = coordinates[0];
		LinearRing linear = factory.createLinearRing(
			coordinates);
		return new Polygon(
			linear,
			null,
			factory);
	}

	public static Double metersToDegrees(
		Double dist,
		Double lat ) {
		if (lat == null) {
			lat = 0.0;
		}
		final Double EquitorialCircumference = 40075160.0; // meters
		Double LatitudeCircumference = EquitorialCircumference * Math.cos(
			lat * Math.toRadians(
				lat));
		return (dist * 360.0) / LatitudeCircumference;
	}

	// This method will parse a list of coordinate strings to a coordinate array
	// ie: "34.2,14.6 34.3,14.6 34.3,14.7 34.2,14.7 34.2,14.6"
	// ASSUMES lon,lat
	public static Coordinate[] parseCoordinatesDegrees(
		String coordsString )
			throws NumberFormatException {
		String delimiter = "[ ]";
		List<Coordinate> coordList = new ArrayList<Coordinate>();
		String[] coordStrings = coordsString.split(
			delimiter);
		if (coordStrings.length == 0) {
			NumberFormatException nfe = new NumberFormatException(
				"No coordinates encountered.");
			throw nfe;
		}

		for (String coordString : coordStrings) {
			coordList.add(
				parseCoordinateDegrees(
					coordString));
		}
		return coordList.toArray(
			new Coordinate[coordList.size()]);
	}

	// This method will parse a list of coordinate strings to a coordinate array
	// ie: "34.2,14.6 34.3,14.6 34.3,14.7 34.2,14.7 34.2,14.6"
	// ASSUMES lon,lat
	public static Coordinate[] parseCoordinatesRadians(
		String coordsString )
			throws NumberFormatException {
		String delimiter = "[ ]";
		List<Coordinate> coordList = new ArrayList<Coordinate>();
		String[] coordStrings = coordsString.split(
			delimiter);
		if (coordStrings.length == 0) {
			NumberFormatException nfe = new NumberFormatException(
				"No coordinates encountered.");
			throw nfe;
		}

		for (String coordString : coordStrings) {
			coordList.add(
				parseCoordinateRadians(
					coordString));
		}
		return coordList.toArray(
			new Coordinate[coordList.size()]);
	}

	// NOTE: Coordinate parsing expects the strings in LONGITUDE,LATITUDE order.
	public static Coordinate parseCoordinateDegrees(
		String coordString )
			throws NumberFormatException {
		Coordinate val = parseCoordinate(
			coordString);

		// Check Lat
		if (val.y > 90.0) {
			NumberFormatException nfe = new NumberFormatException(
				"Latitude cannot be greater than 90");
			throw nfe;
		}
		else if (val.y < -90.0) {
			NumberFormatException nfe = new NumberFormatException(
				"Latitude cannot be less than -90");
			throw nfe;
		}
		// normalize longitude
		while (val.x > 180.0) {
			val.x -= 360.0;
		}
		while (val.x < -180.0) {
			val.x += 360.0;
		}

		return val;
	}

	public static Coordinate parseCoordinateRadians(
		String coordString )
			throws NumberFormatException {
		Coordinate val = parseCoordinate(
			coordString);

		// Check Lat
		if (val.y > Math.PI) {
			NumberFormatException nfe = new NumberFormatException(
				"Latitude cannot be greater than PI");
			throw nfe;
		}
		else if (val.y < -1 * Math.PI) {
			NumberFormatException nfe = new NumberFormatException(
				"Latitude cannot be less than -PI");
			throw nfe;
		}
		// normalize longitude
		while (val.x > 180.0) {
			val.x -= 360.0;
		}
		while (val.x < -180.0) {
			val.x += 360.0;
		}

		return val;
	}

	public static Coordinate parseCoordinate(
		String coordString )
			throws NumberFormatException {
		String delimiter = "[,]";
		String[] components = coordString.split(
			delimiter);
		if (components.length != 2) {
			NumberFormatException nfe = new NumberFormatException(
				"Need X and Y coordinates, received \'" + coordString + "\'");
			throw nfe;
		}
		Double lon = Double.parseDouble(
			components[0]);
		Double lat = Double.parseDouble(
			components[1]);
		return new Coordinate(
			lon,
			lat);
	}

	public static Geometry flipGeometryXY(
		Geometry geom )
			throws GeometryParseException {
		Coordinate[] geometryCoords = geom.getCoordinates();
		if (geometryCoords.length == 1) {
			GeometryParseException gpe = new GeometryParseException(
				"Single point geometry detected.");
			throw gpe;
		}
		for (int i = 0; i < geometryCoords.length; i++) {
			geometryCoords[i] = flipCoordinateXY(
				geometryCoords[i]);
		}
		GeometryFactory factory = new GeometryFactory();
		LinearRing linear = factory.createLinearRing(
			geometryCoords);
		return new Polygon(
			linear,
			null,
			factory);
	}

	public static Coordinate flipCoordinateXY(
		Coordinate coord ) {
		double temp = coord.x;
		coord.x = coord.y;
		coord.y = temp;
		return coord;
	}

	public static Geometry extractGeometry(
		String centerPoint,
		Double radius,
		String upperLeft,
		String bottomRight )
			throws GeometryParseException {
		Geometry geometry = null;
		if (centerPoint != null && radius != null) {
			Coordinate center = GeometryUtils.parseCoordinateDegrees(
				centerPoint);
			geometry = GeometryUtils.createGeoCircle(
				center,
				radius);
			// geometry = GeometryUtils.createCircle(center,
			// GeometryUtils.metersToDegrees(radius, center.y));
		}
		else if (upperLeft != null && bottomRight != null) {
			Coordinate NW = GeometryUtils.parseCoordinateDegrees(
				upperLeft);
			Coordinate SE = GeometryUtils.parseCoordinateDegrees(
				bottomRight);
			geometry = GeometryUtils.createBoundingBox(
				NW,
				SE);
		}
		else {
			// everything null ... whole world
			Coordinate NW = new Coordinate(
				-180,
				90);
			Coordinate SE = new Coordinate(
				180,
				-90);
			geometry = GeometryUtils.createBoundingBox(
				NW,
				SE);
		}
		return geometry;
	}

	public static Geometry extractGeometry(
		String geoJSON )
			throws GeometryParseException {
		Geometry geometry = null;

		if (geoJSON.equals(
			"{}")) {
			// everything null ... whole world
			Coordinate NW = new Coordinate(
				-180,
				90);
			Coordinate SE = new Coordinate(
				180,
				-90);
			geometry = GeometryUtils.createBoundingBox(
				NW,
				SE);
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(
				new GeoJsonModule());
			JsonNode node = null;
			try {
				node = mapper.readTree(
					geoJSON);
				geometry = mapper.treeToValue(
					node,
					Geometry.class);
			}
			catch (JsonProcessingException jpe) {
				GeometryParseException gpe = new GeometryParseException(
					"Failed to process the supplied GeoJSON",
					jpe);
				throw gpe;
			}
			catch (IOException ioe) {
				GeometryParseException gpe = new GeometryParseException(
					"Input stream closed unexpectedly",
					ioe);
				throw gpe;
			}
		}

		return geometry;
	}

	public static Geometry convertGeometryRadiansToDegrees(
		Geometry radGeo )
			throws GeometryParseException {
		final double epsilonRad = 0.00000785; // ~50 meters

		Coordinate[] geometryCoords = radGeo.getCoordinates();
		Coordinate[] radCoords = null;

		// handle single point geometry (invalid)
		if (geometryCoords.length == 1) {
			// add 50 meters to each side
			//
			// 2-----3
			// | X |
			// 1-----4
			//

			radCoords = new Coordinate[5];
			radCoords[0] = new Coordinate(
				geometryCoords[0].x - epsilonRad,
				geometryCoords[0].y - epsilonRad);
			radCoords[1] = new Coordinate(
				geometryCoords[0].x - epsilonRad,
				geometryCoords[0].y + epsilonRad);
			radCoords[2] = new Coordinate(
				geometryCoords[0].x + epsilonRad,
				geometryCoords[0].y + epsilonRad);
			radCoords[3] = new Coordinate(
				geometryCoords[0].x + epsilonRad,
				geometryCoords[0].y - epsilonRad);
			radCoords[4] = radCoords[0];
		}
		else {
			radCoords = geometryCoords;
		}
		Coordinate[] degCoords = new Coordinate[radCoords.length];
		for (int i = 0; i < radCoords.length; i++) {
			Coordinate degCoord = new Coordinate(
				Math.toDegrees(
					radCoords[i].x),
				Math.toDegrees(
					radCoords[i].y));
			degCoords[i] = degCoord;
		}
		try {
			GeometryFactory factory = new GeometryFactory();
			LinearRing linear = factory.createLinearRing(
				degCoords);
			return new Polygon(
				linear,
				null,
				factory);
		}
		catch (Exception e) {
			GeometryParseException gpe = new GeometryParseException(
				"Could not create a polygon",
				e);
			throw gpe;
		}
	}

	public static List<Geometry> getWrappableGeometry(
		Geometry geometry ) {
		// check to see if points wrap around the international date line
		Coordinate[] firstCoords = geometry.getCoordinates();
		boolean wrapsEast = false;
		boolean wrapsWest = false;
		for (Coordinate coord : firstCoords) {
			if (coord.x < -180.0) {
				wrapsWest = true;
			}
			if (coord.x > 180.0) {
				wrapsEast = true;
			}
		}

		if (!wrapsEast && !wrapsWest) {
			return Arrays.asList(
				geometry);
		}

		double shift = 0;

		if (wrapsEast) {
			shift = -360.0;
		}
		if (wrapsWest) {
			shift = 360.0;
		}
		Coordinate[] secondCoords = new Coordinate[firstCoords.length];
		for (int i = 0; i < firstCoords.length; i++) {
			Coordinate shiftedCoord = new Coordinate(
				firstCoords[i].x + shift,
				firstCoords[i].y);
			secondCoords[i] = shiftedCoord;
		}

		Geometry secondGeometry = null;
		try {
			GeometryFactory factory = new GeometryFactory();
			LinearRing linear = factory.createLinearRing(
				secondCoords);
			secondGeometry = new Polygon(
				linear,
				null,
				factory);
		}
		catch (Exception e) {
			// TODO: log error
			// couldn't create the second geometry for some reason.
			return Arrays.asList(
				geometry);
		}

		return Arrays.asList(
			geometry,
			secondGeometry);
	}

	public static Geometry convertGeometryDegreesToRadians(
		Geometry degGeo )
			throws GeometryParseException {
		final double epsilonDeg = 0.0004498; // ~50 meters

		Coordinate[] geometryCoords = degGeo.getCoordinates();
		Coordinate[] degCoords = null;

		// handle single point geometry (invalid)
		if (geometryCoords.length == 1) {
			// add 50 meters to each side
			//
			// 2-----3
			// | X |
			// 1-----4
			//

			degCoords = new Coordinate[5];
			degCoords[0] = new Coordinate(
				geometryCoords[0].x - epsilonDeg,
				geometryCoords[0].y - epsilonDeg);
			degCoords[1] = new Coordinate(
				geometryCoords[0].x - epsilonDeg,
				geometryCoords[0].y + epsilonDeg);
			degCoords[2] = new Coordinate(
				geometryCoords[0].x + epsilonDeg,
				geometryCoords[0].y + epsilonDeg);
			degCoords[3] = new Coordinate(
				geometryCoords[0].x + epsilonDeg,
				geometryCoords[0].y - epsilonDeg);
			degCoords[4] = degCoords[0];
		}
		else {
			degCoords = geometryCoords;
		}
		Coordinate[] radCoords = new Coordinate[degCoords.length];
		for (int i = 0; i < degCoords.length; i++) {
			Coordinate radCoord = new Coordinate(
				Math.toRadians(
					degCoords[i].x),
				Math.toRadians(
					degCoords[i].y));
			radCoords[i] = radCoord;
		}
		try {
			GeometryFactory factory = new GeometryFactory();
			LinearRing linear = factory.createLinearRing(
				radCoords);
			return new Polygon(
				linear,
				null,
				factory);
		}
		catch (Exception e) {
			GeometryParseException gpe = new GeometryParseException(
				"Could not create a polygon",
				e);
			throw gpe;
		}
	}

	public static Point createPointFromLatLon(
		double lat,
		double lon ) {
		return G.createPoint(
			new Coordinate(
				lon,
				lat));
	}

	public static Polygon getPolygonFromCoords(
		Coordinate[] coords ) {
		return G.createPolygon(
			coords);
	}

	public static Polygon getPolygonFromPoint3dList_xyFlip(
		List<Point3d> points ) {
		Coordinate[] coords = new Coordinate[points.size()];
		for (int i = 0; i < coords.length; i++) {
			coords[i] = new Coordinate(
				Math.toDegrees(
					points.get(
						i).getY()),
				Math.toDegrees(
					points.get(
						i).getX()),
				Math.toDegrees(
					points.get(
						i).getZ()));
		}
		return G.createPolygon(
			coords);
	}
}
