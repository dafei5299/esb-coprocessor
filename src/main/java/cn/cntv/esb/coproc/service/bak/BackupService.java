package cn.portal.esb.coproc.service.bak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.portal.esb.coproc.model.BackupModel;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ZkClient;

public abstract class BackupService {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired
	protected ZkClient zk;

	@PostConstruct
	public void init() {
		zk.ensurePresence("/control/backups");
	}

	public List<BackupModel> timeline() {
		List<BackupModel> backups = new ArrayList<>();
		for (String data : zk.children("/control/backups"))
			backups.add(MapperUtils.fromJson(data, BackupModel.class));
		Collections.sort(backups);
		return backups;
	}

	public abstract BackupModel backup();

	public abstract void delete(String timestamp);

	public abstract BackupModel checkpoint(String timestamp);

	public abstract Map<String, Object> restore(String timestamp);

	public abstract Map<String, Object> timeMachine(String timestamp);

}
