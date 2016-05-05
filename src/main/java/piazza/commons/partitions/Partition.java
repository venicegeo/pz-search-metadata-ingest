package piazza.commons.partitions;

import java.util.Date;

import piazza.commons.elasticsearch.ESModel;

public class Partition implements ESModel {

	private String indexName;
	private Date startTime = null;
	private Date stopTime = null;
	private String type;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getId() {
		return this.indexName;
	}

	@Override
	public void setId(String id) {
		this.indexName = id;
	}

}
