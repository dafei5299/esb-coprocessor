package cn.portal.esb.coproc.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.main.NgxNodeDbMapper;
import cn.portal.esb.coproc.model.NgxNodeModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.ngx.NgxCache;
import cn.portal.esb.coproc.ngx.NgxCacheCluster;
import cn.portal.esb.coproc.util.ConnectionUtil;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.util.sub.Result;
import cn.portal.esb.coproc.zk.ChildrenUpdater;
import cn.portal.esb.coproc.zk.ConnectedAware;
import cn.portal.esb.coproc.zk.NodeDataUpdater;
import cn.portal.esb.coproc.zk.ZkClient;
import cn.portal.esb.coproc.zk.ZkElection;
import cn.portal.esb.coproc.zk.ZkElection.State;

import com.google.common.collect.Sets;

@Service
public class NgxCacheLocalCluster implements NgxCache {

	private static final Logger log = LoggerFactory
			.getLogger(NgxCacheLocalCluster.class);
	@Autowired
	private ZkClient zk;
	@Autowired
	private ZkElection election;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Value("${ngx.timeout}")
	private int timeout;
	@Value("${ngx.retryDelay}")
	private int retryDelay;
	@Value("${ngx.maxTryTimes}")
	private int maxTryTimes;
	@Value("${ngx.flushPeriod}")
	private int flushPeriod;
	@Value("${admin.url}")
	private String adminUrl;

	@Autowired
	private NgxNodeDbMapper ngxNodeDbMapper;
	@Autowired
	private ConfigData configData;
	@Autowired
	private ConfigService configService;
	private NgxCacheCluster ngx;
	private Map<Long, NgxNodeModel> ngxs = new ConcurrentHashMap<>();
	private Set<String> availableNodes = new ConcurrentSkipListSet<>();
	private int concurrencyEstimatePerNgxNode = 0;
	private long lastRetriveConcurrencyEstimate = 0;

	@PostConstruct
	public void init() {
		ngx = new NgxCacheCluster("", timeout, retryDelay, maxTryTimes,
				flushPeriod);
		zk.addAware(new ConnectedAware() {
			private boolean needBind = false;

			@Override
			public void disconnect(Type type) {
				log.warn("zk disconnect {}", type);
				needBind = type == Type.EXPIRED;
			}

			@Override
			public void reconnect() {
				log.warn("zk reconnect, need register watchers? {}", needBind);
				if (needBind)
					watch();
			}
		});
		watch();
	}

	@Override
	public void close() throws IOException {
		if (ngx != null)
			ngx.close();
	}

	private void watch() {
		zk.ensurePresence("/ngxs").watchChildren("/ngxs",
				new NgxChildrenUpdater());
	}

	public List<NgxNodeModel> ngxs() {
		List<NgxNodeModel> list = new ArrayList<>();
		for (NgxNodeModel ngx : ngxs.values())
			list.add(MapperUtils.copy(ngx));
		return list;
	}

	public NgxNodeModel ngx(long id) {
		return MapperUtils.copy(ngxs.get(id));
	}

	public int concurrencyEstimate() {
		if (System.currentTimeMillis() - lastRetriveConcurrencyEstimate > 30000) {
			concurrencyEstimatePerNgxNode = ngxNodeDbMapper
					.getConcurrencyEstimatePerNgxNode();
			lastRetriveConcurrencyEstimate = System.currentTimeMillis();
		}
		return availableNodes.size() * concurrencyEstimatePerNgxNode;
	}

	// 观察zk中的转发服务配置，是否有节点增删
	private class NgxChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevNgxs = ngxs.keySet(), currNgxs = new HashSet<>();
			for (String child : children)
				currNgxs.add(Long.parseLong(child));
			// 原先有，更新后没了的节点
			for (Long id : Sets.difference(prevNgxs, currNgxs)) {
				NgxNodeModel prev = ngxs.remove(id);
				ngx.remove(prev.getHost() + ":" + prev.getPort());
				availableNodes.remove(prev.getHost() + ":" + prev.getPort());
				checkSystemConcurrentyEstimate();
			}
			// 原先没有，更新后出现的节点
			for (Long id : Sets.difference(currNgxs, prevNgxs))
				zk.watchNodeData("/ngxs/" + id, new NgxNodeDataUpdater());
		}
	}

	// 注册zk，观察转发服务节点是否配置变更
	private class NgxNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			NgxNodeModel curr = MapperUtils.fromJson(nodeData,
					NgxNodeModel.class);
			if (curr.getDatacenterID() == dataCenterID) {
				// 只关心本机房的转发服务
				NgxNodeModel prev = ngxs.put(curr.getId(), curr);
				if (prev == null) {
					ngx.add(curr.getHost() + ":" + curr.getPort());
				} else if (!prev.equals(curr)) {
					ngx.remove(prev.getHost() + ":" + prev.getPort());
					ngx.add(curr.getHost() + ":" + curr.getPort());
				}
			} else if (ngxs.containsKey(curr.getId())) {
				NgxNodeModel prev = ngxs.remove(curr.getId());
				ngx.remove(prev.getHost() + ":" + prev.getPort());
			}
			if (curr.isEnable() && curr.isAvailable())
				availableNodes.add(curr.getHost() + ":" + curr.getPort());
			else
				availableNodes.remove(curr.getHost() + ":" + curr.getPort());
			checkSystemConcurrentyEstimate();
			// TODO 修改LVS可用列表 TOTEST 要有请求失败时重新请求的逻辑
			if(election.state()== State.LEADER){
				updateLvs(availableNodes);
			}			
		}

		/**
		 * 通知后台更新 lvs
		 * @author 吴昌龙
		 */
		private void updateLvs(Set<String> availableNodes) {
			String lvscontent = "";
			for(String node : availableNodes){
				String[] temp = node.split(":");
				lvscontent += temp[0]+"_"+temp[1];
				lvscontent += ",";
			}
			if(lvscontent!=""){
				lvscontent = lvscontent.substring(0,lvscontent.lastIndexOf(','));
			}
			String url = adminUrl+"interface.php/interface/?action=rsyncLvs&lvscontent=" + lvscontent;
			Result<String> result = ConnectionUtil.tryConnect(url, 3);
			if(result.isOk()){
				log.info("[NgxCacheLocalCluster] Lvs response: {}",result.getResultVal());
			}else{
				log.error("[NgxCacheLocalCluster] Lvs exception: {}",result.getMsg());
				log.info("[NgxCacheLocalCluster] Lvs exception info:{}",url);
			}
		}
		
	}

	// 检查是否触发服务降级
	private void checkSystemConcurrentyEstimate() {
		int platformConcurrencyEstimate = concurrencyEstimate();
		for (SystemModel system : configData.systems()) {
			if (system.getConcurrencyEstimate() > platformConcurrencyEstimate
					&& !system.isConcurrencyExceeded()) {
				log.info(
						"degrade concurrency estimate exceed, system {}: {}, platform: {}",
						system.getName(), system.getConcurrencyEstimate(),
						platformConcurrencyEstimate);
				system.setConcurrencyExceeded(true);
				configService.system(system);
			} else if (system.getConcurrencyEstimate() < platformConcurrencyEstimate
					&& system.isConcurrencyExceeded()) {
				log.info(
						"degrade concurrency estimate recover, system {}: {}, platform: {}",
						system.getName(), system.getConcurrencyEstimate(),
						platformConcurrencyEstimate);
				system.setConcurrencyExceeded(false);
				configService.system(system);
			}
		}
	}

	@Override
	public String get(String key) {
		return ngx.get(key);
	}

	@Override
	public void set(String key, String value) {
		ngx.set(key, value);
	}

	@Override
	public void del(String key) {
		ngx.del(key);
	}

}
