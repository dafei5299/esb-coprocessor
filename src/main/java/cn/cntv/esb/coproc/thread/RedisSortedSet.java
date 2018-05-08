package cn.portal.esb.coproc.thread;

import java.util.List;

import com.google.common.base.Charsets;

/**
 * RedisSortedSet类功能：继承cn.portal.esb.coproc.redis.Redis类，扩展SortedSet数据类型的特定命令
 * Author：liweichao
 */
public class RedisSortedSet {

	/**
	 * 构造函数
	 */
	public RedisSortedSet(String endpoint, int timeout, int maxConn) {
		// super(endpoint, timeout, maxConn);
	}

	/**
	 * ZCARD命令返回队列中元素的个数
	 */
	protected static final byte[] ZCARD = "ZCARD".getBytes(Charsets.US_ASCII);

	public long zcard(String key) {
		// IntegerReply reply = execute(new Command(ZCARD, key));
		// return reply.data();
		return 0L;
	}

	/**
	 * ZADD命令添加元素到队列中
	 */
	protected static final byte[] ZADD = "ZADD".getBytes(Charsets.US_ASCII);

	public void zadd(String key, String... values) {
		// Object[] params = new Object[values.length + 1];
		// params[0] = key;
		// System.arraycopy(values, 0, params, 1, values.length);
		// IntegerReply reply = execute(new Command(ZADD, params));
		// return reply.data();
	}

	/**
	 * ZRANGEBYRANK命令返回分数在指定范围内的成员列表
	 */
	protected static final byte[] ZRANGEBYSCORE = "ZRANGEBYSCORE"
			.getBytes(Charsets.US_ASCII);

	public List<String> zrangebyscore(String key, String min, String max) {
		// Object[] params = new Object[] { key, min, max };
		// MultiBulkReply reply = execute(new Command(ZRANGEBYSCORE, params));
		// if (reply.data() == null)
		// return null;
		// List<String> stringList = reply.asStringList(Charsets.UTF_8);
		// return stringList;
		return null;
	}

	/**
	 * ZREMRANGEBYSCORE命令删除指定范围内分数的成员列表
	 */
	protected static final byte[] ZREMRANGEBYSCORE = "ZREMRANGEBYSCORE"
			.getBytes(Charsets.US_ASCII);

	public void zremrangebyscore(String key, String min, String max) {
		// Object[] params = new Object[] { key, min, max };
		// IntegerReply reply = execute(new Command(ZREMRANGEBYSCORE, params));
		// return reply.data();
	}

	/**
	 * ZRANGE命令返回特定索引范围内带索引的所有数据
	 */
	protected static final byte[] ZRANGE = "ZRANGE".getBytes(Charsets.US_ASCII);

	public List<String> zrange(String key, String min, String max) {
		// Object[] params = new Object[] { key, min, max, "withscores" };
		// MultiBulkReply reply = execute(new Command(ZRANGE, params));
		// if (reply.data() == null)
		// return null;
		// List<String> stringList = reply.asStringList(Charsets.UTF_8);
		// return stringList;
		return null;
	}

	/**
	 * ZCOUNT命令返回特定范围内数据的数量
	 */
	protected static final byte[] ZCOUNT = "ZCOUNT".getBytes(Charsets.US_ASCII);

	public long zcount(String key, String min, String max) {
		// Object[] params = new Object[] { key, min, max };
		// IntegerReply reply = execute(new Command(ZCOUNT, params));
		// return reply.data();
		return 0L;
	}

}
