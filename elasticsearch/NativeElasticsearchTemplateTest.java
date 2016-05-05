package mti.commons.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import mti.commons.date.DateParser;
import mti.commons.exception.DateTimeParseException;
import mti.commons.repositories.filters.TimeFilter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = NativeElasticsearchTemplateConfiguration.class)
@TestPropertySource(properties = { "elasticsearch.clustername = P422-elasticsearch",
		"elasticsearch.hostname = 10.0.0.16", "elasticsearch.port = 9300" })
public class NativeElasticsearchTemplateTest {

	public static class MockTrack implements ESModel {
		String id;
		String uuid;
		String comment;
		Date startTime;
		Date stopTime;

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public Date getStartTime() {
			return startTime;
		}

		public void setStartTime(Date startTime) {
			this.startTime = startTime;
		}

		public Date getStopTime() {
			return stopTime;
		}

		public void setStopTime(Date stopTime) {
			this.stopTime = stopTime;
		}
	}

	@Autowired
	NativeElasticsearchTemplate template;
	
	DateParser dateParser = DateParser.newInstance();
	
	static final String INDEX = "track";
	static final String TYPE = "track";

	@Test
	public void test() {
		// simple test model
		MockTrack track = new MockTrack();
		track.setUuid(UUID.randomUUID().toString());
		track.setComment("This is a test");

		// id is null when created
		assertNull(track.getId());
		assertTrue(template.index(INDEX, TYPE, track));
		
		// not null after created
		assertNotNull(track.getId());
		
		// search for the document
		MockTrack result = template.findOne(INDEX, TYPE, track.getId(), MockTrack.class);
		assertNotNull(result);
		assertEquals(track.getId(), result.getId());
		assertEquals(track.getUuid(), result.getUuid());
		assertEquals(track.getComment(), result.getComment());
		
		// change the comment and save
		result.setComment("This has changed");
		template.index(INDEX, TYPE, result);
		
		// get the changed document
		MockTrack changed = template.findOne(INDEX, TYPE, track.getId(), MockTrack.class);
		assertNotNull(changed);
		assertEquals(track.getId(), changed.getId());
		assertEquals(track.getUuid(), changed.getUuid());
		assertNotEquals(track.getComment(), changed.getComment());
		assertEquals(result.getComment(), changed.getComment());
		
		// delete the original document
		assertTrue(template.delete(INDEX, TYPE, track));
		// cant delete it again
		assertFalse(template.delete(INDEX, TYPE, track));		
		// can't get it now
		assertNull(template.findOne(INDEX, TYPE, track.getId(), MockTrack.class));
		
		// get a null with a bad id
		assertNull(template.findOne(INDEX, TYPE, "BAD_ID", MockTrack.class));	
	}

	@Test
	public void testBulk() {
		final int NUM_TRACKS = 100;
		List<MockTrack> tracks = new ArrayList<>(NUM_TRACKS);
		
		for (int i = 0; i < NUM_TRACKS; ++i) {
			MockTrack track = new MockTrack();
			track.setUuid(UUID.randomUUID().toString());
			track.setComment(String.format("Comment %d", i));
			tracks.add(track);
		}
		
		template.bulkIndex(INDEX, TYPE, tracks);
		
		for (int i = 0; i < NUM_TRACKS; ++i) {
			MockTrack track = tracks.get(i);
			assertNotNull(track.getId());
			
			MockTrack found = template.findOne(INDEX, TYPE, track.getId(), MockTrack.class);
			assertNotNull(found);
			assertEquals(track.getUuid(), found.getUuid());
			assertEquals(track.getComment(), found.getComment());
			
			template.delete(INDEX, TYPE, found);
		}
	}
	
	@Test
	public void testQuery() throws DateTimeParseException, InterruptedException {
		final int NUM_TRACKS = 30;
		List<MockTrack> tracks = new ArrayList<>(NUM_TRACKS);
		
		for (int i = 0; i < NUM_TRACKS; ++i) {
			MockTrack track = new MockTrack();
			track.setUuid(UUID.randomUUID().toString());
			track.setComment(String.format("Comment %d", i));
			track.setStartTime(dateParser.parseDate(String.format("2000-01-%02dT11:00:00", i + 1)));
			track.setStopTime(dateParser.parseDate(String.format("2000-01-%02dT12:00:00", i + 1)));
			tracks.add(track);
		}
		
		template.bulkIndex(INDEX, TYPE, tracks);
		
		// force indexing to complete
		template.refresh(INDEX);
		
		Date start = dateParser.parseDate("2000-01-10T00:00:00");
		Date end = dateParser.parseDate("2000-01-20T00:00:00");;
		AndFilterBuilder preFilter = FilterBuilders
				.andFilter(TimeFilter.getTimeRangeFilter("startTime", "stopTime", start, end));
		SearchRequestBuilder searchQuery = template.NativeSearchQueryBuilder().setIndices(INDEX).setTypes(TYPE)
				.addSort("startTime", SortOrder.ASC)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), preFilter));
		
		List<MockTrack> results = template.queryForList(searchQuery, MockTrack.class);
		
		assertNotNull(results);
		assertEquals(10, results.size());
		
		for (int i = 0; i < 10; ++i) {
			MockTrack original = tracks.get(i + 9);
			MockTrack found = results.get(i);
			assertEquals(original.getId(), found.getId());
			assertEquals(original.getUuid(), found.getUuid());
			assertEquals(original.getStartTime(), found.getStartTime());
			assertEquals(original.getStopTime(), found.getStopTime());
		}
		
		for (MockTrack track: tracks) {
			template.delete(INDEX, TYPE, track);
		}
	}
}
