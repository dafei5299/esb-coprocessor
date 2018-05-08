package cn.portal.esb.coproc.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.exceptions.JedisException;
import cn.portal.esb.coproc.redis.Command.OP;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ChildrenUpdater;
import cn.portal.esb.coproc.zk.ConnectedAware;
import cn.portal.esb.coproc.zk.NodeDataUpdater;
import cn.portal.esb.coproc.zk.ZkClient;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Repository
public class RedisCluster extends PeriodTask {

	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private ZkClient zk;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Value("#{dataCenterAware.coprocessorID()}")
	private long coprocessorID;
	@Value("${redis.monitor.period}")
	private int period;
	private ExecutorService executor;
	// 一致性哈希节点
	private NavigableMap<Long, RedisShard> nodes = new ConcurrentSkipListMap<>();
	private Map<String, RedisShard> shards = new ConcurrentHashMap<>();
	private CountDownLatch latch = new CountDownLatch(1);
	// 非哈希分布的全局消息队列
	private Map<String, List<BrpopTask>> messageQueues = new HashMap<>();
	// 消息队列任务
	private Map<String, BrpopCallback> brpops = new HashMap<>();
	// 各个机房的同步代理
	private Set<String> replDataCenter = new HashSet<>();
	private Map<String, RedisShard> replicas = new HashMap<>();
	private BlockingDeque<String> buffer = new LinkedBlockingDeque<>();

	@PostConstruct
	public void init() {
		executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
				.setNameFormat("brpop-pool-%d").build());
		zk.addAware(new ConnectedAware() {
			private boolean needBind = false;

			@Override
			public void disconnect(Type type) {
				log.warn("zk disconnect {}", type);
				needBind = type == Type.EXPIRED;
			}

			@Override
			public void reconnect() {
				log.warn("zk reconnect, need register watchers? {}", needBind);
				if (needBind)
					watch();
			}
		});
		watch();
		new ReplicateExporter(); // 启动向其他机房复制写操作命令的线程
		super.init(scheduler, zk);
		super.schedule("/control/redis/" + dataCenterID, period, 5, null,
				coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
		executor.shutdown();
		for (RedisShard shard : shards.values()) {
			delMessageQueue(shard.toString());
			shard.destroy();
		}
	}

	private void watch() {
		zk.ensurePresence("/redis").watchChildren("/redis",
				new ReplicaDataCenterUpdater());
		zk.ensurePresence("/redis/" + dataCenterID).watchChildren(
				"/redis/" + dataCenterID, new RedisChildrenUpdater());
	}

	public List<RedisInfo> shards() {
		List<RedisInfo> list = new ArrayList<>();
		for (RedisShard shard : shards.values())
			list.add(MapperUtils.copy(shard.getInfo()));
		return list;
	}

	public Map<String, RedisInfo> replicas() {
		Map<String, RedisInfo> map = new HashMap<>();
		for (String dc : replicas.keySet())
			map.put(dc, MapperUtils.copy(replicas.get(dc).getInfo()));
		return map;
	}

	// 注册事件，查看是否有新的机房需要同步
	private class ReplicaDataCenterUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<String> curr = ImmutableSet.copyOf(children);
			// 新的机房集合与已知机房集合，比差集，得出新增的
			for (final String dc : Sets.difference(curr, replDataCenter)) {
				zk.watchChildren("/redis/" + dc, new ChildrenUpdater() {
					@Override
					public void onChange(List<String> children) {
						String z = "/redis/" + dc + "/";
						for (String e : children)
							zk.watchNodeData(z + e, new ReplicaNodeUpdater(dc));
					}
				});
				// 只要不是本机房，就开启一个复制线程，负责从该机房的同步代理消息队列中提取消息，复制到本机房
				if (!String.valueOf(dataCenterID).equals(dc)) {
					replDataCenter.add(dc);
					new ReplicateImporter(dc);
				}
			}
		}
	}

	// 注册事件，观察其他机房的redis配置项
	private class ReplicaNodeUpdater implements NodeDataUpdater {
		final String dc;

		ReplicaNodeUpdater(String dc) {
			this.dc = dc;
		}

		@Override
		public void onChange(String nodeData) {
			RedisInfo info = MapperUtils.fromJson(nodeData, RedisInfo.class);
			RedisShard prev = replicas.get(dc);
			if (info.isReplication()) {
				// 如果是同步代理节点，则使用该节点来复制本机房的写操作
				if (prev == null) {
					replicas.put(dc, new RedisShard(info));
				} else if (!prev.getInfo().equals(info)) {
					replicas.put(dc, new RedisShard(info));
					prev.destroy();
				}
			} else if (prev != null && prev.getInfo().equals(info)) {
				// 如果当前的同步代理节点修改为非同步节点了，则删除该机房的复制
				replicas.remove(dc);
				prev.destroy();
			}
		}
	}

	// 注册事件，观察本机房的redis节点增删
	private class RedisChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<String> prev = shards.keySet(), curr = ImmutableSet
					.copyOf(children);
			// 看看哪些是原先有，现在没了的
			for (String endpoint : Sets.difference(prev, curr))
				delShard(endpoint, true);
			// 看看哪些是原先没有，现在多出来的
			for (String endpoint : Sets.difference(curr, prev))
				zk.watchNodeData("/redis/" + dataCenterID + "/" + endpoint,
						new RedisDataUpdater());
		}
	}

	private class RedisDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			RedisInfo info = MapperUtils.fromJson(nodeData, RedisInfo.class);
			String endpoint = info.getHost() + ":" + info.getPort();
			// 根据可用性来增删分片
			if (info.isAvailable())
				addShard(new RedisShard(info));
			else
				delShard(endpoint, false);
		}
	}

	protected void addShard(RedisShard shard) {
		if (!shard.getInfo().isMessageQueue())
			delMessageQueue(shard.toString());
		for (int i = 0; i < 256; i++)
			nodes.put(md5(shard + "#" + i), shard);
		RedisShard old = shards.put(shard.toString(), shard);
		if (old != null)
			old.destroy();
		latch.countDown();
		if (shard.getInfo().isMessageQueue())
			addMessageQueue(shard.toString());
		log.info("redis cluster add shard {}", shard);
	}

	protected void delShard(String endpoint, boolean drop) {
		delMessageQueue(endpoint);
		for (int i = 0; i < 256; i++)
			nodes.remove(md5(endpoint + "#" + i));
		if (drop)
			shards.remove(endpoint).destroy();
		if (nodes.size() == 0)
			latch = new CountDownLatch(1);
		log.info("redis cluster del shard {}", endpoint);
	}

	protected synchronized void addMessageQueue(String endpoint) {
		if (!messageQueues.containsKey(endpoint))
			messageQueues.put(endpoint, new ArrayList<BrpopTask>());
		List<BrpopTask> tasks = messageQueues.get(endpoint);
		RedisShard shard = shards.get(endpoint);
		// 每多一个消息队列，就看有几个brpop任务，然后启这么多个线程执行brpop任务
		for (String key : brpops.keySet()) {
			BrpopTask task = new BrpopTask(shard, key, brpops.get(key));
			executor.execute(task);
			tasks.add(task);
		}
	}

	protected synchronized void delMessageQueue(String endpoint) {
		// 删掉消息队列，得把对应的线程池任务关闭掉
		if (messageQueues.containsKey(endpoint))
			for (BrpopTask task : messageQueues.remove(endpoint))
				task.shutdown = true;
	}

	protected RedisShard getShard(String key) {
		try {
			latch.await();
		} catch (InterruptedException _) {
			// ignore
		}
		// 一致性哈希
		SortedMap<Long, RedisShard> tail = nodes.tailMap(murmur(key));
		if (tail.size() == 0)
			return nodes.get(nodes.firstKey());
		return tail.get(tail.firstKey());
	}

	private long md5(String input) {
		return Hashing.md5().hashString(input).padToLong();
	}

	private long murmur(String input) {
		return Hashing.murmur3_128().hashString(input).padToLong();
	}

	// 周期性检查Redis的健康情况，摘除故障节点，重新加入恢复节点
	@Override
	protected void work(long min, long max, Object payload) {
		if (System.currentTimeMillis() - max > 3 * period)
			return;
		for (RedisShard shard : shards.values()) {
			try {
				shard.ping();
			} catch (JedisException _) {
				try {
					Thread.sleep(100);
					shard.ping();
				} catch (JedisException | InterruptedException e) {
					if (shard.getInfo().isAvailable()) {
						// 一个健康的节点，ping两次都不通，摘除
						log.warn("monitor redis {}, status abnormal: {}",
								shard, e.getMessage());
						shard.getInfo().setAvailable(false);
						String zkPath = "/redis/" + dataCenterID + "/" + shard;
						zk.save(zkPath, MapperUtils.toJson(shard.getInfo()));
					}
					continue;
				}
			}
			if (!shard.getInfo().isAvailable()) {
				// 一个已经摘除的节点，能ping通，给它恢复
				log.warn("monitor redis {}, status recover", shard);
				shard.getInfo().setAvailable(true);
				String zkPath = "/redis/" + dataCenterID + "/" + shard;
				zk.save(zkPath, MapperUtils.toJson(shard.getInfo()));
			}
		}
	}

	private class BrpopTask implements Runnable {
		volatile boolean shutdown = false;
		final RedisShard redis;
		final String key;
		final BrpopCallback callback;

		BrpopTask(RedisShard redis, String key, BrpopCallback callback) {
			this.redis = redis;
			this.key = key;
			this.callback = callback;
		}

		@Override
		public void run() {
			while (!shutdown) {
				try {
					String data = redis.brpop(key);
					if (data != null)
						callback.handle(data);
				} catch (Exception e) {
					log.warn("brpop task found error", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException _) {
						// ignore
					}
				}
			}
		}
	}

	// 同步代理，本机房操作向其他机房复制
	private class ReplicateExporter extends Thread {

		ReplicateExporter() {
			super("redis-repl-exp");
			setDaemon(false);
			start();
		}

		@Override
		public void run() {
			while (true) {
				try {
					RedisShard replica = replicas.get(String
							.valueOf(dataCenterID));
					if (replica == null) {
						Thread.sleep(1000);
						continue;
					}
					String command = buffer.takeFirst();
					try {
						for (String dc : replDataCenter)
							replica.lpush("portal_esb:repl_queue:" + dc, command);
					} catch (JedisException e) {
						log.warn("redis repl, local queue error: {}",
								e.getMessage());
						buffer.offerFirst(command);
						Thread.sleep(1000);
					}
				} catch (Exception e) {
					log.warn("redis repl export found error", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException _) {
						// ignore
					}
				}
			}
		}
	}

	// 同步代理，从其他机房队列中提取消息，复制到本机房
	private class ReplicateImporter extends Thread {

		final String dc;

		ReplicateImporter(String dc) {
			super("redis-repl-imp");
			setDaemon(false);
			this.dc = dc;
			start();
		}

		@Override
		public void run() {
			while (true) {
				try {
					RedisShard replica = replicas.get(dc);
					if (replica == null) {
						Thread.sleep(1000);
						continue;
					}
					String json = replica.brpop("portal_esb:repl_queue:" + dc);
					Command cmd = MapperUtils.fromJson(json, Command.class);
					if (cmd == null)
						continue;
					try {
						switch (cmd.getOp()) {
						case SET:
							getShard(cmd.getKey()).set(cmd.getKey(),
									(String) cmd.arg(0));
							break;
						case ZADD:
							getShard(cmd.getKey()).zadd(cmd.getKey(),
									(long) cmd.arg(0), (String) cmd.arg(1));
							break;
						case ZREMRANGEBYSCORE:
							getShard(cmd.getKey()).zremrangebyscore(
									cmd.getKey(), (long) cmd.arg(0),
									(long) cmd.arg(1));
							break;
						}
					} catch (JedisException e) {
						log.warn("redis repl, local redis error: {}",
								e.getMessage());
						replica.rpush("portal_esb:repl_queue:" + dc, json);
						Thread.sleep(1000);
					}
				} catch (Exception e) {
					log.warn("redis repl import found error", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException _) {
						// ignore
					}
				}
			}
		}

	}

	// Redis操作命令

	public synchronized void brpop(String key, BrpopCallback callback) {
		// 注册一个brpop任务回调
		for (String endpoint : messageQueues.keySet()) {
			BrpopTask task = new BrpopTask(shards.get(endpoint), key, callback);
			executor.execute(task);
			messageQueues.get(endpoint).add(task);
		}
		brpops.put(key, callback);
	}

	public void zadd(String key, long score, String value)
			throws JedisException {
		getShard(key).zadd(key, score, value);
		Command command = new Command(OP.ZADD, key, score, value);
		buffer.offerLast(MapperUtils.toJson(command));
	}

	public Set<String> zrange(String key, long min, long max)
			throws JedisException {
		return getShard(key).zrange(key, min, max);
	}

	public Set<String> zremrange(String key, long min, long max)
			throws JedisException {
		Set<String> result = getShard(key).zremrange(key, min, max);
		if (!result.isEmpty()) {
			Command command = new Command(OP.ZREMRANGEBYSCORE, key, min, max);
			buffer.offerLast(MapperUtils.toJson(command));
		}
		return result;
	}

	public String get(String key) throws JedisException {
		return getShard(key).get(key);
	}

	public void set(String key, String value) throws JedisException {
		getShard(key).set(key, value);
		Command command = new Command(OP.SET, key, value);
		buffer.offerLast(MapperUtils.toJson(command));
	}

}
