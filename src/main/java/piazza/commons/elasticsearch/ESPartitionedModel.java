package piazza.commons.elasticsearch;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ESPartitionedModel extends ESModel {

	@JsonIgnore
	public Date getPartitionDate();
}
