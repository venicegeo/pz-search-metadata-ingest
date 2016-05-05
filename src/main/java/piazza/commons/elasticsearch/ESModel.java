package piazza.commons.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ESModel {

	@JsonIgnore
	public String getId();
	
	@JsonIgnore
	public void setId(String id);
}
