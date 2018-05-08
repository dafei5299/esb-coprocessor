package cn.portal.esb.coproc.thread;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ThreadTask类功能：第二个线程和第三个线程的公共变量 Author：liweichao
 */
public class ThreadTask {

	/** 成员变量，第二个Redis队列，数据类型为SortedSet，RedisSortedSet类的实例，依赖关系 */
	private RedisSortedSet redisSortedSetSecond;

	/** 成员变量，第一个Redis队列，数据类型为SortedSet，RedisSortedSet类的实例，依赖关系 */
	private RedisSortedSet redisSortedSetCache;

	/** 成员变量，用来做统计，FirstStatistics类的实例，依赖关系 */
	private FirstStatistics firststatistics;

	/** 成员变量，第二个Redis队列的key */
	private String redisSortedSetSecondKey;

	/** 成员变量，第三个Redis队列的key */
	private String redisSortedSetCacheKey;

	/** 计时器，redis出现故障的超时时间 */
	private static int waitTimes = 0;

	public ThreadTask(RedisSortedSet redisSortedSetSecond,
			String redisSortedSetSecondKey, FirstStatistics firststatistics,
			RedisSortedSet redisSortedSetCache, String redisSortedSetCacheKey) {

		this.redisSortedSetSecond = redisSortedSetSecond;
		this.redisSortedSetSecondKey = redisSortedSetSecondKey;
		this.firststatistics = firststatistics;
		this.redisSortedSetCache = redisSortedSetCache;
		this.redisSortedSetCacheKey = redisSortedSetCacheKey;
	}

	/**
	 * StatisticBySecondThread线程的任务
	 */
	public synchronized void BRPOPAndStatistics() {
		/** 判断redisSortedSetSecond是否为空 */
		if (this.redisSortedSetSecond.zcard(redisSortedSetSecondKey) != 0) {
			/** 获取Redis队列中第一条记录的RequstTime时间戳 */
			List<String> firstdata = this.redisSortedSetSecond.zrange(
					this.redisSortedSetSecondKey, "0", "0");
			String starttime = firstdata.get(1);// 开始时间
			String stoptime = Long.toString(Long.parseLong(starttime) + 999);// 结束时间
			String overflowtime = Long
					.toString(Long.parseLong(starttime) + 1999);// 溢出时间
			if (this.redisSortedSetSecond.zcount(this.redisSortedSetSecondKey,
					stoptime, overflowtime) > 0) {
				/** 下一秒的记录已经存在了，说明本秒的数据brpop完成了，开始统计 */
				this.StatisticDataInOneSecond(starttime, stoptime);
			} else {
				/**
				 * 下一秒的数据还没有出现，有两种情况，一种是redis队列出现故障，只有本秒的数据，需要设一个超时时间，
				 * 到了把本秒现在的数据做统计
				 */
				if ((waitTimes++) > 1000) {
					this.StatisticDataInOneSecond(starttime, stoptime);
				}
				/** 另一种是本秒内的数据brpop还没完，那么接着brpop不用做处理 */
			}
		}
	}

	/**
	 * MySQLInsertDelayThread线程的任务
	 */
	public synchronized void InsertMysqlDBDelay() {
		/** 如果统计数据队列redisSortedSetCache为空，线程等待 */
		while (this.redisSortedSetCache.zcard(this.redisSortedSetCacheKey) == 0) {
			try {
				System.out.println("redisSortedSetCache队列为空"
						+ Thread.currentThread().getName() + "等待！");
				wait();
			} catch (Exception exp) {
				exp.printStackTrace();
			}
		}
		/** 否则，按秒取出数据插入到mysql数据库中 */
		/** 首先取出当前队列中最小的时间戳 */
		List<String> list = this.redisSortedSetCache.zrange(
				this.redisSortedSetCacheKey, "0", "0");
		String mintimestamp = list.get(1);
		/** 然后取出此特定时间戳的统计数据 */
		List<String> timestamplist = this.redisSortedSetCache.zrangebyscore(
				this.redisSortedSetCacheKey, mintimestamp, mintimestamp);
		/** 删除redis队列中的这些数据 */
		this.redisSortedSetCache.zremrangebyscore(this.redisSortedSetCacheKey,
				mintimestamp, mintimestamp);
		/** 获取写入数据库的时间 */
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String datestr = simpleDateFormat.format(Long.parseLong(mintimestamp));
		/** 获得当前日期数据库statistic表名称 */
		String[] datearray = datestr.substring(0, 10).split("-");
		String tablename = "statistics_";
		for (String str : datearray) {
			tablename += str;
		}
		for (String str2 : timestamplist) {
			String[] countandflowsize = str2.split("#");
			String[] appkeyandsystem = countandflowsize[0].split("\\*");
			String sqlStr = "insert into "
					+ tablename
					+ "(static_esb_times,static_esb_bandwidth,static_esb_from,static_esb_to,static_date) values(?,?,?,?,?)";
			Object[] object = new Object[] { countandflowsize[1],
					countandflowsize[2], appkeyandsystem[0],
					appkeyandsystem[1], datestr };
			int result = MySQLDBHelper.executeNonQuery(sqlStr, object);
			if (result == 1) {
				System.out.println(Thread.currentThread().getName()
						+ "插入数据到mysql中！");
			} else {
				System.out.println("find a exception!");
			}
		}
	}

	/**
	 * 第二个线程的公共方法
	 */
	private void StatisticDataInOneSecond(String starttime, String stoptime) {
		/** 取出数据 */
		List<String> jsonStrInOneSecond = this.redisSortedSetSecond
				.zrangebyscore(this.redisSortedSetSecondKey, starttime,
						stoptime);
		/** 逐条开始统计 */
		for (String str : jsonStrInOneSecond) {
			if (str != null) {
				JSONConvertToBean jsonConvertToBean = new JSONConvertToBean(str);
				this.firststatistics.jsonConvertToBean = jsonConvertToBean;
				this.firststatistics.DoStatistics();
				System.out.println(Thread.currentThread().getName()
						+ "统计了一条数据！");
			}
		}

		System.out.println("1秒内数据统计完毕!");
		/** 把firststatistics的hashmap里面的数据当前时间戳为score写入到redisSortedSetCache中 */
		if (this.firststatistics.hashmap.size() != 0) {
			Iterator<Map.Entry<String, IndicatorBean>> iterator = firststatistics.hashmap
					.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, IndicatorBean> entry = (Map.Entry<String, IndicatorBean>) iterator
						.next();
				Object hashkey = entry.getKey();
				IndicatorBean indicator = (IndicatorBean) entry.getValue();
				/** 添加到redisSortedSetCache队列中 */
				this.redisSortedSetCache.zadd(
						redisSortedSetCacheKey,
						stoptime,
						hashkey + "#" + indicator.getCount() + "#"
								+ indicator.getBandwidth());
			}
			System.out.println("1秒内数据zadd到Redis中结束！");
			/** 清空firststatistics.hashmap */
			this.firststatistics.hashmap.clear();
			/** 删除数据 */
			this.redisSortedSetSecond.zremrangebyscore(
					this.redisSortedSetSecondKey, starttime, stoptime);
			/** 通知另一个线程启动 */
			notifyAll();
		}
	}

}
