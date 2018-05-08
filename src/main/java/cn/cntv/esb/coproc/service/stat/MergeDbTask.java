package cn.portal.esb.coproc.service.stat;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.stat.StatisticsDbMapper;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class MergeDbTask extends PeriodTask {

	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private ZkClient zk;
	@Autowired
	private StatisticsDbMapper statisticsDbMapper;
	@Value("#{dataCenterAware.coprocessorID()}")
	private long coprocessorID;
	@Value("${statistics.expireDays}")
	private int expireDays;
	@Value("${statistics.mergePeriod}")
	private int mergePeriod;

	@PostConstruct
	public void init() {
		super.init(scheduler, zk);
		super.schedule("/control/merge/db", 86400000, 60, null, coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		DateTime then = new DateTime(max).minusDays(expireDays);
		merge(then);
	}

	public void merge(DateTime then) {
		String dayTable = "statistics_" + then.toString("YYYYMMdd");
		String yearTable = "statistics_" + then.toString("YYYY");
		if (!statisticsDbMapper.exists(dayTable))
			return;
		statisticsDbMapper.merge(mergePeriod, dayTable, yearTable);
		statisticsDbMapper.drop(dayTable);
		log.info("merge db task finished, compact {} stat in mysql",
				then.toString("YYYY-MM-dd"));
	}

}
