package cn.portal.esb.coproc.redis;

public class Command {

	public static enum OP {
		SET, ZADD, ZREMRANGEBYSCORE
	}

	private OP op;
	private String key;
	private Object[] args;

	public Command(OP op, String key, Object... args) {
		this.op = op;
		this.key = key;
		this.args = args;
	}

	public OP getOp() {
		return op;
	}

	public void setOp(OP op) {
		this.op = op;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Object arg(int i) {
		return args[i];
	}

}
