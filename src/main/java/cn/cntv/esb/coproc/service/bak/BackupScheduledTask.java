package cn.portal.esb.coproc.service.bak;

import java.util.List;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import cn.portal.esb.coproc.model.BackupModel;

// @Service 功能已砍掉
public class BackupScheduledTask {

	@Value("#{dataCenterAware.isLocalBackup() ? backupServiceLocal : null }")
	private BackupServiceLocal backupService;
	@Value("${backup.clear.days}")
	private int days;

	@Scheduled(cron = "${backup.snapshot.cron}")
	public void backup() {
		if (backupService == null)
			return;
		backupService.backup();
	}

	@Scheduled(cron = "${backup.checkpoint.cron}")
	public void checkpoint() {
		if (backupService == null)
			return;
		long today = new DateTime().withTimeAtStartOfDay().getMillis();
		List<BackupModel> backups = backupService.timeline();
		BackupModel latest = null;
		for (int i = backups.size() - 1; i >= 0; i--) {
			if (backups.get(i).isAfter(today))
				latest = backups.get(i);
			else
				break;
		}
		if (latest == null)
			return;
		backupService.checkpoint(latest.getTimestamp());
	}

	@Scheduled(cron = "${backup.clear.cron}")
	public void clear() {
		if (backupService == null)
			return;
		long ago = new DateTime().minusDays(days).withTimeAtStartOfDay()
				.getMillis();
		List<BackupModel> backups = backupService.timeline();
		int expires = 0;
		for (int i = 0; i < backups.size(); i++, expires++)
			if (!backups.get(i).isBefore(ago))
				break;
		for (int i = expires; i < backups.size(); i++, expires++)
			if (backups.get(i).getFromLsn() == 0)
				break;
		if (backups.size() > expires)
			for (int i = 0; i < expires; i++)
				backupService.delete(backups.get(i).getTimestamp());
	}

}
