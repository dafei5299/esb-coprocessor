package cn.portal.esb.coproc.service.ngx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.ngx.SourceNgxModel;
import cn.portal.esb.coproc.ngx.NgxCache;
import cn.portal.esb.coproc.service.Action;
import cn.portal.esb.coproc.service.ConfigHandler;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class SourceNgxConfigHandler extends ConfigHandler<SourceModel> {

	private static final Logger log = LoggerFactory
			.getLogger(SourceNgxConfigHandler.class);
	@Autowired
	private NgxCache ngx;

	@Override
	public boolean canHandle(Action action, Class<?> clazz) {
		return SourceModel.class.equals(clazz);
	}

	@Override
	public void create(SourceModel current) {
		log.info("create source [{}] config on nginx", current.getName());
		String value = MapperUtils.toJson(SourceNgxModel.toNgx(current));
		ngx.set("esb_app_clients_" + current.getMd5(), value);
	}

	@Override
	public void drop(SourceModel previous) {
		log.info("drop source [{}] config on nginx", previous.getName());
		ngx.del("esb_app_clients_" + previous.getMd5());
	}

	@Override
	public void alter(SourceModel previous, SourceModel current) {
		log.info("alter source [{}] config on nginx", current.getName());
		if (!previous.equals(current))
			ngx.del("esb_app_clients_" + previous.getMd5());
		String value = MapperUtils.toJson(SourceNgxModel.toNgx(current));
		ngx.set("esb_app_clients_" + current.getMd5(), value);
	}

}
