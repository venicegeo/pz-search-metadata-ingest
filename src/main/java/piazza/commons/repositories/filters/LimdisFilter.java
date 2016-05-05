package piazza.commons.repositories.filters;

import java.util.List;

import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

public class LimdisFilter {
	public static FilterBuilder getDotMaskFilter(Short mask) {
		return FilterBuilders.scriptFilter("(doc['dotMask'].value & mask) == doc['dotMask'].value").addParam("mask", mask);
	}
	
	public static FilterBuilder getLimdisFilter(List<String> groups) {	
		
		AndFilterBuilder limdisFilter = FilterBuilders.andFilter();
		
		if(!groups.contains("LIMDIS_DOMESTIC"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.domestic", false)));
		if(!groups.contains("LIMDIS_NORTHCOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.northcom", false)));
		if(!groups.contains("LIMDIS_SOUTHCOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.southcom", false)));
		if(!groups.contains("LIMDIS_EUCOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.eucom", false)));
		if(!groups.contains("LIMDIS_AFRICOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.africom", false)));
		if(!groups.contains("LIMDIS_CENTCOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.centcom", false)));
		if(!groups.contains("LIMDIS_PACOM"))
			limdisFilter.add(FilterBuilders.nestedFilter("limdis", FilterBuilders.termFilter("limdis.pacom", false)));
		
		return limdisFilter;
	}
}
