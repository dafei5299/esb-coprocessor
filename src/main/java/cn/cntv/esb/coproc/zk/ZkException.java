package cn.portal.esb.coproc.zk;

@SuppressWarnings("serial")
public class ZkException extends RuntimeException {

	public ZkException() {
		super();
	}

	public ZkException(String message) {
		super(message);
	}

	public ZkException(String message, Throwable cause) {
		super(message, cause);
	}

	public ZkException(Throwable cause) {
		super(cause);
	}

}
