package cn.portal.esb.coproc.service.bak;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.BackupModel;
import cn.portal.esb.coproc.util.MapperUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@Service
public class BackupServiceRemote extends BackupService {

	@Value("#{dataCenterAware.backupEndpoint()}")
	private String endpoint;
	private Client client;
	private WebResource backup;
	private WebResource delete;
	private WebResource checkpoint;
	private WebResource restore;
	private WebResource timeMachine;

	@PostConstruct
	public void init() {
		client = Client.create();
		client.setConnectTimeout(3000);
		backup = client.resource("http://" + endpoint + "/admin/backup");
		delete = client.resource("http://" + endpoint + "/admin/delete");
		checkpoint = client
				.resource("http://" + endpoint + "/admin/checkpoint");
		restore = client.resource("http://" + endpoint + "/admin/restore");
		timeMachine = client.resource("http://" + endpoint
				+ "/admin/time_machine");
	}

	@PreDestroy
	public void destroy() {
		client.destroy();
	}

	@Override
	public BackupModel backup() {
		String json = backup.post(String.class).trim();
		try {
			BackupModel result = MapperUtils.fromJson(json, BackupResult.class)
					.getBackup();
			log.info("execute remote command, create backup {}, elapsed {}s",
					result.getTimestamp(), result.getElapsed());
			return result;
		} catch (RuntimeException e) {
			Map<String, Object> result = MapperUtils.fromJson(json);
			if (!(boolean) result.get("ok")) {
				String message = (String) result.get("msg");
				throw new RuntimeException(message);
			}
			throw e;
		}
	}

	@Override
	public void delete(String timestamp) {
		String json = delete.path(timestamp).post(String.class).trim();
		Map<String, Object> result = MapperUtils.fromJson(json);
		if (!(boolean) result.get("ok")) {
			String message = (String) result.get("msg");
			throw new RuntimeException(message);
		}
		log.info("execute remote command, delete backup {}", timestamp);
	}

	@Override
	public BackupModel checkpoint(String timestamp) {
		String json = checkpoint.path(timestamp).post(String.class).trim();
		try {
			BackupModel result = MapperUtils.fromJson(json, BackupResult.class)
					.getBackup();
			log.info(
					"execute remote command, checkpoint backup {}, elapsed {}s",
					result.getTimestamp(), result.getElapsed());
			return result;
		} catch (RuntimeException e) {
			Map<String, Object> result = MapperUtils.fromJson(json);
			if (!(boolean) result.get("ok")) {
				String message = (String) result.get("msg");
				throw new RuntimeException(message);
			}
			throw e;
		}
	}

	@Override
	public Map<String, Object> restore(String timestamp) {
		String json = restore.path(timestamp).post(String.class).trim();
		Map<String, Object> result = MapperUtils.fromJson(json);
		if (!(boolean) result.get("ok")) {
			String message = (String) result.get("msg");
			throw new RuntimeException(message);
		}
		log.info("execute remote command, restore to backup {}, elapsed {}s",
				timestamp, result.get("elapsed"));
		return result;
	}

	@Override
	public Map<String, Object> timeMachine(String timestamp) {
		String json = timeMachine.path(timestamp).post(String.class).trim();
		Map<String, Object> result = MapperUtils.fromJson(json);
		if (!(boolean) result.get("ok")) {
			String message = (String) result.get("msg");
			throw new RuntimeException(message);
		}
		log.info(
				"execute remote command, restore to backup {}, replay to {}, elapsed {}s",
				result.get("backup"), timestamp, result.get("elapsed"));
		return result;
	}

	public static class BackupResult {

		private boolean ok;
		private BackupModel backup;

		public boolean isOk() {
			return ok;
		}

		public void setOk(boolean ok) {
			this.ok = ok;
		}

		public BackupModel getBackup() {
			return backup;
		}

		public void setBackup(BackupModel backup) {
			this.backup = backup;
		}

	}

}
