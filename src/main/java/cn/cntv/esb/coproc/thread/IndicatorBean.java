package cn.portal.esb.coproc.thread;

/**
 * IndicatorBean类功能：统计过程中bean，封装了次数和宽带 Author：liweichao
 */
public class IndicatorBean {

	/** 访问次数 */
	private int count;
	/** 访问流量 */
	private long bandwidth;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

}
