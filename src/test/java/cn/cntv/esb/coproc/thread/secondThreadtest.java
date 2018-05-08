package cn.portal.esb.coproc.thread;

public class secondThreadtest {

	public static void main(String[] args) {
		String redisSortedSetSecondKey = "queuesecond";
		String redisSortedSetCacheKey = "queuecache";
		RedisSortedSet redisSortedSet = new RedisSortedSet(
				"192.168.1.208:6379", 3000, 8);
		RedisSortedSet redisSortedSetCache = new RedisSortedSet(
				"192.168.1.208:6379", 3000, 8);
		FirstStatistics firstStatistics = new FirstStatistics();
		ThreadTask threadtask = new ThreadTask(redisSortedSet,
				redisSortedSetSecondKey, firstStatistics, redisSortedSetCache,
				redisSortedSetCacheKey);
		Thread statistic = new Thread(new StatisticBySecondThread(threadtask));
		Thread mysqlinsert = new Thread(new MySQLInsertDelayThread(threadtask));
		statistic.start();
		mysqlinsert.start();
	}

}
