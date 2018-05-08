package cn.portal.esb.coproc.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NgxNodeModel implements Serializable {

	private long id;
	private String host;
	private int port;
	private int weight; // 人工设置的权重
	private boolean enable; // 人工是否启用该节点
	private boolean available = true; // 自动是否判定该节点当前为可用
	private long datacenterID;

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

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public long getDatacenterID() {
		return datacenterID;
	}

	public void setDatacenterID(long datacenterID) {
		this.datacenterID = datacenterID;
	}

	public boolean deepEquals(NgxNodeModel other) {
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
		if (weight != other.weight)
			return false;
		if (enable != other.enable)
			return false;
		if (available != other.available)
			return false;
		if (datacenterID != other.datacenterID)
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
		NgxNodeModel other = (NgxNodeModel) obj;
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
		return "NgxNode [id=" + id + ", host=" + host + ", port=" + port
				+ ", weight=" + weight + ", enable=" + enable + ", available="
				+ available + ", datacenterID=" + datacenterID + "]";
	}

}
