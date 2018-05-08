package cn.portal.esb.coproc.util.sub;

public class Result<T> {
	
	private boolean ok;
	private T resultVal;
	private String msg;
	
	public Result(boolean ok, T resultVal) {
		super();
		this.ok = ok;
		this.resultVal = resultVal;
	}
	
	public Result(boolean ok, T resultVal, String msg) {
		this(ok,resultVal);
		this.msg = msg;
	}

	public boolean isOk() {
		return ok;
	}

	public T getResultVal() {
		return resultVal;
	}

	public String getMsg() {
		return msg;
	}
	
	@Override
	public String toString() {
		return "ok: "+ok+"; resultVal: "+resultVal+"; msg: "+msg;
	}

}
