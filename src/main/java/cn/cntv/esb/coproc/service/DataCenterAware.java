package cn.portal.esb.coproc.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cn.portal.esb.coproc.mapper.main.DataCenterDbMapper;
import cn.portal.esb.coproc.model.DataCenterModel;

@Service
public class DataCenterAware {

	private static final Logger log = LoggerFactory
			.getLogger(DataCenterAware.class);
	@Autowired
	private DataCenterDbMapper dataCenterDbMapper;
	@Value("${instance.name}")
	private String endpoint;
	private DataCenterModel dataCenter;
	private String backupEndpoint;

	@PostConstruct
	public void init() {
		int idx = endpoint.indexOf(":");
		String host = endpoint.substring(0, idx);
		int port = Integer.parseInt(endpoint.substring(idx + 1));
		dataCenter = dataCenterDbMapper.findByCoprocessorEndpoint(host, port);
		Assert.notNull(dataCenter,
				"endpoint configuration not found in database");
		log.info("endpoint {}:{} belong to datacenter {}: {}", host, port,
				dataCenter.getId(), dataCenter.getName());
		backupEndpoint = dataCenterDbMapper.findBackupEndpoint();
		Assert.notNull(backupEndpoint,
				"backup endpoint configuration not found in database");
		log.info("backup endpoint {}, {}", backupEndpoint,
				endpoint.equals(backupEndpoint) ? "local" : "remote");
	}

	public long id() {
		return dataCenter.getId();
	}

	public long coprocessorID() {
		return dataCenter.getCoprocessorID();
	}

	public String backupEndpoint() {
		return backupEndpoint;
	}

	public boolean isLocalBackup() {
		return endpoint.equals(backupEndpoint);
	}

}
