package cn.portal.esb.coproc.zk;

public class ZkTicket implements Comparable<ZkTicket> {

	private String node;
	private String data;
	private int serial;
	private long owner;

	public ZkTicket(String node, String data, int serial, long owner) {
		this.node = node;
		this.data = data;
		this.serial = serial;
		this.owner = owner;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getSerial() {
		return serial;
	}

	public void setSerial(int serial) {
		this.serial = serial;
	}

	public long getOwner() {
		return owner;
	}

	public void setOwner(long owner) {
		this.owner = owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
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
		ZkTicket other = (ZkTicket) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}

	@Override
	public int compareTo(ZkTicket o) {
		return (serial < o.serial) ? -1 : ((serial == o.serial) ? 0 : 1);
	}

}
