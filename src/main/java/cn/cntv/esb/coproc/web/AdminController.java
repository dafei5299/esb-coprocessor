package cn.portal.esb.coproc.web;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import cn.portal.esb.coproc.model.BackupModel;
import cn.portal.esb.coproc.redis.RedisCluster;
import cn.portal.esb.coproc.service.bak.BackupService;
import cn.portal.esb.coproc.service.stat.MergeDbTask;
import cn.portal.esb.coproc.zk.ZkElection;

@Controller
@RequestMapping("admin")
public class AdminController extends ControllerSupport {

	@Autowired
	private ZkElection election;
	@Autowired
	private RedisCluster redis;
	@Value("#{dataCenterAware.isLocalBackup() ? backupServiceLocal : backupServiceRemote }")
	private BackupService backupService;
	@Autowired
	private MergeDbTask mergeDbTask;
	@Autowired
	private TaskExecutor executor;

	@RequestMapping("state")
	public View state(Model model) {
		model.addAttribute("ok", true).addAttribute("state", election.state())
				.addAttribute("path", election.path());
		return view;
	}

	@RequestMapping("redis")
	public View redis(Model model) {
		model.addAttribute("ok", true).addAttribute("shards", redis.shards())
				.addAttribute("replicas", redis.replicas());
		return view;
	}

	@RequestMapping("merge/{day}")
	public View merge(@DateTimeFormat(iso = ISO.DATE) @PathVariable Date day,
			Model model) {
		mergeDbTask.merge(new DateTime(day));
		model.addAttribute("ok", true);
		return view;
	}

	@RequestMapping("timeline")
	public View timeline(
			@RequestParam(defaultValue = "false") boolean ascending, Model model) {
		List<BackupModel> backups = backupService.timeline();
		if (!ascending)
			Collections.reverse(backups);
		model.addAttribute("ok", true).addAttribute("backups", backups)
				.addAttribute("count", backups.size());
		return view;
	}

	@RequestMapping("backup")
	public View backup(Model model) {
		model.addAttribute("ok", true).addAttribute("backup",
				backupService.backup());
		return view;
	}

	@RequestMapping("async/backup")
	public View async_backup(Model model) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				backupService.backup();
			}
		});
		model.addAttribute("ok", true).addAttribute("msg", "task submitted");
		return view;
	}

	@RequestMapping("delete/{timestamp}")
	public View delete(@PathVariable String timestamp, Model model) {
		backupService.delete(timestamp);
		model.addAttribute("ok", true);
		return view;
	}

	@RequestMapping("checkpoint/{timestamp}")
	public View checkpoint(@PathVariable String timestamp, Model model) {
		model.addAttribute("ok", true).addAttribute("backup",
				backupService.checkpoint(timestamp));
		return view;
	}

	@RequestMapping("async/checkpoint/{timestamp}")
	public View async_checkpoint(@PathVariable final String timestamp,
			Model model) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				backupService.checkpoint(timestamp);
			}
		});
		model.addAttribute("ok", true).addAttribute("msg", "task submitted");
		return view;
	}

	@RequestMapping("restore/{timestamp}")
	public View restore(@PathVariable String timestamp, Model model) {
		model.addAllAttributes(backupService.restore(timestamp));
		return view;
	}

	@RequestMapping("async/restore/{timestamp}")
	public View async_restore(@PathVariable final String timestamp, Model model) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				backupService.restore(timestamp);
			}
		});
		model.addAttribute("ok", true).addAttribute("msg", "task submitted");
		return view;
	}

	@RequestMapping("time_machine/{timestamp}")
	public View time_machine(@PathVariable String timestamp, Model model) {
		model.addAllAttributes(backupService.timeMachine(timestamp));
		return view;
	}

	@RequestMapping("async/time_machine/{timestamp}")
	public View async_time_machine(@PathVariable final String timestamp,
			Model model) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				backupService.timeMachine(timestamp);
			}
		});
		model.addAttribute("ok", true).addAttribute("msg", "task submitted");
		return view;
	}

}
