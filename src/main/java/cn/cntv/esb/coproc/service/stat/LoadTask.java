package cn.portal.esb.coproc.service.stat;

import java.util.Date;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.stat.StatisticsDbMapper;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.StatisticsModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.redis.RedisCluster;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class LoadTask extends PeriodTask {

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
	private StatisticsDbMapper statisticsDbMapper;
	@Value("${load.period}")
	private int period;

	@PostConstruct
	public void init() {
		super.init(scheduler, zk);
		super.schedule("/control/load", period, 20, null, coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		for (SourceModel source : configData.sources())
			for (SystemModel system : configData.systems())
				load(source.getKey(), system.getName(), min, max);
	}

	private void load(String source, String system, long min, long max) {
		String key = "esb:" + source + ":" + system + ":ks-";
		int freq = sum(redis.zrange(key + "freq", min, max));
		int thrpt = sum(redis.zrange(key + "thrpt", min, max));
		if (freq == 0 || thrpt == 0)
			return;
		StatisticsModel stat = new StatisticsModel();
		stat.setSource(source);
		stat.setSystem(system);
		stat.setFrequency(freq);
		stat.setThroughput(thrpt);
		stat.setTime(new Date(max));
		statisticsDbMapper.save(stat);
	}

	private int sum(Set<String> values) {
		int sum = 0;
		for (String value : values)
			sum += Integer.parseInt(value.substring(0, value.indexOf(",")));
		return sum;
	}

}
