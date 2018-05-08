package cn.portal.esb.coproc.service.ngx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.ngx.ApiNgxModel;
import cn.portal.esb.coproc.ngx.NgxCache;
import cn.portal.esb.coproc.service.Action;
import cn.portal.esb.coproc.service.ConfigHandler;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class ApiNgxConfigHandler extends ConfigHandler<ApiModel> {

	private static final Logger log = LoggerFactory
			.getLogger(ApiNgxConfigHandler.class);
	@Autowired
	private NgxCache ngx;

	@Override
	public boolean canHandle(Action action, Class<?> clazz) {
		return ApiModel.class.equals(clazz);
	}

	@Override
	public void create(ApiModel current) {
		log.info("create api [{}_{}] config on nginx", current.getName(),
				current.getVersion());
		String key = "esb_api_" + current.getGroup().getSystem().getName()
				+ "_" + current.getGroup().getName() + "_" + current.getName();
		String value = MapperUtils.toJson(ApiNgxModel.toNgx(current));
		ngx.set(key + "_" + current.getVersion(), value);
		if (current.isDefaultVersion())
			ngx.set(key, value);
	}

	@Override
	public void drop(ApiModel previous) {
		log.info("drop api [{}_{}] config on nginx", previous.getName(),
				previous.getVersion());
		String key = "esb_api_" + previous.getGroup().getSystem().getName()
				+ "_" + previous.getGroup().getName() + "_"
				+ previous.getName();
		ngx.del(key + "_" + previous.getVersion());
		if (previous.isDefaultVersion())
			ngx.del(key);
	}

	@Override
	public void alter(ApiModel previous, ApiModel current) {
		log.info("alter api [{}_{}] config on nginx", current.getName(),
				current.getVersion());
		if (!previous.equals(current)
				|| !previous.getGroup().equals(current.getGroup())
				|| !previous.getGroup().getSystem()
						.equals(current.getGroup().getSystem())) {
			String key1 = "esb_api_"
					+ previous.getGroup().getSystem().getName() + "_"
					+ previous.getGroup().getName() + "_" + previous.getName();
			ngx.del(key1 + "_" + previous.getVersion());
			if (previous.isDefaultVersion())
				ngx.del(key1);
		}
		String key2 = "esb_api_" + current.getGroup().getSystem().getName()
				+ "_" + current.getGroup().getName() + "_" + current.getName();
		String value = MapperUtils.toJson(ApiNgxModel.toNgx(current));
		ngx.set(key2 + "_" + current.getVersion(), value);
		if (current.isDefaultVersion())
			ngx.set(key2, value);
	}

}
