package cn.portal.esb.coproc.service.ngx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.ngx.GroupNgxModel;
import cn.portal.esb.coproc.ngx.NgxCache;
import cn.portal.esb.coproc.service.Action;
import cn.portal.esb.coproc.service.ConfigHandler;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class GroupNgxConfigHandler extends ConfigHandler<GroupModel> {

	private static final Logger log = LoggerFactory
			.getLogger(GroupNgxConfigHandler.class);
	@Autowired
	private NgxCache ngx;
	@Autowired
	private ApiNgxConfigHandler apiNgxConfigHandler;

	@Override
	public boolean canHandle(Action action, Class<?> clazz) {
		return GroupModel.class.equals(clazz);
	}

	@Override
	public void create(GroupModel current) {
		log.info("create group [{}] config on nginx", current.getName());
		String value = MapperUtils.toJson(GroupNgxModel.toNgx(current));
		ngx.set("esb_group_" + current.getSystem().getName() + "_"
				+ current.getName(), value);
	}

	@Override
	public void drop(GroupModel previous) {
		log.info("drop group [{}] config on nginx", previous.getName());
		ngx.del("esb_group_" + previous.getSystem().getName() + "_"
				+ previous.getName());
	}

	@Override
	public void alter(GroupModel previous, GroupModel current) {
		log.info("alter group [{}] config on nginx", current.getName());
		if (!previous.equals(current)
				|| !previous.getSystem().equals(current.getSystem())) {
			ngx.del("esb_group_" + previous.getSystem().getName() + "_"
					+ previous.getName());
			for (ApiModel currApi : current.getApis()) {
				ApiModel prevApi = previous.getApis().get(
						previous.getApis().indexOf(currApi));
				apiNgxConfigHandler.alter(prevApi, currApi);
			}
		}
		String value = MapperUtils.toJson(GroupNgxModel.toNgx(current));
		ngx.set("esb_group_" + current.getSystem().getName() + "_"
				+ current.getName(), value);
	}

}
