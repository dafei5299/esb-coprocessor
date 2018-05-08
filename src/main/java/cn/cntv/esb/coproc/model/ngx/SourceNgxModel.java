package cn.portal.esb.coproc.model.ngx;

import java.util.HashMap;
import java.util.Map;

import cn.portal.esb.coproc.model.SourceAuthModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.util.CollUtils;

public abstract class SourceNgxModel {

	public static Map<String, Object> toNgx(SourceModel source) {
		// 权限
		Map<String, Map<String, Object>> auths = new HashMap<>();
		for (SourceAuthModel auth : source.getAuths())
			auths.put(auth.getSystem().getName(), CollUtils.map("AppAllow", 1));
		// 来源属性
		return CollUtils.map("AppName", source.getName(), "AppKey",
				source.getKey(), "AppAvailable", source.isEnable() ? 1 : 0,
				"AppPermissions", auths);
	}

}
