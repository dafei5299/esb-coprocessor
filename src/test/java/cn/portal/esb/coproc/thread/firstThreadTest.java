package cn.portal.esb.coproc.thread;

import java.io.IOException;

import cn.portal.esb.coproc.redis.RedisCluster;

public class firstThreadTest {

	public static void main(String[] args) throws IOException {
		RedisCluster redis = new RedisCluster();
		RedisSortedSet redisSortedSet = new RedisSortedSet(
				"192.168.1.208:6379", 3000, 8);
		String redisKey = "portal_esb:statistics_queue";
		String redisSortedSetKey = "queuesecond";
		Thread getJSONStrBySecondThread = new Thread(
				new GetJSONStrBySecondThread(redis, redisKey, redisSortedSet,
						redisSortedSetKey));
		getJSONStrBySecondThread.start();
	}

}
