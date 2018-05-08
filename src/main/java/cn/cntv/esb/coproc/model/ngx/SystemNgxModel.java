package cn.portal.esb.coproc.model.ngx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.util.CollUtils;

public abstract class SystemNgxModel {

	public static Map<String, Object> toNgx(SystemModel system,
			int platformConcurrencyEstimate) {
		Map<String, Object> map = new LinkedHashMap<>();

		// 节点
		List<Integer> networkLevel = new ArrayList<>();
		List<Map<String, Object>> nodes = new ArrayList<>();
		for (NodeModel node : system.getNodes()) {
			// 节点权重
			int weight = node.getWeight() * node.getAdjustRatio();
			// 节点可用性
			if (!node.isEnable() || weight == 0 || node.getLatency() < 0)
				continue;
			// 链路优化
			networkLevel.add(node.getAdjustRatio());
			// 节点属性
			nodes.add(CollUtils.map("ServerIP", node.getHost(), "ServerPort",
					node.getPort(), "ServerWeight", weight, "ServerDebug",
					(node.isDebug() ? 1 : 0)));
		}
		if (nodes.isEmpty()) {
			for (NodeModel n : system.getNodes()) {
				if (n.isEnable() && n.getWeight() > 0) {
					nodes.add(CollUtils.map("ServerIP", n.getHost(),
							"ServerPort", n.getPort(), "ServerWeight",
							n.getWeight(), "ServerDebug", (n.isDebug() ? 1 : 0)));
				}
			}
		} else if (!system.isAutoAdjust()) {
			// 未开启自动托管，使用链路优化，根据设定分级，只保留最优链路
			int retain = Collections.max(networkLevel);
			for (int i = networkLevel.size() - 1; i >= 0; i--)
				if (networkLevel.get(i) < retain)
					nodes.remove(i);
		}
		map.put("SystemServers", nodes);

		// 系统可用性
		map.put("SystemAvailable", system.isEnable() ? 1 : 0);

		// 调试开关
		map.put("SystemDebug", CollUtils.map("DebugSwitcher",
				system.isDebug() ? 1 : 0, "DebugKey", system.getDebugKey()));

		// 系统限制
		Map<String, Object> limits = new LinkedHashMap<>();
		limits.put("LimitTimes", system.getFrequencyLimit());
		limits.put("CircleTimes", system.getFrequencyPeriod());
		limits.put("LimitBandwidth", system.getThroughputLimit());
		limits.put("CircleBandwidth", system.getThroughputPeriod());
		limits.put("LimitConnection", system.getConnectionLimit());
		limits.put("CircleConnection", 1);
		if (system.isDebug()) {
			limits.put("CurrentTimes", system.getFrequencyCurrent());
			limits.put("CurrentBandwidth", system.getThroughputCurrent());
			limits.put("CurrentConnection", system.getConnectionCurrent());
		}
		map.put("SystemLimits", limits);

		// 限制状态
		boolean degradeDeny = system.getConcurrencyEstimate() > platformConcurrencyEstimate;
		map.put("SystemSwitchers", CollUtils.map("SwitcherDenyDegrade",
				degradeDeny ? 1 : 0, "SwitcherTimes",
				system.isFrequencyExceeded() ? 0 : 1, "SwitcherBandwidth",
				system.isThroughputExceeded() ? 0 : 1, "SwitcherConnection",
				system.isConnectionExceeded() ? 0 : 1));

		// 降级延迟
		int deferred = system.isShouldDeferred() ? system.getRequestDeferred()
				: 0;
		if (system.isDebug() && system.getDeferredReason() != null) {
			map.put("SystemDegrades", CollUtils.map("DegradeDelay", deferred,
					"DelayResult", system.getDeferredReason()));
		} else {
			map.put("SystemDegrades", CollUtils.map("DegradeDelay", deferred));
		}

		return map;
	}

	public static Map<String, Object> transparent(List<SystemModel> systems) {
		Map<String, Object> map = new HashMap<>();
		for (SystemModel system : systems)
			map.put(system.getName(), system.isTransparent() ? 1 : 0);
		return map;
	}

}
