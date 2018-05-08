package cn.portal.esb.coproc.thread;

import java.util.Timer;
import java.util.TimerTask;

/**
 * StatisticBySecondThread类功能：第二个线程 Author：liweichao
 */
public class StatisticBySecondThread implements Runnable {

	private ThreadTask threadTask;

	/**
	 * 构造函数重载
	 */
	public StatisticBySecondThread(ThreadTask threadTask) {
		this.threadTask = threadTask;
	}

	/**
	 * 重写Runnable接口的run()方法
	 */
	public void run() {
		Timer timer = new Timer("StatisticBySecondThread");
		timer.schedule(new BRPOPThreadTask(), 0, 1000);
	}

	/**
	 * 定义一个内部类BRPOPThreadTask，规定线程RedisBRPOPer的具体任务
	 */
	class BRPOPThreadTask extends TimerTask {

		/**
		 * 重写基类的run()方法
		 */
		public void run() {
			StatisticBySecondThread.this.threadTask.BRPOPAndStatistics();
		}
	}

}
