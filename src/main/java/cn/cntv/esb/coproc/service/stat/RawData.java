package cn.portal.esb.coproc.service.stat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RawData {

	private String system;
	private String group;
	private String api;
	private int version;
	private String source;
	private String remoteAddr;
	private String targetAddr;
	private int targetPort;
	private int statusCode;
	private int contentLength;
	private long inboundTime;
	private long forwardReqTime;
	private long forwardRespTime;
	private long recordTime;

	public String getSystem() {
		return system;
	}

	@JsonProperty("System")
	public void setSystem(String system) {
		this.system = system;
	}

	public String getGroup() {
		return group;
	}

	@JsonProperty("Group")
	public void setGroup(String group) {
		this.group = group;
	}

	public String getApi() {
		return api;
	}

	@JsonProperty("Api")
	public void setApi(String api) {
		this.api = api;
	}

	public int getVersion() {
		return version;
	}

	@JsonProperty("Version")
	public void setVersion(int version) {
		this.version = version;
	}

	public String getSource() {
		return source;
	}

	@JsonProperty("Appkey")
	public void setSource(String source) {
		this.source = source;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	@JsonProperty("RemoteIP")
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public String getTargetAddr() {
		return targetAddr;
	}

	@JsonProperty("TargetIP")
	public void setTargetAddr(String targetAddr) {
		this.targetAddr = targetAddr;
	}

	public int getTargetPort() {
		return targetPort;
	}

	@JsonProperty("TargetPort")
	public void setTargetPort(int targetPort) {
		this.targetPort = targetPort;
	}

	public int getStatusCode() {
		return statusCode;
	}

	@JsonProperty("Status")
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public int getContentLength() {
		return contentLength;
	}

	@JsonProperty("Bandwidth")
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public long getInboundTime() {
		return inboundTime;
	}

	@JsonProperty("RequestTime")
	public void setInboundTime(long inboundTime) {
		this.inboundTime = inboundTime;
	}

	public long getForwardReqTime() {
		return forwardReqTime;
	}

	@JsonProperty("RedirectTime")
	public void setForwardReqTime(long forwardReqTime) {
		this.forwardReqTime = forwardReqTime;
	}

	public long getForwardRespTime() {
		return forwardRespTime;
	}

	@JsonProperty("ResponseTime")
	public void setForwardRespTime(long forwardRespTime) {
		this.forwardRespTime = forwardRespTime;
	}

	public long getRecordTime() {
		return recordTime;
	}

	@JsonProperty("RecordTime")
	public void setRecordTime(long recordTime) {
		this.recordTime = recordTime;
	}

}
