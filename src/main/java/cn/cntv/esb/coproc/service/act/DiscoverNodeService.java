package cn.portal.esb.coproc.service.act;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.main.NodeLatencyDbMapper;
import cn.portal.esb.coproc.model.NodeLatencyModel;
import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.ConfigService;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ZkClient;

import com.sun.jersey.api.client.Client;

@Service
public class DiscoverNodeService extends PeriodTask {

	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private ZkClient zk;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Value("#{dataCenterAware.coprocessorID()}")
	private long coprocessorID;
	@Autowired
	private ForkJoinPool pool;
	@Autowired
	private ConfigData configData;
	@Autowired
	private ConfigService configService;
	@Autowired
	private NodeLatencyDbMapper nodeLatencyDbMapper;
	@Value("${discover.period}")
	private int period;
	@Value("${discover.timeout.conn}")
	private int connTimeout; // 毫秒
	@Value("${discover.timeout.read}")
	private int readTimeout; // 毫秒
	private Client client;

	@PostConstruct
	public void init() {
		client = Client.create();
		client.setConnectTimeout(connTimeout);
		client.setReadTimeout(readTimeout);
		super.init(scheduler, zk);
		super.schedule("/control/discover/node/" + dataCenterID, period, 15,
				null, coprocessorID);
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
		client.destroy();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		if (System.currentTimeMillis() - max > 3 * period)
			return;
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Map<NodeModel, ForkJoinTask<NodeModel>> tasks = new HashMap<>();
					for (NodeModel node : configData.nodes())
						tasks.put(node, new LatencyChecker(node).fork());
					for (ForkJoinTask<NodeModel> task : tasks.values())
						apply(task.join());
				} catch (Exception e) {
					log.warn("discover node task found error", e);
				}
			}
		});
	}

	@SuppressWarnings("serial")
	private class LatencyChecker extends ForkJoinTask<NodeModel> {

		NodeModel node;

		LatencyChecker(NodeModel node) {
			this.node = node;
		}

		@Override
		public NodeModel getRawResult() {
			return node;
		}

		@Override
		protected void setRawResult(NodeModel value) {
			node = value;
		}

		@Override
		protected boolean exec() {
			long start = System.nanoTime();
			int ratio;
			if (node.getSystem().getDiscoverUri() == null) {
				// 没提供监测接口则走tcp连通性测试
				InetSocketAddress endpoint = new InetSocketAddress(
						node.getHost(), node.getPort());
				try (Socket socket = new Socket()) {
					socket.setReuseAddress(true);
					socket.setTcpNoDelay(true);
					socket.setSoLinger(true, 0);
					socket.connect(endpoint, connTimeout);
					node.setLatency((int) ((System.nanoTime() - start) / 1000)); // 微秒
				} catch (IOException e) {
					log.debug("target node {} connectivity abnormal: {}",
							endpoint, e.getMessage());
					node.setLatency(-1);
				}
				ratio = ratio(node.getLatency(), connTimeout);
			} else {
				String endpoint = node.getHost() + ":" + node.getPort();
				String url = "http://" + endpoint
						+ node.getSystem().getDiscoverUri();
				try {
					String json = client.resource(url).get(String.class).trim();
					Map<String, Object> result = MapperUtils.fromJson(json);
					if (result != null && result.containsKey("state")
							&& result.get("state").toString().equals("0")) {
						node.setLatency((int) ((System.nanoTime() - start) / 1000)); // 微秒
					} else {
						log.debug("target node {} discover resp abnormal: {}",
								endpoint, json);
						node.setLatency(-1);
					}
				} catch (RuntimeException e) {
					log.debug("target node {} discover abnormal: {}", endpoint,
							e.getMessage());
					node.setLatency(-1);
				}
				ratio = ratio(node.getLatency(), readTimeout);
			}
			if (node.getSystem().isAutoAdjust()) {
				if (Math.abs(ratio - node.getAdjustRatio()) < 2)
					node.setAdjustRatio(ratio);
				else
					node.setAdjustRatio((ratio + node.getAdjustRatio()) / 2);
			}
			return true;
		}

		// 将延迟数字归一化到一个0-10之间的权重值
		private int ratio(int latency, int timeout) {
			if (latency < 0)
				return 0;
			if (latency < timeout * 5)
				return 10;
			if (latency > timeout * 500)
				return 1;
			return (int) (timeout * 500 / 11.0 / latency + 10 / 11.0);
		}

	}

	private void apply(NodeModel node) {
		// 更新zk
		configService.node(node);
		// 更新db
		NodeLatencyModel nl;
		if ((nl = nodeLatencyDbMapper.find(node.getId(), dataCenterID)) != null) {
			nl.setRatio(node.getAdjustRatio());
			nl.setLatency(node.getLatency());
			nl.setTime(new Date());
			nodeLatencyDbMapper.update(nl);
		} else {
			nl = new NodeLatencyModel();
			nl.setRatio(node.getAdjustRatio());
			nl.setLatency(node.getLatency());
			nl.setTime(new Date());
			nl.setNodeID(node.getId());
			nl.setDataCenterID(dataCenterID);
			nodeLatencyDbMapper.insert(nl);
		}
	}

}
