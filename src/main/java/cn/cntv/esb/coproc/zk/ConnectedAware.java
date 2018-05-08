package cn.portal.esb.coproc.zk;

public abstract class ConnectedAware {

	public static enum Type {
		DISCONNECTED, EXPIRED, SHUTDOWN
	}

	public void reconnect() {
		// do nothing
	}

	public void disconnect(Type type) {
		// do nothing
	}

}
