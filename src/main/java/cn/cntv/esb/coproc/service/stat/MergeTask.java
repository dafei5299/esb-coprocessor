package cn.portal.esb.coproc.service.stat;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.redis.RedisCluster;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class MergeTask extends PeriodTask {

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

	@PostConstruct
	public void init() {
		super.init(scheduler, zk);
		super.schedule("/control/merge/min", 60000, 30, "min", coprocessorID);
		super.schedule("/control/merge/hour", 3600000, 60, "hour",
				coprocessorID);
		super.schedule("/control/merge/day", 86400000, 60, "day", coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		String mark = (String) payload;
		for (SystemModel system : configData.systems()) {
			String key = "esb:" + system.getName();
			merge(key + ":s-freq", min, max, mark);
			merge(key + ":s-thrpt", min, max, mark);
			for (GroupModel group : system.getGroups()) {
				key = "esb:" + system.getName() + ":" + group.getName();
				merge(key + ":sg-freq", min, max, mark);
				merge(key + ":sg-thrpt", min, max, mark);
				for (ApiModel api : group.getApis()) {
					key = "esb:" + system.getName() + ":" + group.getName()
							+ ":" + api.getName() + ":" + api.getVersion();
					merge(key + ":sga-freq", min, max, mark);
					merge(key + ":sga-thrpt", min, max, mark);
				}
			}
			for (SourceModel source : configData.sources()) {
				key = "esb:" + source.getKey() + ":" + system.getName();
				merge(key + ":ks-freq", min, max, mark);
				merge(key + ":ks-thrpt", min, max, mark);
			}
		}
		log.debug("merge task finished, compact {} stat in redis", mark);
	}

	private void merge(String key, long min, long max, String mark) {
		long sum = 0;
		for (String value : redis.zremrange(key, min, max))
			sum += Long.parseLong(value.substring(0, value.indexOf(",")));
		if (sum > 0)
			redis.zadd(key, max, sum + "," + mark + "," + UUID.randomUUID());
	}

}
