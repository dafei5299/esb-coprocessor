package cn.portal.esb.coproc.model;

import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class BackupModel implements Comparable<BackupModel> {

	private Date timestamp;
	private String size;
	private String md5;
	private String type;
	private long fromLsn;
	private long toLsn;
	private boolean binlogInfo;
	private int elapsed;

	public static BackupModel fromMap(Map<String, Object> map) {
		BackupModel backup = new BackupModel();
		String timestamp = (String) map.get("timestamp");
		backup.setTimestamp(timestamp);
		String size = (String) map.get("size");
		backup.setSize(size);
		String md5 = (String) map.get("md5");
		backup.setMd5(md5);
		String type = (String) map.get("type");
		backup.setType(type);
		String fromLsn = (String) map.get("from_lsn");
		backup.setFromLsn(Long.parseLong(fromLsn));
		String toLsn = (String) map.get("to_lsn");
		backup.setToLsn(Long.parseLong(toLsn));
		String binlogInfo = (String) map.get("binlog_info");
		backup.setBinlogInfo("yes".equals(binlogInfo));
		int elapsed = (int) map.get("elapsed");
		backup.setElapsed(elapsed);
		return backup;
	}

	public boolean isBefore(long time) {
		return new DateTime(timestamp).isBefore(time);
	}

	public boolean isAfter(long time) {
		return new DateTime(timestamp).isAfter(time);
	}

	public String getTimestamp() {
		return new DateTime(timestamp).toString("YYYY-MM-dd_HH-mm-ss");
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = DateTime.parse(timestamp,
				DateTimeFormat.forPattern("YYYY-MM-dd_HH-mm-ss")).toDate();
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getFromLsn() {
		return fromLsn;
	}

	public void setFromLsn(long fromLsn) {
		this.fromLsn = fromLsn;
	}

	public long getToLsn() {
		return toLsn;
	}

	public void setToLsn(long toLsn) {
		this.toLsn = toLsn;
	}

	public boolean isBinlogInfo() {
		return binlogInfo;
	}

	public void setBinlogInfo(boolean binlogInfo) {
		this.binlogInfo = binlogInfo;
	}

	public int getElapsed() {
		return elapsed;
	}

	public void setElapsed(int elapsed) {
		this.elapsed = elapsed;
	}

	@Override
	public int compareTo(BackupModel o) {
		return timestamp.compareTo(o.timestamp);
	}

}
