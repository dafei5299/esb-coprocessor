package cn.portal.esb.coproc.thread;

import cn.portal.esb.coproc.redis.RedisCluster;

public class AllThreadtest {

	public static void main(String[] args) {
		RedisCluster redis = new RedisCluster();
		RedisSortedSet redisSortedSet = new RedisSortedSet(
				"192.168.1.208:6379", 3000, 8);
		RedisSortedSet redisSortedSetCache = new RedisSortedSet(
				"192.168.1.208:6379", 3000, 8);
		String redisKey = "portal_esb:statistics_queue";
		String redisSortedSetSecondKey = "queuesecond";
		String redisSortedSetCacheKey = "queuecache";
		FirstStatistics firstStatistics = new FirstStatistics();
		ThreadTask threadtask = new ThreadTask(redisSortedSet,
				redisSortedSetSecondKey, firstStatistics, redisSortedSetCache,
				redisSortedSetCacheKey);
		Thread getJSONStrBySecondThread = new Thread(
				new GetJSONStrBySecondThread(redis, redisKey, redisSortedSet,
						redisSortedSetSecondKey));
		Thread statistic = new Thread(new StatisticBySecondThread(threadtask));
		Thread mysqlinsert = new Thread(new MySQLInsertDelayThread(threadtask));
		getJSONStrBySecondThread.start();
		statistic.start();
		mysqlinsert.start();
	}

}
