package mti.commons.elasticsearch.dao.trackpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import mti.commons.date.DateParser;
import mti.commons.elasticsearch.dao.track.ElasticsearchTrackPointDAO;
import mti.commons.exception.DateTimeParseException;
import mti.commons.model.track.TrackPoint;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TrackPointDAOTestConfiguration.class)
@TestPropertySource(properties = { "elasticsearch.clustername = P422-elasticsearch", "elasticsearch.hostname = 10.0.0.16",
		"elasticsearch.port = 9300" })
public class TrackPointDAOTest {

	@Autowired
	ElasticsearchTrackPointDAO dao;

	DateParser dateParser = DateParser.newInstance();
	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	Random rand = new Random();

	@Test
	public void testFindOne() throws DateTimeParseException {
		final String trackUuid = UUID.randomUUID().toString();

		TrackPoint tp = createTrackPoint(trackUuid, "2000-06-01T00:00:00");
		dao.refresh();

		try {
			assertNull(dao.findOne("INVALID_UUID"));

			TrackPoint result = dao.findOne(tp.getUuid());
			assertNotNull(result);
			assertEquals(tp.getUuid(), result.getUuid());
		} finally {
			dao.delete(tp);
		}
	}

	@Test
	public void testFindForTrack() throws DateTimeParseException {
		final String trackUuid1 = UUID.randomUUID().toString();
		List<TrackPoint> tps1 = createTrackPoints(trackUuid1, 5, "2000-01-01T12:00:00", "2000-01-01T12:30:00");

		final String trackUuid2 = UUID.randomUUID().toString();
		List<TrackPoint> tps2 = createTrackPoints(trackUuid2, 7, "2000-01-02T12:30:00", "2000-01-02T13:00:00");

		dao.refresh();

		try {
			List<TrackPoint> results = dao.findAllByTrackUuidOrderByTimeAsc(trackUuid1);
			assertNotNull(results);
			assertEquals(tps1.size(), results.size());
			assertTrue(!pointsAreTimeOrdered(tps1));
			assertTrue(pointsAreTimeOrdered(results));
			assertTrue(allPointsFound(tps1, results));

			results = dao.findAllByTrackUuidOrderByTimeAsc(trackUuid2);
			assertNotNull(results);
			assertEquals(tps2.size(), results.size());
			assertTrue(!pointsAreTimeOrdered(tps2));
			assertTrue(pointsAreTimeOrdered(results));
			assertTrue(allPointsFound(tps2, results));

			results = dao.findAllByTrackUuidOrderByTimeAsc("INVALID_UUID");
			assertNotNull(results);
			assertTrue(results.isEmpty());
		} finally {
			dao.delete(tps1);
			dao.delete(tps2);
		}
	}

	private boolean allPointsFound(List<TrackPoint> expected, List<TrackPoint> found) {

		for (TrackPoint e : expected) {
			boolean matched = false;
			for (TrackPoint f : found) {
				if (f.getUuid().equals(e.getUuid()) && f.getTrackUuid().equals(e.getTrackUuid())) {
					matched = true;
					break;
				}
			}

			if (!matched)
				return false;
		}

		return true;
	}

	private boolean pointsAreTimeOrdered(List<TrackPoint> tps) {

		long last = 0L;
		for (TrackPoint tp : tps) {
			if (tp.getTime().getTime() < last)
				return false;
			last = tp.getTime().getTime();
		}

		return true;
	}

	private List<TrackPoint> createTrackPoints(String trackUuid, int count, String start, String stop)
			throws DateTimeParseException {

		List<TrackPoint> tps = new ArrayList<>(count);
		long startMS = dateParser.parseDate(start).getTime();
		long stopMS = dateParser.parseDate(stop).getTime();
		int delta = (int) (stopMS - startMS);

		for (int i = 0; i < count; ++i) {
			TrackPoint tp = new TrackPoint();
			tp.setUuid(UUID.randomUUID().toString());
			tp.setTrackUuid(trackUuid);
			cal.setTimeInMillis(startMS + rand.nextInt(delta));
			tp.setTime(cal.getTime());
			tps.add(dao.save(tp));
		}

		return tps;
	}

	private TrackPoint createTrackPoint(String trackUuid, String time) throws DateTimeParseException {

		TrackPoint tp = new TrackPoint();
		tp.setUuid(UUID.randomUUID().toString());
		tp.setTrackUuid(trackUuid);
		tp.setTime(dateParser.parseDate(time));

		tp = dao.save(tp);

		return tp;
	}

}
