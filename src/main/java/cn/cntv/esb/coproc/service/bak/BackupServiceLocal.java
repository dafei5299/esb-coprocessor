package cn.portal.esb.coproc.service.bak;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.model.BackupModel;
import cn.portal.esb.coproc.util.MapperUtils;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

@Service
public class BackupServiceLocal extends BackupService {

	@Value("${backup.script.path}")
	private String scriptPath;
	@Value("${backup.cluster}")
	private boolean cluster;

	@Override
	public BackupModel backup() {
		long start = System.currentTimeMillis();
		List<String> result = process(scriptPath, "create");
		int elapsed = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));

		Map<String, Object> map = parseResult(result);
		map.put("elapsed", elapsed);
		BackupModel backup = BackupModel.fromMap(map);
		log.info("execute local command, create {} backup {}, elapsed {}s",
				backup.getType(), backup.getTimestamp(), backup.getElapsed());
		zk.save("/control/backups/" + backup.getTimestamp(),
				MapperUtils.toJson(backup));
		return backup;
	}

	@Override
	public void delete(String timestamp) {
		List<String> result = process(scriptPath, "delete", timestamp);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));
		log.info("execute local command, delete backup {}", timestamp);
		zk.remove("/control/backups/" + timestamp);
	}

	@Override
	public BackupModel checkpoint(String timestamp) {
		List<BackupModel> backups = timeline();
		int target = -1;
		for (int i = backups.size() - 1; i >= 0; i--) {
			if (backups.get(i).getTimestamp().equals(timestamp)) {
				target = i;
				break;
			}
		}
		if (target == -1)
			throw new RuntimeException("backup not found: " + timestamp);
		List<BackupModel> steps = new ArrayList<>();
		steps.add(backups.get(target));
		long lsn = backups.get(target).getFromLsn();
		for (int i = target - 1; lsn != 0 && i >= 0; i--) {
			if (backups.get(i).getToLsn() == lsn) {
				steps.add(backups.get(i));
				lsn = backups.get(i).getFromLsn();
			}
		}
		if (lsn != 0)
			throw new RuntimeException("no base backup available");
		if (steps.size() == 1)
			return steps.get(0);
		Collections.reverse(steps);
		String baseTimestamp = steps.get(0).getTimestamp();
		StringBuilder incrTimestamps = new StringBuilder();
		for (int i = 1; i < steps.size(); i++) {
			incrTimestamps.append(steps.get(i).getTimestamp());
			if (i < steps.size() - 1)
				incrTimestamps.append(",");
		}
		log.info("make backup checkpoint, base: {}, incrs: {}", baseTimestamp,
				incrTimestamps);

		long start = System.currentTimeMillis();
		List<String> result = process(scriptPath, "checkpoint", baseTimestamp,
				incrTimestamps.toString());
		int elapsed = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));

		Map<String, Object> map = parseResult(result);
		map.put("elapsed", elapsed);
		BackupModel backup = BackupModel.fromMap(map);
		log.info("execute local command, checkpoint {} backup {}, elapsed {}s",
				backup.getType(), backup.getTimestamp(), backup.getElapsed());
		zk.save("/control/backups/" + backup.getTimestamp(),
				MapperUtils.toJson(backup));
		return backup;
	}

	@Override
	public Map<String, Object> restore(String timestamp) {
		Map<String, Object> map = new LinkedHashMap<>();
		// 先准备好备份还原点
		BackupModel backup1 = checkpoint(timestamp);
		map.put("backup1", backup1);
		// 执行还原过程
		long start = System.currentTimeMillis();
		List<String> result = process(scriptPath, "restore", "primary",
				backup1.getTimestamp());
		int elapsed1 = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));
		log.info(
				"execute local command, restore primary to backup {}, elapsed {}s",
				backup1.getTimestamp(), elapsed1);
		Map<String, Object> map1 = parseResult(result);
		map1.put("elapsed", elapsed1);
		map.put("restore1", map1);
		// 记录最近一次恢复的时间戳
		zk.save("/control/restore", String.valueOf(new Date().getTime()));
		if (!cluster) {
			map.put("elapsed", backup1.getElapsed() + elapsed1);
			map.put("ok", true);
			return map;
		}
		// 集群环境下还原另一节点
		// 先备份并准备，以获取binlog信息
		BackupModel backup2 = checkpoint(backup().getTimestamp());
		map.put("backup2", backup2);
		// 执行还原过程
		start = System.currentTimeMillis();
		result = process(scriptPath, "sync", backup2.getTimestamp());
		int elapsed2 = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));
		log.info(
				"execute local command, restore secondary to backup {}, elapsed {}s",
				backup2.getTimestamp(), elapsed2);
		Map<String, Object> map2 = parseResult(result);
		map2.put("elapsed", elapsed2);
		map.put("restore2", map2);
		map.put("elapsed",
				backup1.getElapsed() + elapsed1 + backup2.getElapsed()
						+ elapsed2);
		map.put("ok", true);
		return map;
	}

	@Override
	public Map<String, Object> timeMachine(String timestamp) {
		long time = DateTime.parse(timestamp,
				DateTimeFormat.forPattern("YYYY-MM-dd_HH-mm-ss")).getMillis();
		String restore = zk.data("/control/restore");
		Long lastRestore = null;
		if (restore != null) {
			lastRestore = Long.valueOf(restore);
			if (time <= lastRestore) {
				throw new RuntimeException("timestamp was before last restore "
						+ "applied, cannot use time machine, "
						+ "please use restore operation");
			}
		}
		List<BackupModel> backups = timeline();
		int i;
		for (i = backups.size() - 1; i >= 0; i--) {
			if (backups.get(i).isBefore(time)
					&& backups.get(i).isBinlogInfo()
					&& (lastRestore == null || backups.get(i).isAfter(
							lastRestore)))
				break;
		}
		if (i < 0)
			throw new RuntimeException("no binlog backup early than "
					+ timestamp);

		Map<String, Object> map = new LinkedHashMap<>();
		// 先准备好备份还原点
		BackupModel backup1 = checkpoint(backups.get(i).getTimestamp());
		map.put("backup1", backup1);
		// 执行还原过程
		long start = System.currentTimeMillis();
		List<String> result = process(scriptPath, "restore", "primary",
				backup1.getTimestamp(), timestamp);
		int elapsed1 = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));
		log.info(
				"execute local command, restore primary to backup {}, replay to {}, elapsed {}s",
				backup1.getTimestamp(), timestamp, elapsed1);
		Map<String, Object> map1 = parseResult(result);
		map1.put("elapsed", elapsed1);
		map.put("restore1", map1);
		// 记录最近一次恢复的时间戳
		zk.save("/control/restore", String.valueOf(new Date().getTime()));
		if (!cluster) {
			map.put("elapsed", backup1.getElapsed() + elapsed1);
			map.put("ok", true);
			return map;
		}
		// 集群环境下还原另一节点
		// 先备份并准备，以获取binlog信息
		BackupModel backup2 = checkpoint(backup().getTimestamp());
		map.put("backup2", backup2);
		// 执行还原过程
		start = System.currentTimeMillis();
		result = process(scriptPath, "sync", backup2.getTimestamp());
		int elapsed2 = (int) ((System.currentTimeMillis() - start) / 1000);
		if (result.size() == 0 || !"OK".equals(result.get(0)))
			throw new RuntimeException(Joiner.on("\n").join(result));
		log.info(
				"execute local command, restore secondary to backup {}, elapsed {}s",
				backup2.getTimestamp(), elapsed2);
		Map<String, Object> map2 = parseResult(result);
		map2.put("elapsed", elapsed2);
		map.put("restore2", map2);
		map.put("elapsed",
				backup1.getElapsed() + elapsed1 + backup2.getElapsed()
						+ elapsed2);
		map.put("ok", true);
		return map;
	}

	private synchronized List<String> process(String... command) {
		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(true)
					.start();
			Reader reader = new InputStreamReader(process.getInputStream());
			List<String> result = CharStreams.readLines(reader);
			process.waitFor();
			return result;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			process.destroy();
		}
	}

	private Map<String, Object> parseResult(List<String> result) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 1; i < result.size(); i++) {
			String line = result.get(i);
			int idx = line.indexOf(":");
			if (idx <= 1)
				continue;
			String key = line.substring(0, idx);
			String value = line.substring(idx + 1).trim();
			map.put(key, value);
		}
		return map;
	}

}
