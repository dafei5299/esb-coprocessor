package cn.portal.esb.coproc.service.ngx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.model.ngx.SystemNgxModel;
import cn.portal.esb.coproc.service.Action;
import cn.portal.esb.coproc.service.ConfigHandler;
import cn.portal.esb.coproc.service.NgxCacheLocalCluster;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class NodeNgxConfigHandler extends ConfigHandler<NodeModel> {

	private static final Logger log = LoggerFactory
			.getLogger(NodeNgxConfigHandler.class);
	@Autowired
	private NgxCacheLocalCluster ngx;

	@Override
	public boolean canHandle(Action action, Class<?> clazz) {
		return NodeModel.class.equals(clazz);
	}

	@Override
	public void create(NodeModel current) {
		log.info("create node [{}:{}] config on nginx", current.getHost(),
				current.getPort());
		String value = MapperUtils.toJson(SystemNgxModel.toNgx(
				current.getSystem(), ngx.concurrencyEstimate()));
		ngx.set("esb_system_" + current.getSystem().getName(), value);
	}

	@Override
	public void drop(NodeModel previous) {
		log.info("drop node [{}:{}] config on nginx", previous.getHost(),
				previous.getPort());
		String value = MapperUtils.toJson(SystemNgxModel.toNgx(
				previous.getSystem(), ngx.concurrencyEstimate()));
		ngx.set("esb_system_" + previous.getSystem().getName(), value);
	}

	@Override
	public void alter(NodeModel previous, NodeModel current) {
		log.info("alter node [{}:{}] config on nginx", current.getHost(),
				current.getPort());
		if (!previous.getSystem().equals(current.getSystem())) {
			String value = MapperUtils.toJson(SystemNgxModel.toNgx(
					previous.getSystem(), ngx.concurrencyEstimate()));
			ngx.set("esb_system_" + previous.getSystem().getName(), value);
		}
		String value = MapperUtils.toJson(SystemNgxModel.toNgx(
				current.getSystem(), ngx.concurrencyEstimate()));
		ngx.set("esb_system_" + current.getSystem().getName(), value);
	}

}
