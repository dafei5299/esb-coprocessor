package cn.portal.esb.coproc.redis;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RedisInfo implements Serializable {

	private String host;
	private int port = 6379;
	private boolean available = true; // 当前是否可用
	private boolean messageQueue = true; // 是否是一个消息队列，即转发服务是否向这里投递消息
	private boolean replication = false; // 是否是同步代理，跨机房同步会用到

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

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public boolean isMessageQueue() {
		return messageQueue;
	}

	public void setMessageQueue(boolean messageQueue) {
		this.messageQueue = messageQueue;
	}

	public boolean isReplication() {
		return replication;
	}

	public void setReplication(boolean replication) {
		this.replication = replication;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		RedisInfo other = (RedisInfo) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

}
