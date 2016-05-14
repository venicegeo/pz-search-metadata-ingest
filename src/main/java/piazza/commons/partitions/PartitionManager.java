package piazza.commons.partitions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
//import org.elasticsearch.index.query.AndFilterBuilder;
//import org.elasticsearch.index.query.FilterBuilders.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import piazza.commons.elasticsearch.NativeElasticsearchTemplate;
//import piazza.commons.repositories.filters.TimeFilter;

public class PartitionManager {
	
	public static String index = "partitions";
	public static String type = "partition";

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private NativeElasticsearchTemplate elasticsearchTemplate;

	private PartitionType partitionType;

	public PartitionManager(PartitionType partitionType) {
		this.partitionType = partitionType;
	}
	
	//TEMP!!!!!
	public String getPartition(Date date) throws Exception {
		return ("dummy placeholder for migration to ES 2.X");
	}
/* css 5/14/16  migreate to ES 2.X  A MESS!!!!!!!
	public String getPartition(Date date) throws Exception {
		
		String partitionName = null;

		RangeQueryBuilder rqb = QueryBuilders.rangeQuery("time").to("stopTime").from("startTime");
		QueryBuilders filter = QueryBuilders.andFilter(TimeFilter.getTimeRangeFilter("startTime", "stopTime", date),
				QueryBuilders.termQuery("type", partitionType.toString().toLowerCase();
		
		SearchRequestBuilder searchQuery = elasticsearchTemplate.NativeSearchQueryBuilder().setIndices(index).setTypes(type)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));

		Partition partition = elasticsearchTemplate.queryForOne(searchQuery, Partition.class);

		if (partition != null) 
		{	
			partitionName = partition.getIndexName();		
		} 
		else
		{
			Partition p = new Partition();
			p.setIndexName(getIndexName(date));
			p.setType(partitionType.toString().toLowerCase());
			p.setStartTime(getStartTime(date));
			p.setStopTime(getStopTime(date));

			try
			{
				elasticsearchTemplate.createIndex(p.getIndexName());
				elasticsearchTemplate.index(index, type, p);
				elasticsearchTemplate.createAlias(p.getIndexName(), partitionType.toString().toLowerCase());
				elasticsearchTemplate.refresh(index);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
			
			partitionName = p.getIndexName();
			
		} 

		return partitionName;
	}
	
	public String[] getPartitions(Date startDate, Date stopDate)
	{	
		AndFilterBuilder filter = FilterBuilders.andFilter(TimeFilter.getTimeRangeFilter("startTime", "stopTime", startDate, stopDate),
				FilterBuilders.termFilter("type", partitionType.toString().toLowerCase()));

		SearchRequestBuilder searchQuery = elasticsearchTemplate.NativeSearchQueryBuilder().setIndices(index).setTypes(type)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter));

		List<Partition> partitions = elasticsearchTemplate.queryForList(searchQuery, Partition.class);
		
		List<String> partitionNames = new ArrayList<String>();
		
		for(Partition p : partitions)
			partitionNames.add(p.getIndexName());
		
		return partitionNames.toArray(new String[partitions.size()]);
	}
*/	
	public String getAlias() {
		return this.partitionType.toString().toLowerCase();
	}

	private String getIndexName(Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		return String.format("%s_y%d_%02d", partitionType.toString().toLowerCase(), year, month + 1);
	}

	private Date getStartTime(Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
		return cal.getTime();
	}

	private Date getStopTime(Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
		return cal.getTime();
	}

}
