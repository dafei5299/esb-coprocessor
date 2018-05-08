package cn.portal.esb.coproc.model;

import java.util.Date;

import org.joda.time.DateTime;

public class StatisticsModel {

	private String source;
	private String system;
	private int frequency;
	private int throughput;
	private Date time;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getThroughput() {
		return throughput;
	}

	public void setThroughput(int throughput) {
		this.throughput = throughput;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	// 表名后缀，按日分表
	public String getSuffix() {
		return new DateTime(time).toString("YYYYMMdd");
	}

}
