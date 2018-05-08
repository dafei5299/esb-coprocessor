package cn.portal.esb.coproc.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "system", "adjustRatio", "latency" })
public class NodeModel implements Serializable {

	private long id;
	private String host;
	private int port;
	private boolean debug; // 是否允许debug请求路由导该节点
	private int weight; // 人工设置的权重
	private int adjustRatio = 5; // 自动调节系数，在0-10之间波动，原点为5
	private int latency = 0; // 主观探测获得的相对链路延迟，微秒
	private boolean enable; // 人工是否启用该节点
	private long systemID;
	private SystemModel system;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getAdjustRatio() {
		return adjustRatio;
	}

	public void setAdjustRatio(int adjustRatio) {
		this.adjustRatio = adjustRatio;
	}

	public int getLatency() {
		return latency;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public long getSystemID() {
		return systemID;
	}

	public void setSystemID(long systemID) {
		this.systemID = systemID;
	}

	public SystemModel getSystem() {
		return system;
	}

	public void setSystem(SystemModel system) {
		if (this.system != null)
			this.system.getNodes().remove(this);
		if (system != null)
			system.getNodes().add(this);
		this.system = system;
	}

	public boolean deepEquals(NodeModel other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (id != other.id)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (debug != other.debug)
			return false;
		if (weight != other.weight)
			return false;
		if (adjustRatio != other.adjustRatio)
			return false;
		if (enable != other.enable)
			return false;
		if (systemID != other.systemID)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
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
		NodeModel other = (NodeModel) obj;
		if (id != other.id)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [id=" + id + ", host=" + host + ", port=" + port
				+ ", debug=" + debug + ", weight=" + weight + ", adjustRatio="
				+ adjustRatio + ", latency=" + latency + ", enable=" + enable
				+ ", systemID=" + systemID + "]";
	}

}
