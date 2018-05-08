package cn.portal.esb.coproc.service.ngx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.model.ngx.SystemNgxModel;
import cn.portal.esb.coproc.service.Action;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.ConfigHandler;
import cn.portal.esb.coproc.service.NgxCacheLocalCluster;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class SystemNgxConfigHandler extends ConfigHandler<SystemModel> {

	private static final Logger log = LoggerFactory
			.getLogger(SystemNgxConfigHandler.class);
	@Autowired
	private NgxCacheLocalCluster ngx;
	@Autowired
	private GroupNgxConfigHandler groupNgxConfigHandler;
	@Autowired
	private ConfigData data; // 小心，有循环依赖，请谨慎处理

	@Override
	public boolean canHandle(Action action, Class<?> clazz) {
		return SystemModel.class.equals(clazz);
	}

	@Override
	public void create(SystemModel current) {
		log.info("create system [{}] config on nginx", current.getName());
		String value = MapperUtils.toJson(SystemNgxModel.toNgx(current,
				ngx.concurrencyEstimate()));
		ngx.set("esb_system_" + current.getName(), value);
		ngx.set("esb_direct_proxy",
				MapperUtils.toJson(SystemNgxModel.transparent(data.systems())));
	}

	@Override
	public void drop(SystemModel previous) {
		log.info("drop system [{}] config on nginx", previous.getName());
		ngx.del("esb_system_" + previous.getName());
		ngx.set("esb_direct_proxy",
				MapperUtils.toJson(SystemNgxModel.transparent(data.systems())));
	}

	@Override
	public void alter(SystemModel previous, SystemModel current) {
		log.info("alter system [{}] config on nginx", current.getName());
		if (!previous.equals(current)) {
			ngx.del("esb_system_" + previous.getName());
			for (GroupModel currGroup : current.getGroups()) {
				GroupModel prevGroup = previous.getGroups().get(
						previous.getGroups().indexOf(currGroup));
				groupNgxConfigHandler.alter(prevGroup, currGroup);
			}
		}
		String value = MapperUtils.toJson(SystemNgxModel.toNgx(current,
				ngx.concurrencyEstimate()));
		ngx.set("esb_system_" + current.getName(), value);
		ngx.set("esb_direct_proxy",
				MapperUtils.toJson(SystemNgxModel.transparent(data.systems())));
	}

}
