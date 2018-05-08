package cn.portal.esb.coproc.thread;

import java.util.Timer;
import java.util.TimerTask;

/**
 * MySQLInsertDelayThread类功能：每隔几秒讲第三个redis队列中的数据转存到MySQL Author：liweichao
 */
public class MySQLInsertDelayThread implements Runnable {

	private ThreadTask threadTask;

	/**
	 * 构造函数重载
	 */
	public MySQLInsertDelayThread(ThreadTask threadTask) {
		this.threadTask = threadTask;
	}

	/**
	 * 重写接口的run()方法
	 */
	public void run() {
		Timer timer = new Timer("MySQLInserterThread");
		timer.schedule(new InsertDBThreadTask(), 0, 3000);
	}

	/**
	 * 定义一个内部类InsertDBThreadTask，规定线程MySQLInserter的具体任务
	 */
	class InsertDBThreadTask extends TimerTask {

		/**
		 * 重写基类的run()方法
		 */
		public void run() {
			MySQLInsertDelayThread.this.threadTask.InsertMysqlDBDelay();
		}
	}

}
