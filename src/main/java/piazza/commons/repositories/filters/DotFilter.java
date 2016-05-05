package piazza.commons.repositories.filters;

import java.util.HashMap;
import java.util.List;

import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DotFilter
{
	private final static Logger log = LoggerFactory.getLogger(DotFilter.class);
	
	public static FilterBuilder extractDotFilter(List<String> metadataList)
	{
		HashMap<String, String[]> metadataQueryMap = new HashMap<String, String[]>();
		
		if (metadataList != null) {
			for (String metadata : metadataList) {
				String[] metadataQuery = metadata.trim().split(";");
				metadataQueryMap.put(metadataQuery[0], new String[] { metadataQuery[1], metadataQuery[2] });
			}
		}
		
		return extractDotFilter(metadataQueryMap);
	}
	
	public static FilterBuilder extractDotFilter(HashMap<String, String[]> metadataQueryMap)
	{
		AndFilterBuilder filter = FilterBuilders.andFilter();
		
		boolean dotFiltersDefined = false;
		
		for(String key : metadataQueryMap.keySet())
		{
			if(key.equals("radialVelocity") || key.equals("radarCrossSection") || key.equals("signalToNoise"))
			{
				FilterBuilder convertedFilter = convertRelationToFilter(key, metadataQueryMap.get(key));
				
				if(convertedFilter != null)
				{
					filter.add(convertedFilter);
					dotFiltersDefined = true;
				}
				
			}
		}
		
		if(!dotFiltersDefined)
			filter = null;
		
		return filter;
	}
	
	private static FilterBuilder convertRelationToFilter(String key, String[] relationValuePair) {
		BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();

		try {

			String relation = relationValuePair[0];
			Integer value = Integer.parseInt(relationValuePair[1]);

			switch (relation) {
			case "gt":
				boolFilter.must(FilterBuilders.rangeFilter(key).gt(value));
				break;

			case "gte":
				boolFilter.must(FilterBuilders.rangeFilter(key).gte(value));
				break;

			case "lt":
				boolFilter.must(FilterBuilders.rangeFilter(key).lt(value));
				break;

			case "lte":
				boolFilter.must(FilterBuilders.rangeFilter(key).lte(value));
				break;

			case "eq":
				boolFilter.must(FilterBuilders.termFilter(key, value));
				break;

			case "noteq":
				boolFilter.mustNot(FilterBuilders.termFilter(key, value));
				break;

			default:
				boolFilter = null;
				break;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			boolFilter = null;
		}

		return boolFilter;

	}
}