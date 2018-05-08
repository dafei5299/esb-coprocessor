package cn.portal.esb.coproc.redis;

import java.util.List;
import java.util.Set;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;

public class RedisShard {

	public static final int BRPOP_BLOCK_TIMEOUT = 1;
	private final RedisInfo info;
	private final String endpoint;
	private final JedisPool pool;

	public RedisShard(RedisInfo info) {
		this.info = info;
		endpoint = info.getHost() + ":" + info.getPort();
		Config conf = new Config();
		conf.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		pool = new JedisPool(conf, info.getHost(), info.getPort()); // 连接池
	}

	public void destroy() {
		try {
			pool.destroy();
		} catch (JedisException _) {
			// ignore
		}
	}

	public RedisInfo getInfo() {
		return info;
	}

	protected Jedis borrow() throws JedisException {
		return pool.getResource();
	}

	protected void returnGood(Jedis jedis) throws JedisException {
		pool.returnResource(jedis);
	}

	protected void returnBad(Jedis jedis) throws JedisException {
		pool.returnBrokenResource(jedis);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((endpoint == null) ? 0 : endpoint.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RedisShard other = (RedisShard) obj;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return endpoint;
	}

	// Commands

	public void lpush(String key, String value) throws JedisException {
		Jedis jedis = borrow();
		try {
			jedis.lpush(key, value);
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public void rpush(String key, String value) throws JedisException {
		Jedis jedis = borrow();
		try {
			jedis.rpush(key, value);
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public String brpop(String key) throws JedisException {
		Jedis jedis = borrow();
		try {
			List<String> result = jedis.brpop(BRPOP_BLOCK_TIMEOUT, key);
			returnGood(jedis);
			return result != null ? result.get(1) : null;
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public void zadd(String key, long score, String value)
			throws JedisException {
		Jedis jedis = borrow();
		try {
			jedis.zadd(key, score, value);
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public Set<String> zrange(String key, long min, long max)
			throws JedisException {
		Jedis jedis = borrow();
		try {
			// 左开右闭区间
			Set<String> result = jedis.zrangeByScore(key, "(" + min, "" + max);
			returnGood(jedis);
			return result;
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public Set<String> zremrange(String key, long min, long max)
			throws JedisException {
		Jedis jedis = borrow();
		try {
			// 用pipeline串联两个操作，先查出来，再给它删掉
			Pipeline pipe = jedis.pipelined();
			// 左开右闭区间
			Response<Set<String>> result = pipe.zrangeByScore(key, "(" + min,
					"" + max);
			pipe.zremrangeByScore(key.getBytes(), ("(" + min).getBytes(),
					("" + max).getBytes());
			pipe.sync();
			returnGood(jedis);
			return result.get();
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public void zremrangebyscore(String key, long min, long max)
			throws JedisException {
		Jedis jedis = borrow();
		try {
			// 左开右闭区间
			jedis.zremrangeByScore(key, "(" + min, "" + max);
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public String get(String key) throws JedisException {
		Jedis jedis = borrow();
		try {
			String result = jedis.get(key);
			returnGood(jedis);
			return result;
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public void set(String key, String value) throws JedisException {
		Jedis jedis = borrow();
		try {
			jedis.set(key, value);
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

	public void ping() throws JedisException {
		Jedis jedis = borrow();
		try {
			jedis.ping();
			returnGood(jedis);
		} catch (JedisException e) {
			returnBad(jedis);
			throw e;
		}
	}

}
