package cn.portal.esb.coproc.ngx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class NgxCacheCluster implements NgxCache, Runnable {

	private int timeout;
	private int retryDelay;
	private int maxTryTimes;
	private ScheduledExecutorService executor;
	private List<NgxCacheNode> nodes = new ArrayList<>();
	private Map<String, String> buffer = new ConcurrentHashMap<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public NgxCacheCluster(String endpoints, int timeout, int retryDelay,
			int maxTryTimes, int flushPeriod) {
		this.timeout = timeout;
		this.retryDelay = retryDelay;
		this.maxTryTimes = maxTryTimes;
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime()
				.availableProcessors(), new ThreadFactoryBuilder()
				.setNameFormat("ngx-pool-%d").build());
		for (String endpoint : Splitter.on(",").trimResults()
				.omitEmptyStrings().split(endpoints)) {
			nodes.add(new NgxCacheNode(endpoint, timeout, retryDelay,
					maxTryTimes, executor));
		}
		executor.scheduleAtFixedRate(this, flushPeriod, flushPeriod,
				TimeUnit.MILLISECONDS); // 每隔一段时间触发一次批量推送
	}

	@Override
	public void close() throws IOException {
		try {
			for (NgxCache node : nodes)
				node.close();
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException _) {
			// ignore
		}
	}

	public void add(String endpoint) {
		nodes.add(new NgxCacheNode(endpoint, timeout, retryDelay, maxTryTimes,
				executor));
	}

	public void remove(String endpoint) {
		for (NgxCacheNode node : nodes) {
			if (node.endpoint().equals(endpoint)) {
				nodes.remove(node);
				break;
			}
		}
	}

	@Override
	public String get(String key) {
		return nodes.get(0).get(key);
	}

	@Override
	public void set(String key, String value) {
		lock.readLock().lock(); // 共享锁
		try {
			// 不是每次调用set都会发送，先放buffer里，每隔一段时间批量发送一次
			// 使用Map来做buffer的目的是，对于同一个key的连续多个value的写操作，合并，最后一次操作会生效
			// 避免部分频繁读写的key对转发服务造成冲击
			buffer.put(key, value);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void del(String key) {
		lock.writeLock().lock(); // 互斥锁
		try {
			// 删除之前，先手动触发一次推送
			// 避免有些操作是先set，再del,那就会在某些情况下，会发生明明最后是删掉了，但还保持上次set的结果
			run();
			for (NgxCache node : nodes)
				node.del(key);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void run() {
		if (buffer.isEmpty())
			return;
		lock.writeLock().lock(); // 互斥锁
		try {
			for (String key : buffer.keySet()) {
				String value = buffer.get(key);
				for (NgxCache node : nodes)
					node.set(key, value);
			}
			buffer.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

}
