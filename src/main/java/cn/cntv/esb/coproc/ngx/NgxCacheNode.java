package cn.portal.esb.coproc.ngx;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

public class NgxCacheNode implements NgxCache {

	private static final Logger log = LoggerFactory
			.getLogger(NgxCacheNode.class);
	private Client client;
	private WebResource get;
	private AsyncWebResource set;
	private AsyncWebResource del;
	private String endpoint;
	private int retryDelay;
	private int maxTryTimes;
	private ScheduledExecutorService executor;

	public NgxCacheNode(String endpoint, int timeout, int retryDelay,
			int maxTryTimes, ScheduledExecutorService executor) {
		Assert.hasText(endpoint, "endpoint should not be empty");
		Assert.isTrue(timeout >= 0,
				"connect/read timeout should be positive or zero");
		Assert.isTrue(retryDelay > 0, "retry delay should be positive");
		Assert.isTrue(maxTryTimes > 0, "max try times should be positive");
		Assert.notNull(executor, "executor not assigned");
		client = Client.create();
		client.setConnectTimeout(timeout);
		client.setReadTimeout(timeout);
		client.setExecutorService(executor);
		get = client.resource("http://" + endpoint + "/c_get");
		set = client.asyncResource("http://" + endpoint + "/c_set");
		del = client.asyncResource("http://" + endpoint + "/c_del");
		this.endpoint = endpoint;
		this.retryDelay = retryDelay;
		this.maxTryTimes = maxTryTimes;
		this.executor = executor;
	}

	@Override
	public void close() throws IOException {
		client.destroy();
	}

	public String endpoint() {
		return endpoint;
	}

	@Override
	public String get(String key) {
		// 同步调用
		String result = get.queryParam("key", check(key)).get(String.class)
				.trim();
		if (!result.isEmpty())
			return result;
		return null;
	}

	@Override
	public void set(String key, String value) {
		doSet(check(key), check(value), maxTryTimes);
	}

	private void doSet(final String key, final String value,
			final int remainingTryTimes) {
		Form form = new Form();
		form.add("key", key);
		form.add("value", value);
		// 异步调用
		ListenableFuture<String> future = JdkFutureAdapters.listenInPoolThread(
				set.post(String.class, form), executor);
		// 回调，失败重试
		Futures.addCallback(future, new FutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				log.debug("ngx {} set ok: [{}]=[{}]", endpoint, key, value);
			}

			@Override
			public void onFailure(Throwable t) {
				if (remainingTryTimes > 1) {
					executor.schedule(new Runnable() {
						public void run() {
							doSet(key, value, remainingTryTimes - 1);
						}
					}, retryDelay, TimeUnit.MILLISECONDS);
					return;
				}
				log.warn("ngx {} set error, try {} times: [{}]=[{}]", endpoint,
						maxTryTimes, key, value, t);
			}
		});
	}

	@Override
	public void del(String key) {
		doDel(check(key), maxTryTimes);
	}

	private void doDel(final String key, final int remainingTryTimes) {
		// 异步调用
		ListenableFuture<String> future = JdkFutureAdapters.listenInPoolThread(
				del.queryParam("key", key).get(String.class), executor);
		// 回调，失败重试
		Futures.addCallback(future, new FutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				log.debug("ngx {} del ok: [{}]", endpoint, key);
			}

			@Override
			public void onFailure(Throwable t) {
				if (remainingTryTimes > 1) {
					executor.schedule(new Runnable() {
						public void run() {
							doDel(key, remainingTryTimes - 1);
						}
					}, retryDelay, TimeUnit.MILLISECONDS);
					return;
				}
				log.warn("ngx {} del error, try {} times: [{}]", endpoint,
						maxTryTimes, key, t);
			}
		});
	}

	private String check(String text) {
		Assert.hasText(text, "key/value should not be empty");
		return text.trim();
	}

}
