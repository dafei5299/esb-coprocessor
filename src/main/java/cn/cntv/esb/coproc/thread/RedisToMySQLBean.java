package cn.portal.esb.coproc.thread;

/**
 * RedisToMySQLBean类功能：作为Redis中JSON数据的bean Author：liweichao
 */
public class RedisToMySQLBean {

	private String Api;
	private long RedirectTime;
	private long RequestTime;
	private long RecordTime;
	private int Status;
	private long ResponseTime;
	private String TargetIP;
	private int TargetPort;
	private long Bandwidth;
	private String System;
	private String Appkey;
	private String Group;

	public String getApi() {
		return Api;
	}

	public void setApi(String api) {
		Api = api;
	}

	public long getRedirectTime() {
		return RedirectTime;
	}

	public void setRedirectTime(long redirectTime) {
		RedirectTime = redirectTime;
	}

	public long getRequestTime() {
		return RequestTime;
	}

	public void setRequestTime(long requestTime) {
		RequestTime = requestTime;
	}

	public long getRecordTime() {
		return RecordTime;
	}

	public void setRecordTime(long recordTime) {
		RecordTime = recordTime;
	}

	public int getStatus() {
		return Status;
	}

	public void setStatus(int status) {
		Status = status;
	}

	public long getResponseTime() {
		return ResponseTime;
	}

	public void setResponseTime(long responseTime) {
		ResponseTime = responseTime;
	}

	public String getTargetIP() {
		return TargetIP;
	}

	public void setTargetIP(String targetIP) {
		TargetIP = targetIP;
	}

	public int getTargetPort() {
		return TargetPort;
	}

	public void setTargetPort(int targetPort) {
		TargetPort = targetPort;
	}

	public long getBandwidth() {
		return Bandwidth;
	}

	public void setBandwidth(long bandwidth) {
		Bandwidth = bandwidth;
	}

	public String getSystem() {
		return System;
	}

	public void setSystem(String system) {
		System = system;
	}

	public String getAppkey() {
		return Appkey;
	}

	public void setAppkey(String appkey) {
		Appkey = appkey;
	}

	public String getGroup() {
		return Group;
	}

	public void setGroup(String group) {
		Group = group;
	}

}
