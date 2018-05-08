package cn.portal.esb.coproc.model;

import java.util.Date;

public class NodeLatencyModel {

	private long id;
	private int ratio;
	private int latency;
	private Date time;
	private long nodeID;
	private long dataCenterID;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getRatio() {
		return ratio;
	}

	public void setRatio(int ratio) {
		this.ratio = ratio;
	}

	public int getLatency() {
		return latency;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public long getNodeID() {
		return nodeID;
	}

	public void setNodeID(long nodeID) {
		this.nodeID = nodeID;
	}

	public long getDataCenterID() {
		return dataCenterID;
	}

	public void setDataCenterID(long dataCenterID) {
		this.dataCenterID = dataCenterID;
	}

}
