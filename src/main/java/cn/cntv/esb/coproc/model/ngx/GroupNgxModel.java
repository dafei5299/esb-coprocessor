package cn.portal.esb.coproc.model.ngx;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.util.CollUtils;

public abstract class GroupNgxModel {

	public static Map<String, Object> toNgx(GroupModel group) {
		Map<String, Object> map = new LinkedHashMap<>();

		// 分组可用性
		map.put("GroupAvailable", group.isEnable() ? 1 : 0);

		// 分组限制
		Map<String, Object> limits = new LinkedHashMap<>();
		limits.put("LimitTimes", group.getFrequencyLimit());
		limits.put("CircleTimes", group.getFrequencyPeriod());
		limits.put("LimitBandwidth", group.getThroughputLimit());
		limits.put("CircleBandwidth", group.getThroughputPeriod());
		limits.put("LimitConnection", group.getConnectionLimit());
		limits.put("CircleConnection", 1);
		if (group.getSystem().isDebug()) {
			limits.put("CurrentTimes", group.getFrequencyCurrent());
			limits.put("CurrentBandwidth", group.getThroughputCurrent());
			limits.put("CurrentConnection", group.getConnectionCurrent());
		}
		map.put("GroupLimits", limits);

		// 限制状态
		map.put("GroupSwitchers", CollUtils.map("SwitcherTimes",
				group.isFrequencyExceeded() ? 0 : 1, "SwitcherBandwidth",
				group.isThroughputExceeded() ? 0 : 1, "SwitcherConnection",
				group.isConnectionExceeded() ? 0 : 1));

		// 降级延迟
		int deferred = group.isShouldDeferred() ? group.getRequestDeferred()
				: 0;
		if (group.getSystem().isDebug() && group.getDeferredReason() != null) {
			map.put("GroupDegrades", CollUtils.map("DegradeDelay", deferred,
					"DelayResult", group.getDeferredReason()));
		} else {
			map.put("GroupDegrades", CollUtils.map("DegradeDelay", deferred));
		}

		return map;
	}

}
