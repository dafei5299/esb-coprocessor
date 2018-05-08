package cn.portal.esb.coproc.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "group" })
public class ApiModel implements Serializable {

	public static enum HttpMethod {
		GET, POST
	}

	private long id;
	private String name;
	private String uri;
	private HttpMethod method;
	private int timeout;
	private int version;
	private boolean defaultVersion;
	private boolean injectionDetection;
	private boolean enable; // 人工是否启用该分组
	private boolean test; // 在线测试开关
	private int requestDeferred; // 降级策略，请求延迟的毫秒数
	private int deferredTriggerFactor; // 触发降级延迟的系数
	private boolean shouldDeferred = false; // 自动降级
	private String deferredReason = null; // 降级原因
	private long frequencyLimit; // 频度限制 计数（次数）
	private long frequencyCurrent = 0; // 频度当前 计数（次数）
	private int frequencyPeriod; // 频度限制 时间周期（秒）
	private long throughputLimit; // 流量限制 计数（字节）
	private long throughputCurrent = 0; // 流量当前 计数（字节）
	private int throughputPeriod; // 流量限制 时间周期（秒）
	private int connectionLimit; // 每秒并发连接数限制
	private int connectionCurrent = 0; // 每秒并发连接数当前
	private boolean frequencyExceeded = false; // 是否超过频度限制
	private boolean throughputExceeded = false; // 是否超过流量限制
	private boolean connectionExceeded = false; // 是否超过连接数限制
	private List<ApiParamModel> params;
	private long groupID;
	private GroupModel group;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public boolean isDefaultVersion() {
		return defaultVersion;
	}

	public void setDefaultVersion(boolean defaultVersion) {
		this.defaultVersion = defaultVersion;
	}

	public boolean isInjectionDetection() {
		return injectionDetection;
	}

	public void setInjectionDetection(boolean injectionDetection) {
		this.injectionDetection = injectionDetection;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public int getRequestDeferred() {
		return requestDeferred;
	}

	public void setRequestDeferred(int requestDeferred) {
		this.requestDeferred = requestDeferred;
	}

	public int getDeferredTriggerFactor() {
		return deferredTriggerFactor;
	}

	public void setDeferredTriggerFactor(int deferredTriggerFactor) {
		this.deferredTriggerFactor = deferredTriggerFactor;
	}

	public boolean isShouldDeferred() {
		return shouldDeferred;
	}

	public void setShouldDeferred(boolean shouldDeferred) {
		this.shouldDeferred = shouldDeferred;
	}

	public String getDeferredReason() {
		return deferredReason;
	}

	public void setDeferredReason(String deferredReason) {
		this.deferredReason = deferredReason;
	}

	public long getFrequencyLimit() {
		return frequencyLimit;
	}

	public void setFrequencyLimit(long frequencyLimit) {
		this.frequencyLimit = frequencyLimit;
	}

	public long getFrequencyCurrent() {
		return frequencyCurrent;
	}

	public void setFrequencyCurrent(long frequencyCurrent) {
		this.frequencyCurrent = frequencyCurrent;
	}

	public int getFrequencyPeriod() {
		return frequencyPeriod;
	}

	public void setFrequencyPeriod(int frequencyPeriod) {
		this.frequencyPeriod = frequencyPeriod;
	}

	public long getThroughputLimit() {
		return throughputLimit;
	}

	public void setThroughputLimit(long throughputLimit) {
		this.throughputLimit = throughputLimit;
	}

	public long getThroughputCurrent() {
		return throughputCurrent;
	}

	public void setThroughputCurrent(long throughputCurrent) {
		this.throughputCurrent = throughputCurrent;
	}

	public int getThroughputPeriod() {
		return throughputPeriod;
	}

	public void setThroughputPeriod(int throughputPeriod) {
		this.throughputPeriod = throughputPeriod;
	}

	public int getConnectionLimit() {
		return connectionLimit;
	}

	public void setConnectionLimit(int connectionLimit) {
		this.connectionLimit = connectionLimit;
	}

	public int getConnectionCurrent() {
		return connectionCurrent;
	}

	public void setConnectionCurrent(int connectionCurrent) {
		this.connectionCurrent = connectionCurrent;
	}

	public boolean isFrequencyExceeded() {
		return frequencyExceeded;
	}

	public void setFrequencyExceeded(boolean frequencyExceeded) {
		this.frequencyExceeded = frequencyExceeded;
	}

	public boolean isThroughputExceeded() {
		return throughputExceeded;
	}

	public void setThroughputExceeded(boolean throughputExceeded) {
		this.throughputExceeded = throughputExceeded;
	}

	public boolean isConnectionExceeded() {
		return connectionExceeded;
	}

	public void setConnectionExceeded(boolean connectionExceeded) {
		this.connectionExceeded = connectionExceeded;
	}

	public List<ApiParamModel> getParams() {
		return params;
	}

	public void setParams(List<ApiParamModel> params) {
		this.params = params;
	}

	public long getGroupID() {
		return groupID;
	}

	public void setGroupID(long groupID) {
		this.groupID = groupID;
	}

	public GroupModel getGroup() {
		return group;
	}

	public void setGroup(GroupModel group) {
		if (this.group != null)
			this.group.getApis().remove(this);
		if (group != null)
			group.getApis().add(this);
		this.group = group;
	}

	public boolean deepEquals(ApiModel prev) {
		if (this == prev)
			return true;
		if (prev == null)
			return false;
		if (id != prev.id)
			return false;
		if (name == null) {
			if (prev.name != null)
				return false;
		} else if (!name.equals(prev.name))
			return false;
		if (uri == null) {
			if (prev.uri != null)
				return false;
		} else if (!uri.equals(prev.uri))
			return false;
		if (method != prev.method)
			return false;
		if (timeout != prev.timeout)
			return false;
		if (version != prev.version)
			return false;
		if (defaultVersion != prev.defaultVersion)
			return false;
		if (injectionDetection != prev.injectionDetection)
			return false;
		if (enable != prev.enable)
			return false;
		if (test != prev.test)
			return false;
		if (shouldDeferred && requestDeferred != prev.requestDeferred)
			return false;
		// deferredTriggerFactor
		if (shouldDeferred != prev.shouldDeferred)
			return false;
		if (group.getSystem().isDebug()) {
			if (deferredReason == null) {
				if (prev.deferredReason != null)
					return false;
			} else if (!deferredReason.equals(prev.deferredReason))
				return false;
		}
		if (frequencyLimit != prev.frequencyLimit)
			return false;
		if (group.getSystem().isDebug()
				&& frequencyCurrent != prev.frequencyCurrent)
			return false;
		if (frequencyPeriod != prev.frequencyPeriod)
			return false;
		if (throughputLimit != prev.throughputLimit)
			return false;
		if (group.getSystem().isDebug()
				&& throughputCurrent != prev.throughputCurrent)
			return false;
		if (throughputPeriod != prev.throughputPeriod)
			return false;
		if (connectionLimit != prev.connectionLimit)
			return false;
		if (group.getSystem().isDebug()
				&& connectionCurrent != prev.connectionCurrent)
			return false;
		if (frequencyExceeded != prev.frequencyExceeded)
			return false;
		if (throughputExceeded != prev.throughputExceeded)
			return false;
		if (connectionExceeded != prev.connectionExceeded)
			return false;
		if (params == null) {
			if (prev.params != null)
				return false;
		} else if (!params.equals(prev.params))
			return false;
		if (groupID != prev.groupID)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApiModel other = (ApiModel) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Api [id=" + id + ", name=" + name + ", uri=" + uri
				+ ", method=" + method + ", timeout=" + timeout + ", version="
				+ version + ", defaultVersion=" + defaultVersion
				+ ", injectionDetection=" + injectionDetection + ", enable="
				+ enable + ", test=" + test + ", requestDeferred="
				+ requestDeferred + ", deferredTriggerFactor="
				+ deferredTriggerFactor + ", shouldDeferred=" + shouldDeferred
				+ ", deferredReason=" + deferredReason + ", frequencyLimit="
				+ frequencyLimit + ", frequencyCurrent=" + frequencyCurrent
				+ ", frequencyPeriod=" + frequencyPeriod + ", throughputLimit="
				+ throughputLimit + ", throughputCurrent=" + throughputCurrent
				+ ", throughputPeriod=" + throughputPeriod
				+ ", connectionLimit=" + connectionLimit
				+ ", connectionCurrent=" + connectionCurrent
				+ ", frequencyExceeded=" + frequencyExceeded
				+ ", throughputExceeded=" + throughputExceeded
				+ ", connectionExceeded=" + connectionExceeded + ", params="
				+ params + ", groupID=" + groupID + "]";
	}

}
