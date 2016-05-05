package piazza.commons.repositories.filters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

public class TimeFilter {
	private static String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	
	public static FilterBuilder getTimeRangeFilter(String startTimeFieldName, String stopTimeFieldName,
			Date startTime, Date stopTime) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String startStr = sdf.format(startTime.getTime());
		String stopStr = sdf.format(stopTime.getTime());
		return FilterBuilders.andFilter(FilterBuilders.rangeFilter(stopTimeFieldName).gte(startStr),
										FilterBuilders.rangeFilter(startTimeFieldName).lte(stopStr));
	}
	
	public static FilterBuilder getTimeRangeFilter(String startTimeFieldName, String stopTimeFieldName,
			Date time) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String timeStr = sdf.format(time.getTime());
		return FilterBuilders.andFilter(FilterBuilders.rangeFilter(stopTimeFieldName).gte(timeStr),
										FilterBuilders.rangeFilter(startTimeFieldName).lte(timeStr));
	}
	
	public static FilterBuilder getTimeRangeFilter(String timeFieldName,
			Date startTime, Date stopTime) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String startStr = sdf.format(startTime.getTime());
		String stopStr = sdf.format(stopTime.getTime());
		return FilterBuilders.andFilter(FilterBuilders.rangeFilter(timeFieldName).from(startStr).to(stopStr));
	}
}
