package mti.commons.elasticsearch.dao.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import mti.commons.date.DateParser;
import mti.commons.elasticsearch.dao.track.ElasticsearchTrackDAO;
import mti.commons.exception.DateTimeParseException;
import mti.commons.model.track.Track;
import mti.commons.util.GeometryUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TrackDAOTestConfiguration.class)
@TestPropertySource(properties = { "elasticsearch.clustername = P422-elasticsearch", "elasticsearch.hostname = 10.0.0.16",
"elasticsearch.port = 9300" })
public class TrackDAOTest {

	@Autowired
	ElasticsearchTrackDAO dao;

	DateParser dateParser = DateParser.newInstance();

	@Test
	public void testFindOne() throws DateTimeParseException {
		assertNotNull(dao);

		Track track = createTrack("2000-01-01T00:00:00", "2000-01-01T00:00:00", null);
		dao.refresh();

		try {
			Track result = dao.findOne(track.getUuid());
			assertNotNull(result);
			assertEquals(result.getUuid(), track.getUuid());
		} finally {
			if (track != null)
				dao.delete(track);
		}
	}

	@Test
	public void testFindAll() throws DateTimeParseException {
		String startTrack = "2000-01-01T00:00:00";
		String stopTrack = "2000-01-01T00:01:00";
		String[] trackPoints = { "44.0, 36.0", "44.1, 36.0", "44.1, 36.1", "44.2, 36.2" };
		Track track = createTrack(startTrack, stopTrack, trackPoints);
		dao.refresh();

		try {
			Date start;
			Date end;
			List<Geometry> geometries = new ArrayList<>(10);
			int pageNumber = 0;
			int pageSize = 1000;
			List<Track> results;

			// no overlap
			start = dateParser.parseDate("2001-01-01T00:00:00");
			end = dateParser.parseDate("2002-01-01T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("40,30"),
					GeometryUtils.parseCoordinateDegrees("30,20")));

			results = dao.findAllMatching(start, end, geometries, pageNumber, pageSize);
			assertNotNull(results);
			assertTrue(results.isEmpty());

			// overlap temporal but not spatial
			start = dateParser.parseDate("1999-01-01T00:00:00");
			end = dateParser.parseDate("2002-01-01T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("40,30"),
					GeometryUtils.parseCoordinateDegrees("30,20")));

			results = dao.findAllMatching(start, end, geometries, pageNumber, pageSize);
			assertNotNull(results);
			assertTrue(results.isEmpty());

			// overlap spatial but not temporal
			start = dateParser.parseDate("2001-01-01T00:00:00");
			end = dateParser.parseDate("2002-01-01T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("40,40"),
					GeometryUtils.parseCoordinateDegrees("50,30")));

			results = dao.findAllMatching(start, end, geometries, pageNumber, pageSize);
			assertNotNull(results);
			assertTrue(results.isEmpty());

			// spatial and temporal overlap
			start = dateParser.parseDate("1999-01-01T00:00:00");
			end = dateParser.parseDate("2002-01-01T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("40,40"),
					GeometryUtils.parseCoordinateDegrees("50,30")));

			results = dao.findAllMatching(start, end, geometries, pageNumber, pageSize);
			assertNotNull(results);
			assertEquals(1, results.size());
			Track result = results.get(0);
			assertEquals(track.getUuid(), result.getUuid());

		} finally {
			if (track != null)
				dao.delete(track);
		}
	}

	@Test
	public void testCount() throws DateTimeParseException {
		Track track1 = createTrack("2000-01-01T00:00:00", "2000-01-01T00:01:00", new String[] { "36, 44", "37, 45" });
		Track track2 = createTrack("2000-01-02T00:00:00", "2000-01-02T00:01:00", new String[] { "37, 45", "38, 46" });
		dao.refresh();

		try {
			Date start;
			Date end;
			List<Geometry> geometries = new ArrayList<>(10);

			// no overlap
			start = dateParser.parseDate("2001-01-01T00:00:00");
			end = dateParser.parseDate("2002-01-01T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("10,10"),
					GeometryUtils.parseCoordinateDegrees("20,0")));

			assertEquals(0L, dao.countAllMatching(start, end, geometries));

			// first overlap
			start = dateParser.parseDate("2000-01-01T00:00:00");
			end = dateParser.parseDate("2000-01-02T00:00:00");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("35.5,44.5"),
					GeometryUtils.parseCoordinateDegrees("36.5,43.5")));

			assertEquals(1L, dao.countAllMatching(start, end, geometries));

			// both overlap
			start = dateParser.parseDate("2000-01-01T00:00:30");
			end = dateParser.parseDate("2000-01-02T00:00:30");
			geometries.clear();
			geometries.add(GeometryUtils.createBoundingBox(GeometryUtils.parseCoordinateDegrees("36.5,45.5"),
					GeometryUtils.parseCoordinateDegrees("37.5,44.5")));

			assertEquals(2L, dao.countAllMatching(start, end, geometries));
		} finally {
			if (track1 != null)
				dao.delete(track1);
			
			if (track2 != null)
				dao.delete(track2);
		}
	}

	@Test
	public void testTrackSet() throws DateTimeParseException {
		final int SET_1_SIZE = 2;
		final int SET_2_SIZE = 3;

		String set1Uuid = UUID.randomUUID().toString();
		List<Track> set1 = createTrackSet(set1Uuid, SET_1_SIZE, "2001-12-31T23:59:59", "2002-01-01T00:59:59");

		String set2Uuid = UUID.randomUUID().toString();
		List<Track> set2 = createTrackSet(set2Uuid, SET_2_SIZE, "2002-12-31T23:59:59", "2003-01-01T00:59:59");
		dao.refresh();

		int pageNumber = 0;
		int pageSize = 1000;

		try {
			List<Track> results;

			// unknown trackset
			results = dao.findAllByTrackSetUuid("UNKNOWN_TRACKSET", pageNumber, pageSize);
			assertNotNull(results);
			assertTrue(results.isEmpty());

			// set 1
			results = dao.findAllByTrackSetUuid(set1Uuid, pageNumber, pageSize);
			compareTrackSets(set1, results);

			// set 2
			results = dao.findAllByTrackSetUuid(set2Uuid, pageNumber, pageSize);
			compareTrackSets(set2, results);

		} finally {
			dao.delete(set1);
			dao.delete(set2);
		}
	}

	private void compareTrackSets(List<Track> expected, List<Track> found) {
		assertNotNull(found);
		assertEquals(expected.size(), found.size());
		String setUuid = expected.get(0).getTrackSetUuid();

		for (Track f : found) {
			assertEquals(setUuid, f.getTrackSetUuid());
			boolean matched = false;
			for (Track e : expected) {
				if (e.getUuid().equals(f.getUuid())) {
					matched = true;
					break;
				}
			}
			assertTrue(matched);
		}
	}

	private List<Track> createTrackSet(String trackSetUuid, int count, String start, String stop) throws DateTimeParseException {

		List<Track> tracks = new ArrayList<>(count);

		for (int i = 0; i < count; ++i) {
			Track track = new Track();
			track.setUuid(UUID.randomUUID().toString());
			track.setTrackSetUuid(trackSetUuid);
			track.setStartTime(dateParser.parseDate(start));
			track.setStopTime(dateParser.parseDate(stop));
			track = dao.save(track);
			tracks.add(track);
		}

		return tracks;
	}

	private Track createTrack(String start, String stop, String[] pathPoints) throws DateTimeParseException {
		Track track = new Track();
		track.setUuid(UUID.randomUUID().toString());

		if (pathPoints != null) {
			Coordinate[] coordinates = new Coordinate[pathPoints.length];
			for (int i = 0; i < pathPoints.length; ++i) {
				coordinates[i] = GeometryUtils.parseCoordinateDegrees(pathPoints[i]);
			}
			
			track.setPath(GeometryUtils.G.createMultiLineString(new LineString[] { GeometryUtils.G.createLineString(coordinates) }));
			track.setSanitizedPath(GeometryUtils.G.createMultiLineString(new LineString[] { GeometryUtils.G.createLineString(coordinates) }));
			track.setPointCount(pathPoints.length);
			track.setSanitizedPointCount(pathPoints.length);
		}

		if (start != null && stop != null) {
			track.setStartTime(dateParser.parseDate(start));
			track.setStopTime(dateParser.parseDate(stop));
		}
		else {
			throw new RuntimeException("Track must have start/stop dates");
		}

		track = dao.save(track);
		return track;
	}

}
