package cn.portal.esb.coproc.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import cn.portal.esb.coproc.zk.ZkClient;

public abstract class PeriodTask {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private volatile boolean shutdown = false;
	private List<Runner> runners = new ArrayList<>();
	private TaskScheduler scheduler;
	private ZkClient zk;

	protected void init(TaskScheduler scheduler, ZkClient zk) {
		this.scheduler = scheduler;
		this.zk = zk;
	}

	protected void shutdown() {
		shutdown = true;
		for (Runner runner : runners)
			runner.stop();
	}

	// 设定定时任务
	protected void schedule(String zkPath, int period, int initDelay,
			Object payload, long coprocessorID) {
		zk.ensurePresence(zkPath.substring(0, zkPath.lastIndexOf("/")));
		long max;
		Date init;
		// 先看看有没有上次停止时，由于没来得及执行，但进程需要退出而留下的licence
		String recovery = zk.data(zkPath + "/" + coprocessorID);
		if (recovery != null) {
			zk.remove(zkPath + "/" + coprocessorID);
			max = Long.parseLong(recovery);
			init = new Date(max + period);
		} else {
			max = 0;
			init = new DateTime().plusSeconds(initDelay).toDate();
		}
		Runner runner = new Runner(zkPath, period, payload, coprocessorID, max);
		runners.add(runner);
		scheduler.schedule(runner, init);
	}

	private class Runner implements Runnable {

		volatile long max;
		final String zkPath;
		final int period;
		final Object payload;
		final long coprocessorID;

		Runner(String zkPath, int period, Object payload, long coprocessorID,
				long max) {
			this.zkPath = zkPath;
			this.period = period;
			this.payload = payload;
			this.coprocessorID = coprocessorID;
			this.max = max;
		}

		@Override
		public void run() {
			if (shutdown)
				return;
			try {
				if (max > 0)
					work(max - period, max, payload); // 执行
			} catch (Exception e) {
				log.warn("period task found error", e);
			} finally {
				scheduler.schedule(this, next()); // 领下一次执行的licence，并设定预定时间
			}
		}

		Date next() {
			long init = System.currentTimeMillis() / period * period - period;
			try {
				max = zk.licence(zkPath, init, period);
				return new Date(max + period);
			} catch (Exception e) {
				log.warn("period task get next licence error {}",
						e.getMessage());
				max = 0;
				return new Date(init + 2 * period);
			}
		}

		void stop() {
			// 退出前留存没来得及执行的licence
			if (max > 0)
				zk.save(zkPath + "/" + coprocessorID, String.valueOf(max));
		}

	}

	abstract protected void work(long min, long max, Object payload);

}
