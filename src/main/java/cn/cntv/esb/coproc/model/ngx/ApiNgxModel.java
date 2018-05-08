package cn.portal.esb.coproc.model.ngx;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.ApiParamModel;
import cn.portal.esb.coproc.util.CollUtils;

public abstract class ApiNgxModel {

	public static Map<String, Object> toNgx(ApiModel api) {
		Map<String, Object> map = new LinkedHashMap<>();

		// 接口基本属性
		map.put("ApiUri", api.getUri());
		map.put("ApiMethod", api.getMethod());
		map.put("ApiAvailable", api.isEnable() ? 1 : 0);

		// 接口限制
		Map<String, Object> limits = new LinkedHashMap<>();
		limits.put("LimitTimes", api.getFrequencyLimit());
		limits.put("CircleTimes", api.getFrequencyPeriod());
		limits.put("LimitBandwidth", api.getThroughputLimit());
		limits.put("CircleBandwidth", api.getThroughputPeriod());
		limits.put("LimitConnection", api.getConnectionLimit());
		limits.put("CircleConnection", 1);
		if (api.getGroup().getSystem().isDebug()) {
			limits.put("CurrentTimes", api.getFrequencyCurrent());
			limits.put("CurrentBandwidth", api.getThroughputCurrent());
			limits.put("CurrentConnection", api.getConnectionCurrent());
		}
		map.put("ApiLimits", limits);

		// 限制状态
		map.put("ApiSwitchers", CollUtils.map("SwitcherApiTest",
				api.isTest() ? 1 : 0, "SwitcherTimes",
				api.isFrequencyExceeded() ? 0 : 1, "SwitcherBandwidth",
				api.isThroughputExceeded() ? 0 : 1, "SwitcherConnection",
				api.isConnectionExceeded() ? 0 : 1));

		// 降级延迟、请求超时
		int deferred = api.isShouldDeferred() ? api.getRequestDeferred() : 0;
		if (api.getGroup().getSystem().isDebug()
				&& api.getDeferredReason() != null) {
			map.put("ApiDegrades",
					CollUtils.map("DegradeDelay", deferred, "DelayResult",
							api.getDeferredReason(), "WaitTimeout",
							api.getTimeout()));
		} else {
			map.put("ApiDegrades",
					CollUtils.map("DegradeDelay", deferred, "WaitTimeout",
							api.getTimeout()));
		}

		// 参数
		Map<String, String> params = new HashMap<>();
		for (ApiParamModel param : api.getParams())
			params.put(param.getField(), param.getType());
		map.put("ApiParamaters", params);
		map.put("ApiParamatersFilter", api.isInjectionDetection() ? 1 : 0);

		return map;
	}

}
