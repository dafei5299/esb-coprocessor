package cn.portal.esb.coproc.model;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "groups", "nodes" })
public class SystemModel implements Serializable {

	private long id;
	private String name;
	private boolean debug; // 是否允许debug请求
	private String debugKey; // 开启debug，生成的临时key
	private boolean transparent; // 是否允许透明转发
	private String discoverUri; // 目标服务提供的探测健康状态接口
	private boolean autoAdjust; // 是否开启托管模式（自动调节 节点权重、降级延迟）
	private boolean enable; // 人工是否启用该系统
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
	private int concurrencyEstimate; // 本系统需要多少并发量支撑的估算值
	private boolean concurrencyExceeded = false; // 并发量预估值是否超过警戒线
	private List<GroupModel> groups = new CopyOnWriteArrayList<>();
	private List<NodeModel> nodes = new CopyOnWriteArrayList<>();

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

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getDebugKey() {
		return debugKey;
	}

	public void setDebugKey(String debugKey) {
		this.debugKey = debugKey;
	}

	public boolean isTransparent() {
		return transparent;
	}

	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	public String getDiscoverUri() {
		return discoverUri;
	}

	public void setDiscoverUri(String discoverUri) {
		if (discoverUri != null && discoverUri.isEmpty())
			discoverUri = null;
		if (discoverUri != null && !discoverUri.startsWith("/"))
			discoverUri = "/" + discoverUri;
		this.discoverUri = discoverUri;
	}

	public boolean isAutoAdjust() {
		return autoAdjust;
	}

	public void setAutoAdjust(boolean autoAdjust) {
		this.autoAdjust = autoAdjust;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
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

	public int getConcurrencyEstimate() {
		return concurrencyEstimate;
	}

	public void setConcurrencyEstimate(int concurrencyEstimate) {
		this.concurrencyEstimate = concurrencyEstimate;
	}

	public boolean isConcurrencyExceeded() {
		return concurrencyExceeded;
	}

	public void setConcurrencyExceeded(boolean concurrencyExceeded) {
		this.concurrencyExceeded = concurrencyExceeded;
	}

	public void setGroups(List<GroupModel> groups) {
		this.groups = groups;
	}

	public void setNodes(List<NodeModel> nodes) {
		this.nodes = nodes;
	}

	public List<GroupModel> getGroups() {
		return groups;
	}

	public List<NodeModel> getNodes() {
		return nodes;
	}

	public boolean deepEquals(SystemModel prev) {
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
		if (debug != prev.debug)
			return false;
		if (debugKey == null) {
			if (prev.debugKey != null)
				return false;
		} else if (!debugKey.equals(prev.debugKey))
			return false;
		if (transparent != prev.transparent)
			return false;
		// discoverUri
		if (autoAdjust != prev.autoAdjust)
			return false;
		if (enable != prev.enable)
			return false;
		if (shouldDeferred && requestDeferred != prev.requestDeferred)
			return false;
		// deferredTriggerFactor
		if (shouldDeferred != prev.shouldDeferred)
			return false;
		if (debug) {
			if (deferredReason == null) {
				if (prev.deferredReason != null)
					return false;
			} else if (!deferredReason.equals(prev.deferredReason))
				return false;
		}
		if (frequencyLimit != prev.frequencyLimit)
			return false;
		if (debug && frequencyCurrent != prev.frequencyCurrent)
			return false;
		if (frequencyPeriod != prev.frequencyPeriod)
			return false;
		if (throughputLimit != prev.throughputLimit)
			return false;
		if (debug && throughputCurrent != prev.throughputCurrent)
			return false;
		if (throughputPeriod != prev.throughputPeriod)
			return false;
		if (connectionLimit != prev.connectionLimit)
			return false;
		if (debug && connectionCurrent != prev.connectionCurrent)
			return false;
		if (frequencyExceeded != prev.frequencyExceeded)
			return false;
		if (throughputExceeded != prev.throughputExceeded)
			return false;
		if (connectionExceeded != prev.connectionExceeded)
			return false;
		if (concurrencyEstimate != prev.concurrencyEstimate)
			return false;
		if (concurrencyExceeded != prev.concurrencyExceeded)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SystemModel other = (SystemModel) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "System [id=" + id + ", name=" + name + ", debug=" + debug
				+ ", debugKey=" + debugKey + ", transparent=" + transparent
				+ ", discoverUri=" + discoverUri + ", autoAdjust=" + autoAdjust
				+ ", enable=" + enable + ", requestDeferred=" + requestDeferred
				+ ", deferredTriggerFactor=" + deferredTriggerFactor
				+ ", shouldDeferred=" + shouldDeferred + ", deferredReason="
				+ deferredReason + ", frequencyLimit=" + frequencyLimit
				+ ", frequencyCurrent=" + frequencyCurrent
				+ ", frequencyPeriod=" + frequencyPeriod + ", throughputLimit="
				+ throughputLimit + ", throughputCurrent=" + throughputCurrent
				+ ", throughputPeriod=" + throughputPeriod
				+ ", connectionLimit=" + connectionLimit
				+ ", connectionCurrent=" + connectionCurrent
				+ ", frequencyExceeded=" + frequencyExceeded
				+ ", throughputExceeded=" + throughputExceeded
				+ ", connectionExceeded=" + connectionExceeded
				+ ", concurrencyEstimate=" + concurrencyEstimate
				+ ", concurrencyExceeded=" + concurrencyExceeded + "]";
	}

}
