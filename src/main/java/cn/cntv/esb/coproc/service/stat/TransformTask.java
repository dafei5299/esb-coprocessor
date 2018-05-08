package cn.portal.esb.coproc.service.stat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.redis.RedisCluster;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.ConfigService;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.util.DefaultMap;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class TransformTask extends PeriodTask {

	private static final String FREQUENCY_KEY_ITEM = "freq";
	private static final String THROUGHPUT_KEY_ITEM = "thrpt";
	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private ZkClient zk;
	@Value("#{dataCenterAware.coprocessorID()}")
	private long coprocessorID;
	@Autowired
	private RedisCluster redis;
	@Autowired
	private ConfigData configData;
	@Autowired
	private ConfigService configService;

	@PostConstruct
	public void init() {
		super.init(scheduler, zk);
		super.schedule("/control/transform", 500, 10, null, coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		// 统计运算
		// 从 esb:<system>:timeline 中提取数据 （Sorted Set结构，时间戳为score）
		// 以 system 为驱动，分两个维度进行汇总
		// 一个维度是 group-api
		// 一个维度是 source
		// 输出，都是Sorted Set
		// esb:<system>:s-freq
		// esb:<system>:s-thrpt
		// esb:<system>:<group>:sg-freq
		// esb:<system>:<group>:sg-thrpt
		// esb:<system>:<group>:<api>:<version>:sga-freq
		// esb:<system>:<group>:<api>:<version>:sga-thrpt
		// esb:<source>:<system>:ks-freq
		// esb:<source>:<system>:ks-thrpt
		// 历史平均响应时长，是List
		// esb:<system>:s-proc
		// esb:<system>:<group>:sg-proc
		// esb:<system>:<group>:<api>:<version>:sga-proc
		for (SystemModel system : configData.systems())
			statistics(system.getName(), max);
		// 检查是否超过流量限制
		// 分别遍历系统、分组、接口的响应统计数据zset
		// 根据管理后台设定的区间范围，计算汇总数据是否超过限制
		// 如果状态发生变化，调用configService的接口，写入ZooKeeper
		for (SystemModel system : configData.systems()) {
			system(system, max);
			for (GroupModel group : system.getGroups()) {
				group(group, max);
				for (ApiModel api : group.getApis()) {
					api(api, max);
				}
			}
		}
	}

	private void statistics(String system, long max) {
		Map<String, Integer> freqFrom = DefaultMap.si(); // 频度 - 来源维度
		Map<String, Integer> thrptFrom = DefaultMap.si(); // 流量 - 来源维度
		Map<String, Map<String, Map<Integer, Integer>>> freqTo = DefaultMap
				.ssii(); // 频度 - 目标维度
		Map<String, Map<String, Map<Integer, Integer>>> thrptTo = DefaultMap
				.ssii(); // 流量 - 目标维度
		Map<String, Map<String, Map<Integer, Integer>>> procTime = DefaultMap
				.ssii(); // 响应时间 - 目标维度
		for (String json : redis.zremrange("esb:" + system + ":timeline", 0,
				max)) {
			PreparedData pd = MapperUtils.fromJson(json, PreparedData.class);
			freqFrom.put(pd.getSource(), freqFrom.get(pd.getSource()) + 1); // freqFrom[pd.getSource()]++
			thrptFrom.put(pd.getSource(),
					thrptFrom.get(pd.getSource()) + pd.getContentLength()); // thrptFrom[pd.getSource()]+=contentLength
			Map<Integer, Integer> freq = freqTo.get(pd.getGroup()).get(
					pd.getApi());
			freq.put(pd.getVersion(), freq.get(pd.getVersion()) + 1);
			Map<Integer, Integer> thrpt = thrptTo.get(pd.getGroup()).get(
					pd.getApi());
			thrpt.put(pd.getVersion(),
					thrpt.get(pd.getVersion()) + pd.getContentLength());
			Map<Integer, Integer> proc = procTime.get(pd.getGroup()).get(
					pd.getApi());
			proc.put(pd.getVersion(),
					proc.get(pd.getVersion()) + pd.getProcessTime());
		}
		sumGroupByFrom(freqFrom, system, max, FREQUENCY_KEY_ITEM);
		sumGroupByFrom(thrptFrom, system, max, THROUGHPUT_KEY_ITEM);
		sumGroupByTo(freqTo, system, max, FREQUENCY_KEY_ITEM);
		sumGroupByTo(thrptTo, system, max, THROUGHPUT_KEY_ITEM);
		avgGroupBy(procTime, freqTo, system);
	}

	private void sumGroupByFrom(Map<String, Integer> data, String system,
			long timestamp, String item) {
		int sum = 0;
		for (String source : data.keySet()) {
			int value = data.get(source);
			redis.zadd("esb:" + source + ":" + system + ":ks-" + item,
					timestamp, value + "," + UUID.randomUUID());
			sum += value;
		}
		if (sum > 0) {
			redis.zadd("esb:" + system + ":s-" + item, timestamp, sum + ","
					+ UUID.randomUUID());
		}
	}

	private void sumGroupByTo(
			Map<String, Map<String, Map<Integer, Integer>>> data,
			String system, long timestamp, String item) {
		for (String group : data.keySet()) {
			int sum = 0;
			for (String api : data.get(group).keySet()) {
				for (int version : data.get(group).get(api).keySet()) {
					int value = data.get(group).get(api).get(version);
					redis.zadd("esb:" + system + ":" + group + ":" + api + ":"
							+ version + ":sga-" + item, timestamp, value + ","
							+ UUID.randomUUID());
					sum += value;
				}
			}
			redis.zadd("esb:" + system + ":" + group + ":sg-" + item,
					timestamp, sum + "," + UUID.randomUUID());
		}
	}

	private void avgGroupBy(
			Map<String, Map<String, Map<Integer, Integer>>> data,
			Map<String, Map<String, Map<Integer, Integer>>> count, String system) {
		int ss = 0, cs = 0;
		for (String group : data.keySet()) {
			int sg = 0, cg = 0;
			for (String api : data.get(group).keySet()) {
				for (int version : data.get(group).get(api).keySet()) {
					int sa = data.get(group).get(api).get(version);
					int ca = count.get(group).get(api).get(version);
					api(system, group, api, version, sa / ca);
					sg += sa;
					cg += ca;
				}
			}
			group(system, group, sg / cg);
			ss += sg;
			cs += cg;
		}
		if (cs > 0) {
			system(system, ss / cs);
		}
	}

	// 检查是否过载

	private String isOverload(String key, int avg, int factor) {
		String value = redis.get(key); // 取历史平均响应时长
		double newAvg = avg;
		if (value != null) {
			int idx = value.indexOf(",");
			double oldAvg = Double.parseDouble(value.substring(0, idx));
			if (oldAvg > 0) {
				if (factor <= 0) {
					newAvg = oldAvg * 0.95 + avg * 0.05;
				} else {
					int health = Integer.parseInt(value.substring(idx + 1)); // 当前健康度
					if (avg / oldAvg > factor) { // 本次响应时间对比历史响应时间，超过系数
						newAvg = oldAvg * 0.9995 + avg * 0.0005;
						health++;
					} else {
						newAvg = oldAvg * 0.95 + avg * 0.05;
						health--;
					}
					int maxHeal = 50 / factor;
					maxHeal = maxHeal < 1 ? 1 : maxHeal > 10 ? 10 : maxHeal;
					health = health < 0 ? 0
							: health > maxHeal * 3 ? maxHeal * 3 : health;
					redis.set(key, String.format("%.2f,%d", newAvg, health));
					// 健康度大于一定阈值后，触发降级
					return String.format("%s, history avg latency is %.2f, "
							+ "current avg latency is %d, "
							+ "health value is %d, threshold is %d",
							health >= maxHeal * 2 ? "overload" : "health",
							oldAvg, avg, health, maxHeal * 2);
				}
			}
		}
		redis.set(key, String.format("%.2f,0", newAvg));
		return String.format("health, current avg latency is %d", avg);
	}

	private void api(String s, String g, String a, int v, int avg) {
		ApiModel api = configData.api(s, g, a, v);
		String key = "esb:" + s + ":" + g + ":" + a + ":" + v + ":sga-proc";
		String reason = isOverload(key, avg, api.getDeferredTriggerFactor());
		boolean changed = false;
		if (!reason.equals(api.getDeferredReason())) {
			api.setDeferredReason(reason);
			changed = true;
		}
		boolean overload = reason.startsWith("overload");
		if (overload != api.isShouldDeferred()) {
			log.info("api {} overload control update, "
					+ "avg process time: {} ({})", a, avg,
					overload ? "overload" : "normal");
			api.setShouldDeferred(overload);
			changed = true;
		}
		if (changed)
			configService.api(api);
	}

	private void group(String s, String g, int avg) {
		GroupModel group = configData.group(s, g);
		String key = "esb:" + s + ":" + g + ":sg-proc";
		String reason = isOverload(key, avg, group.getDeferredTriggerFactor());
		boolean changed = false;
		if (!reason.equals(group.getDeferredReason())) {
			group.setDeferredReason(reason);
			changed = true;
		}
		boolean overload = reason.startsWith("overload");
		if (overload != group.isShouldDeferred()) {
			log.info("group {} overload control update, "
					+ "avg process time: {} ({})", g, avg,
					overload ? "overload" : "normal");
			group.setShouldDeferred(overload);
			changed = true;
		}
		if (changed)
			configService.group(group);
	}

	private void system(String s, int avg) {
		SystemModel system = configData.system(s);
		String key = "esb:" + s + ":s-proc";
		String reason = isOverload(key, avg, system.getDeferredTriggerFactor());
		boolean changed = false;
		if (!reason.equals(system.getDeferredReason())) {
			system.setDeferredReason(reason);
			changed = true;
		}
		boolean overload = reason.startsWith("overload");
		if (overload != system.isShouldDeferred()) {
			log.info("system {} overload control update, "
					+ "avg process time: {} ({})", s, avg,
					overload ? "overload" : "normal");
			system.setShouldDeferred(overload);
			changed = true;
		}
		if (changed)
			configService.system(system);
	}

	// 检查是否超过流量限制

	private void system(SystemModel system, long now) {
		boolean freqExceeded = false, thrptExceeded = false, connExceeded = false, changed = false;
		int currConn = 0, freqPeriod = system.getFrequencyPeriod(), thrptPeriod = system
				.getThroughputPeriod(), connLimit = system.getConnectionLimit();
		long currFreq = 0, currThrpt = 0, freqLimit = system
				.getFrequencyLimit(), thrptLimit = system.getThroughputLimit();
		if (freqLimit > 0 && freqPeriod > 0) {
			currFreq = sum(redis.zrange("esb:" + system.getName() + ":s-"
					+ FREQUENCY_KEY_ITEM, now - freqPeriod * 1000, now));
			if (currFreq != system.getFrequencyCurrent()) {
				system.setFrequencyCurrent(currFreq);
				changed = true;
			}
			if (currFreq >= freqLimit)
				freqExceeded = true;
		}
		if (thrptLimit > 0 && thrptPeriod > 0) {
			currThrpt = sum(redis.zrange("esb:" + system.getName() + ":s-"
					+ THROUGHPUT_KEY_ITEM, now - thrptPeriod * 1000, now));
			if (currThrpt != system.getThroughputCurrent()) {
				system.setThroughputCurrent(currThrpt);
				changed = true;
			}
			if (currThrpt >= thrptLimit)
				thrptExceeded = true;
		}
		if (connLimit > 0) {
			currConn = (int) sum(redis.zrange("esb:" + system.getName() + ":s-"
					+ FREQUENCY_KEY_ITEM, now - 1000, now));
			if (currConn != system.getConnectionCurrent()) {
				system.setConnectionCurrent(currConn);
				changed = true;
			}
			if (currConn >= connLimit)
				connExceeded = true;
		}
		if (freqExceeded != system.isFrequencyExceeded()
				|| thrptExceeded != system.isThroughputExceeded()
				|| connExceeded != system.isConnectionExceeded()) {
			log.info("system {} exceed control update, freq: {}->{}/{} ({}), "
					+ "thrpt: {}->{}/{} ({}), conn: {}->{} ({})", system
					.getName(), freqLimit, currFreq, freqPeriod,
					freqExceeded ? "exceeded" : "normal", thrptLimit,
					currThrpt, thrptPeriod, thrptExceeded ? "exceeded"
							: "normal", connLimit, currConn,
					connExceeded ? "exceeded" : "normal");
			system.setFrequencyExceeded(freqExceeded);
			system.setThroughputExceeded(thrptExceeded);
			system.setConnectionExceeded(connExceeded);
			changed = true;
		}
		if (changed)
			configService.system(system);
	}

	private void group(GroupModel group, long now) {
		boolean freqExceeded = false, thrptExceeded = false, connExceeded = false, changed = false;
		int currConn = 0, freqPeriod = group.getFrequencyPeriod(), thrptPeriod = group
				.getThroughputPeriod(), connLimit = group.getConnectionLimit();
		long currFreq = 0, currThrpt = 0, freqLimit = group.getFrequencyLimit(), thrptLimit = group
				.getThroughputLimit();
		if (freqLimit > 0 && freqPeriod > 0) {
			currFreq = sum(redis.zrange("esb:" + group.getSystem().getName()
					+ ":" + group.getName() + ":sg-" + FREQUENCY_KEY_ITEM, now
					- freqPeriod * 1000, now));
			if (currFreq != group.getFrequencyCurrent()) {
				group.setFrequencyCurrent(currFreq);
				changed = true;
			}
			if (currFreq >= freqLimit)
				freqExceeded = true;
		}
		if (thrptLimit > 0 && thrptPeriod > 0) {
			currThrpt = sum(redis.zrange("esb:" + group.getSystem().getName()
					+ ":" + group.getName() + ":sg-" + THROUGHPUT_KEY_ITEM, now
					- thrptPeriod * 1000, now));
			if (currThrpt != group.getThroughputCurrent()) {
				group.setThroughputCurrent(currThrpt);
				changed = true;
			}
			if (currThrpt >= thrptLimit)
				thrptExceeded = true;
		}
		if (connLimit > 0) {
			currConn = (int) sum(redis.zrange("esb:"
					+ group.getSystem().getName() + ":" + group.getName()
					+ ":sg-" + FREQUENCY_KEY_ITEM, now - 1000, now));
			if (currConn != group.getConnectionCurrent()) {
				group.setConnectionCurrent(currConn);
				changed = true;
			}
			if (currConn >= connLimit)
				connExceeded = true;
		}
		if (freqExceeded != group.isFrequencyExceeded()
				|| thrptExceeded != group.isThroughputExceeded()
				|| connExceeded != group.isConnectionExceeded()) {
			log.info("group {} exceed control update, freq: {}->{}/{} ({}), "
					+ "thrpt: {}->{}/{} ({}), conn: {}->{} ({})", group
					.getName(), freqLimit, currFreq, freqPeriod,
					freqExceeded ? "exceeded" : "normal", thrptLimit,
					currThrpt, thrptPeriod, thrptExceeded ? "exceeded"
							: "normal", connLimit, currConn,
					connExceeded ? "exceeded" : "normal");
			group.setFrequencyExceeded(freqExceeded);
			group.setThroughputExceeded(thrptExceeded);
			group.setConnectionExceeded(connExceeded);
			changed = true;
		}
		if (changed)
			configService.group(group);

	}

	private void api(ApiModel api, long now) {
		boolean freqExceeded = false, thrptExceeded = false, connExceeded = false, changed = false;
		int currConn = 0, freqPeriod = api.getFrequencyPeriod(), thrptPeriod = api
				.getThroughputPeriod(), connLimit = api.getConnectionLimit();
		long currFreq = 0, currThrpt = 0, freqLimit = api.getFrequencyLimit(), thrptLimit = api
				.getThroughputLimit();
		if (freqLimit > 0 && freqPeriod > 0) {
			currFreq = sum(redis.zrange("esb:"
					+ api.getGroup().getSystem().getName() + ":"
					+ api.getGroup().getName() + ":" + api.getName() + ":"
					+ api.getVersion() + ":sga-" + FREQUENCY_KEY_ITEM, now
					- freqPeriod * 1000, now));
			if (currFreq != api.getFrequencyCurrent()) {
				api.setFrequencyCurrent(currFreq);
				changed = true;
			}
			if (currFreq >= freqLimit)
				freqExceeded = true;
		}
		if (thrptLimit > 0 && thrptPeriod > 0) {
			currThrpt = sum(redis.zrange("esb:"
					+ api.getGroup().getSystem().getName() + ":"
					+ api.getGroup().getName() + ":" + api.getName() + ":"
					+ api.getVersion() + ":sga-" + THROUGHPUT_KEY_ITEM, now
					- thrptPeriod * 1000, now));
			if (currThrpt != api.getThroughputCurrent()) {
				api.setThroughputCurrent(currThrpt);
				changed = true;
			}
			if (currThrpt >= thrptLimit)
				thrptExceeded = true;
		}
		if (connLimit > 0) {
			currConn = (int) sum(redis.zrange("esb:"
					+ api.getGroup().getSystem().getName() + ":"
					+ api.getGroup().getName() + ":" + api.getName() + ":"
					+ api.getVersion() + ":sga-" + FREQUENCY_KEY_ITEM,
					now - 1000, now));
			if (currConn != api.getConnectionCurrent()) {
				api.setConnectionCurrent(currConn);
				changed = true;
			}
			if (currConn >= connLimit)
				connExceeded = true;
		}
		if (freqExceeded != api.isFrequencyExceeded()
				|| thrptExceeded != api.isThroughputExceeded()
				|| connExceeded != api.isConnectionExceeded()) {
			log.info("api {} exceed control update, freq: {}->{}/{} ({}), "
					+ "thrpt: {}->{}/{} ({}), conn: {}->{} ({})",
					api.getName(), freqLimit, currFreq, freqPeriod,
					freqExceeded ? "exceeded" : "normal", thrptLimit,
					currThrpt, thrptPeriod, thrptExceeded ? "exceeded"
							: "normal", connLimit, currConn,
					connExceeded ? "exceeded" : "normal");
			api.setFrequencyExceeded(freqExceeded);
			api.setThroughputExceeded(thrptExceeded);
			api.setConnectionExceeded(connExceeded);
			changed = true;
		}
		if (changed)
			configService.api(api);

	}

	private long sum(Set<String> values) {
		int sum = 0;
		for (String value : values)
			sum += Long.parseLong(value.substring(0, value.indexOf(",")));
		return sum;
	}

}
