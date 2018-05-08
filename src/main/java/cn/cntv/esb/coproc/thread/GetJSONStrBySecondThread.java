package cn.portal.esb.coproc.thread;

import java.util.Timer;
import java.util.TimerTask;

import cn.portal.esb.coproc.redis.RedisCluster;

/**
 * GetJSONStrBySecondThread是第一个线程，不断从Redis里取JSON数据的线程 Author：liweichao
 */
public class GetJSONStrBySecondThread implements Runnable {

	private RedisCluster redis;
	private String redisKey;// redis队列的key
	private RedisSortedSet redisSortedSet;
	private String redisSortedSetKey;// RedisSortedSet队列的key

	/**
	 * 构造函数重载
	 */
	public GetJSONStrBySecondThread(RedisCluster redis, String redisKey,
			RedisSortedSet redisSortedSet, String redisSortedSetKey) {
		this.redis = redis;
		this.redisKey = redisKey;
		this.redisSortedSet = redisSortedSet;
		this.redisSortedSetKey = redisSortedSetKey;
	}

	/**
	 * 重写接口的run方法
	 */
	public void run() {
		Timer timer = new Timer("GetJSONStrBySecondThread");
		timer.schedule(new GetJSONStrBySecondTask(), 0, 100);// 每100毫秒从Redis里brpop取出一条数据
	}

	/**
	 * 定义一个内部类GetJSONStrBySecondTask，每一毫秒从Redis里取出一条数据
	 */
	class GetJSONStrBySecondTask extends TimerTask {

		/**
		 * 重写基类的run()方法
		 */
		public synchronized void run() {
			String jsonstr = GetJSONStrBySecondThread.this.redis
					.get(GetJSONStrBySecondThread.this.redisKey);
			if (jsonstr != null) {
				JSONConvertToBean jsonConvertToBean = new JSONConvertToBean(
						jsonstr);
				String jsonstrwithscore = jsonConvertToBean.BeanWithScore();
				String[] jsonstrarray = jsonstrwithscore.split("#");
				// 写入到redisSortedSetKey中
				GetJSONStrBySecondThread.this.redisSortedSet.zadd(
						GetJSONStrBySecondThread.this.redisSortedSetKey,
						jsonstrarray[0], jsonstrarray[1]);
			} else {
				System.out.println("portal_esb:statistics_queue队列为空！");
			}
		}
	}

}
