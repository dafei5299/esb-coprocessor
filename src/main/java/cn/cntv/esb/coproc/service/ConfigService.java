package cn.portal.esb.coproc.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.main.ApiDbMapper;
import cn.portal.esb.coproc.mapper.main.GroupDbMapper;
import cn.portal.esb.coproc.mapper.main.NgxNodeDbMapper;
import cn.portal.esb.coproc.mapper.main.NodeDbMapper;
import cn.portal.esb.coproc.mapper.main.NodeLatencyDbMapper;
import cn.portal.esb.coproc.mapper.main.SourceDbMapper;
import cn.portal.esb.coproc.mapper.main.SystemDbMapper;
import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.NgxNodeModel;
import cn.portal.esb.coproc.model.NodeLatencyModel;
import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.util.CollUtils;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class ConfigService {

	private static final Logger log = LoggerFactory
			.getLogger(ConfigService.class);
	@Autowired
	private ZkClient zk;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Autowired
	private SystemDbMapper systemDbMapper;
	@Autowired
	private GroupDbMapper groupDbMapper;
	@Autowired
	private ApiDbMapper apiDbMapper;
	@Autowired
	private NodeDbMapper nodeDbMapper;
	@Autowired
	private NodeLatencyDbMapper nodeLatencyDbMapper;
	@Autowired
	private SourceDbMapper sourceDbMapper;
	@Autowired
	private NgxNodeDbMapper ngxNodeDbMapper;

	public void system(SystemModel system) {
		zk.save("/systems/" + system.getId(), MapperUtils.toJson(system));
		log.debug("update system [{}]", system.getName());
	}

	public Action system(long id) {
		Action result = Action.NONE;
		SystemModel system = systemDbMapper.findByID(id);
		String zkPath = "/systems/" + id;
		if (system != null) {
			result = zk.save(zkPath, MapperUtils.toJson(system)) ? Action.CREATE
					: Action.ALTER;
		} else if (zk.remove(zkPath)) {
			result = Action.DROP;
		}
		log.info("refresh system [{}], result: {}", id, result);
		return result;
	}

	public void group(GroupModel group) {
		zk.save("/groups/" + group.getId(), MapperUtils.toJson(group));
		log.debug("update group [{}]", group.getName());
	}

	public Action group(long id) {
		Action result = Action.NONE;
		GroupModel group = groupDbMapper.findByID(id);
		String zkPath = "/groups/" + id;
		if (group != null) {
			result = zk.save(zkPath, MapperUtils.toJson(group)) ? Action.CREATE
					: Action.ALTER;
		} else if (zk.remove(zkPath)) {
			result = Action.DROP;
		}
		log.info("refresh group [{}], result: {}", id, result);
		return result;
	}

	public void api(ApiModel api) {
		zk.save("/apis/" + api.getId(), MapperUtils.toJson(api));
		log.debug("update api [{}]", api.getName());
	}

	public Action api(long id) {
		Action result = Action.NONE;
		ApiModel api = apiDbMapper.findByID(id);
		String zkPath = "/apis/" + id;
		if (api != null) {
			result = zk.save(zkPath, MapperUtils.toJson(api)) ? Action.CREATE
					: Action.ALTER;
		} else if (zk.remove(zkPath)) {
			result = Action.DROP;
		}
		log.info("refresh api [{}], result: {}", id, result);
		return result;
	}

	public void node(NodeModel node) {
		String zkPath = "/nodes/" + node.getId(), subZkPath = zkPath + "/"
				+ dataCenterID;
		zk.ensurePresence(zkPath);
		Map<String, Object> subData = CollUtils.map("latency",
				node.getLatency(), "ratio", node.getAdjustRatio());
		zk.save(subZkPath, MapperUtils.toJson(subData));
		zk.save(zkPath, MapperUtils.toJson(node));
		log.debug("update node [{}:{}]", node.getHost(), node.getPort());
	}

	public Action node(long id) {
		Action result = Action.NONE;
		NodeModel node = nodeDbMapper.findByID(id);
		String zkPath = "/nodes/" + id;
		if (node != null) {
			result = zk.save(zkPath, MapperUtils.toJson(node)) ? Action.CREATE
					: Action.ALTER;
		} else {
			zk.removeChildren(zkPath);
			if (zk.remove(zkPath))
				result = Action.DROP;
		}
		log.info("refresh node [{}], result: {}", id, result);
		return result;
	}

	public Action latency(long id) {
		Action result = Action.NONE;
		NodeLatencyModel latency = nodeLatencyDbMapper.findByID(id);
		if (latency != null) {
			String zkPath = "/nodes/" + latency.getNodeID() + "/"
					+ latency.getDataCenterID();
			Map<String, Object> data = CollUtils.map("latency",
					latency.getLatency(), "ratio", latency.getRatio());
			result = zk.save(zkPath, MapperUtils.toJson(data)) ? Action.CREATE
					: Action.ALTER;
			zk.touch("/nodes/" + latency.getNodeID());
		}
		log.info("refresh latency [{}], result: {}", id, result);
		return result;
	}

	public Action source(long id) {
		Action result = Action.NONE;
		SourceModel source = sourceDbMapper.findByID(id);
		String zkPath = "/sources/" + id;
		if (source != null) {
			result = zk.save(zkPath, MapperUtils.toJson(source)) ? Action.CREATE
					: Action.ALTER;
		} else if (zk.remove(zkPath)) {
			result = Action.DROP;
		}
		log.info("refresh source [{}], result: {}", id, result);
		return result;
	}

	public void ngx(NgxNodeModel ngx) {
		zk.save("/ngxs/" + ngx.getId(), MapperUtils.toJson(ngx));
		log.debug("update ngx [{}:{}]", ngx.getHost(), ngx.getPort());
	}

	public Action ngx(long id) {
		Action result = Action.NONE;
		NgxNodeModel ngx = ngxNodeDbMapper.findByID(id);
		String zkPath = "/ngxs/" + id;
		if (ngx != null) {
			result = zk.save(zkPath, MapperUtils.toJson(ngx)) ? Action.CREATE
					: Action.ALTER;
		} else if (zk.remove(zkPath)) {
			result = Action.DROP;
		}
		log.info("refresh ngx [{}], result: {}", id, result);
		return result;
	}

	// 一个强制刷新的标志位
	public void sync() {
		zk.save("/control/sync", "");
	}

}
