package cn.portal.esb.coproc.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.model.SourceAuthModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ChildrenUpdater;
import cn.portal.esb.coproc.zk.ConnectedAware;
import cn.portal.esb.coproc.zk.NodeDataUpdater;
import cn.portal.esb.coproc.zk.ZkClient;
import cn.portal.esb.coproc.zk.ZkElection;
import cn.portal.esb.coproc.zk.ZkElection.State;

import com.google.common.collect.Sets;

@Service
public class ConfigData {

	private static final Logger log = LoggerFactory.getLogger(ConfigData.class);
	@Autowired
	private ZkClient zk;
	@Autowired
	private ZkElection election;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Autowired
	private ConfigHandler<?>[] handlers;
	private boolean initialized = false;
	private Map<Long, SystemModel> systems = new ConcurrentHashMap<>();
	private Map<Long, GroupModel> groups = new ConcurrentHashMap<>();
	private Map<Long, ApiModel> apis = new ConcurrentHashMap<>();
	private Map<Long, NodeModel> nodes = new ConcurrentHashMap<>();
	private Map<Long, SourceModel> sources = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
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
		initialized = true;
		log.info("configuration load from zookeeper completed");
	}

	private void watch() {
		zk.ensurePresence("/systems").watchChildren("/systems",
				new SystemChildrenUpdater());
		zk.ensurePresence("/groups").watchChildren("/groups",
				new GroupChildrenUpdater());
		zk.ensurePresence("/apis").watchChildren("/apis",
				new ApiChildrenUpdater());
		zk.ensurePresence("/nodes").watchChildren("/nodes",
				new NodeChildrenUpdater());
		zk.ensurePresence("/sources").watchChildren("/sources",
				new SourceChildrenUpdater());
		zk.ensurePresence("/control/sync").watchNodeData("/control/sync",
				new SyncNodeDataUpdater());
	}

	public List<SystemModel> systems() {
		List<SystemModel> list = new ArrayList<>();
		for (SystemModel system : systems.values())
			list.add(MapperUtils.copy(system));
		return list;
	}

	public SystemModel system(long id) {
		return MapperUtils.copy(systems.get(id));
	}

	public SystemModel system(String s) {
		for (SystemModel system : systems.values())
			if (system.getName().equals(s))
				return MapperUtils.copy(system);
		return null;
	}

	public List<GroupModel> groups() {
		List<GroupModel> list = new ArrayList<>();
		for (GroupModel group : groups.values())
			list.add(MapperUtils.copy(group));
		return list;
	}

	public GroupModel group(long id) {
		return MapperUtils.copy(groups.get(id));
	}

	public GroupModel group(String s, String g) {
		for (SystemModel system : systems.values()) {
			if (!system.getName().equals(s))
				continue;
			for (GroupModel group : system.getGroups())
				if (group.getName().equals(g))
					return MapperUtils.copy(group);
			break;
		}
		return null;
	}

	public List<ApiModel> apis() {
		List<ApiModel> list = new ArrayList<>();
		for (ApiModel api : apis.values())
			list.add(MapperUtils.copy(api));
		return list;
	}

	public ApiModel api(long id) {
		return MapperUtils.copy(apis.get(id));
	}

	public ApiModel api(String s, String g, String a) {
		for (SystemModel system : systems.values()) {
			if (!system.getName().equals(s))
				continue;
			for (GroupModel group : system.getGroups()) {
				if (!group.getName().equals(g))
					continue;
				for (ApiModel api : group.getApis())
					if (api.getName().equals(a) && api.isDefaultVersion())
						return MapperUtils.copy(api);
				break;
			}
			break;
		}
		return null;
	}

	public ApiModel api(String s, String g, String a, int v) {
		for (SystemModel system : systems.values()) {
			if (!system.getName().equals(s))
				continue;
			for (GroupModel group : system.getGroups()) {
				if (!group.getName().equals(g))
					continue;
				for (ApiModel api : group.getApis())
					if (api.getName().equals(a) && api.getVersion() == v)
						return MapperUtils.copy(api);
				break;
			}
			break;
		}
		return null;
	}

	public List<NodeModel> nodes() {
		List<NodeModel> list = new ArrayList<>();
		for (NodeModel node : nodes.values())
			list.add(MapperUtils.copy(node));
		return list;
	}

	public NodeModel node(long id) {
		return MapperUtils.copy(nodes.get(id));
	}

	public List<SourceModel> sources() {
		List<SourceModel> list = new ArrayList<>();
		for (SourceModel source : sources.values())
			list.add(MapperUtils.copy(source));
		return list;
	}

	public SourceModel source(long id) {
		return MapperUtils.copy(sources.get(id));
	}

	private class SystemChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevSystems = systems.keySet(), currSystems = new HashSet<>();
			for (String child : children)
				currSystems.add(Long.parseLong(child));
			for (Long id : Sets.difference(prevSystems, currSystems)) {
				SystemModel prev = systems.remove(id);
				Assert.state(prev.getGroups().size() == 0, "groups not empty");
				Assert.state(prev.getNodes().size() == 0, "nodes not empty");
				drop(prev);
			}
			for (Long id : Sets.difference(currSystems, prevSystems))
				zk.watchNodeData("/systems/" + id, new SystemNodeDataUpdater());
		}
	}

	private class SystemNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			SystemModel curr = MapperUtils
					.fromJson(nodeData, SystemModel.class);
			log.info("cuipengfei log systemModel [{}]",curr.toString());
			SystemModel prev = systems.put(curr.getId(), curr);
			if (prev == null) {
				create(curr);
			} else {
				SystemModel snapshot = MapperUtils.copy(prev);
				for (GroupModel group : prev.getGroups())
					group.setSystem(curr);
				for (NodeModel node : prev.getNodes())
					node.setSystem(curr);
				if (!curr.deepEquals(prev))
					alter(snapshot, curr);
			}
		}
	}

	private class GroupChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevGroups = groups.keySet(), currGroups = new HashSet<>();
			for (String child : children)
				currGroups.add(Long.parseLong(child));
			for (Long id : Sets.difference(prevGroups, currGroups)) {
				GroupModel prev = groups.remove(id);
				Assert.state(prev.getApis().size() == 0, "apis not empty");
				GroupModel snapshot = MapperUtils.copy(prev);
				prev.setSystem(null);
				drop(snapshot);
			}
			for (Long id : Sets.difference(currGroups, prevGroups))
				zk.watchNodeData("/groups/" + id, new GroupNodeDataUpdater());
		}
	}

	private class GroupNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			GroupModel curr = MapperUtils.fromJson(nodeData, GroupModel.class);
			GroupModel prev = groups.put(curr.getId(), curr);
			if (prev == null) {
				curr.setSystem(getSystem(curr.getSystemID()));
				create(curr);
			} else {
				GroupModel snapshot = MapperUtils.copy(prev);
				prev.setSystem(null);
				curr.setSystem(getSystem(curr.getSystemID()));
				for (ApiModel api : prev.getApis())
					api.setGroup(curr);
				if (!curr.deepEquals(prev))
					alter(snapshot, curr);
			}
		}
	}

	private class ApiChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevApis = apis.keySet(), currApis = new HashSet<>();
			for (String child : children)
				currApis.add(Long.parseLong(child));
			for (Long id : Sets.difference(prevApis, currApis)) {
				ApiModel prev = apis.remove(id);
				ApiModel snapshot = MapperUtils.copy(prev);
				prev.setGroup(null);
				drop(snapshot);
			}
			for (Long id : Sets.difference(currApis, prevApis))
				zk.watchNodeData("/apis/" + id, new ApiNodeDataUpdater());
		}
	}

	private class ApiNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			ApiModel curr = MapperUtils.fromJson(nodeData, ApiModel.class);
			ApiModel prev = apis.put(curr.getId(), curr);
			if (prev == null) {
				curr.setGroup(getGroup(curr.getGroupID()));
				create(curr);
			} else {
				ApiModel snapshot = MapperUtils.copy(prev);
				prev.setGroup(null);
				curr.setGroup(getGroup(curr.getGroupID()));
				if (!curr.deepEquals(prev))
					alter(snapshot, curr);
			}
		}
	}

	private class NodeChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevNodes = nodes.keySet(), currNodes = new HashSet<>();
			for (String child : children)
				currNodes.add(Long.parseLong(child));
			for (Long id : Sets.difference(prevNodes, currNodes)) {
				NodeModel prev = nodes.remove(id);
				NodeModel snapshot = MapperUtils.copy(prev);
				prev.setSystem(null);
				drop(snapshot);
			}
			for (Long id : Sets.difference(currNodes, prevNodes))
				zk.watchNodeData("/nodes/" + id, new NodeNodeDataUpdater());
		}
	}

	private class NodeNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			NodeModel curr = MapperUtils.fromJson(nodeData, NodeModel.class);
			if (curr == null)
				return;
			Map<String, Object> subData = MapperUtils.fromJson(zk
					.data("/nodes/" + curr.getId() + "/" + dataCenterID));
			if (subData != null) {
				curr.setAdjustRatio((int) subData.get("ratio"));
				curr.setLatency((int) subData.get("latency"));
			}
			NodeModel prev = nodes.put(curr.getId(), curr);
			if (prev == null) {
				curr.setSystem(getSystem(curr.getSystemID()));
				create(curr);
			} else {
				NodeModel snapshot = MapperUtils.copy(prev);
				prev.setSystem(null);
				curr.setSystem(getSystem(curr.getSystemID()));
				if (!curr.deepEquals(prev))
					alter(snapshot, curr);
			}
		}
	}

	private class SourceChildrenUpdater implements ChildrenUpdater {
		@Override
		public void onChange(List<String> children) {
			Set<Long> prevSources = sources.keySet(), currSources = new HashSet<>();
			for (String child : children)
				currSources.add(Long.parseLong(child));
			for (Long id : Sets.difference(prevSources, currSources))
				drop(sources.remove(id));
			for (Long id : Sets.difference(currSources, prevSources))
				zk.watchNodeData("/sources/" + id, new SourceNodeDataUpdater());
		}
	}

	private class SourceNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			SourceModel curr = MapperUtils
					.fromJson(nodeData, SourceModel.class);
			for (SourceAuthModel auth : curr.getAuths())
				auth.setSystem(getSystem(auth.getSystemID()));
			SourceModel prev = sources.put(curr.getId(), curr);
			if (prev == null) {
				create(curr);
			} else {
				if (!curr.deepEquals(prev))
					alter(prev, curr);
			}
		}
	}

	private class SyncNodeDataUpdater implements NodeDataUpdater {
		@Override
		public void onChange(String nodeData) {
			// 强制刷新标志位，强制推送到转发服务
			for (SystemModel system : systems.values())
				create(system);
			for (GroupModel group : groups.values())
				create(group);
			for (ApiModel api : apis.values())
				create(api);
			// node 属于 system，可省略
			// for (NodeModel node : nodes.values())
			// create(node);
			for (SourceModel source : sources.values())
				create(source);
		}
	}

	private SystemModel getSystem(long id) {
		Assert.isTrue(systems.containsKey(id), "system not found");
		return systems.get(id);
	}

	private GroupModel getGroup(long id) {
		Assert.isTrue(groups.containsKey(id), "group not found");
		return groups.get(id);
	}

	@SuppressWarnings("unchecked")
	private <T> void create(T current) {
		if (!initialized)
			return;
		// 只有Leader才可以推送
		if (election.state() != State.LEADER)
			return;
		for (ConfigHandler<?> handler : handlers)
			if (handler.canHandle(Action.CREATE, current.getClass()))
				((ConfigHandler<T>) handler).create(current);
	}

	@SuppressWarnings("unchecked")
	private <T> void drop(T previous) {
		// 只有Leader才可以推送
		if (election.state() != State.LEADER)
			return;
		for (ConfigHandler<?> handler : handlers)
			if (handler.canHandle(Action.CREATE, previous.getClass()))
				((ConfigHandler<T>) handler).drop(previous);
	}

	@SuppressWarnings("unchecked")
	private <T> void alter(T previous, T current) {
		// 只有Leader才可以推送
		if (election.state() != State.LEADER)
			return;
		for (ConfigHandler<?> handler : handlers)
			if (handler.canHandle(Action.ALTER, current.getClass()))
				((ConfigHandler<T>) handler).alter(previous, current);
	}

}
