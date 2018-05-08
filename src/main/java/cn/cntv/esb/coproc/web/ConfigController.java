package cn.portal.esb.coproc.web;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import cn.portal.esb.coproc.mapper.main.ApiDbMapper;
import cn.portal.esb.coproc.mapper.main.GroupDbMapper;
import cn.portal.esb.coproc.mapper.main.NgxNodeDbMapper;
import cn.portal.esb.coproc.mapper.main.NodeDbMapper;
import cn.portal.esb.coproc.mapper.main.NodeLatencyDbMapper;
import cn.portal.esb.coproc.mapper.main.SourceDbMapper;
import cn.portal.esb.coproc.mapper.main.SystemDbMapper;
import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.GroupModel;
import cn.portal.esb.coproc.model.NgxNodeModel;
import cn.portal.esb.coproc.model.NodeModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.model.SystemModel;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.ConfigService;
import cn.portal.esb.coproc.service.NgxCacheLocalCluster;

import com.google.common.collect.Sets;

@Controller
@RequestMapping("config")
public class ConfigController extends ControllerSupport {

	@Autowired
	private ConfigService configService;
	@Autowired
	private ConfigData configData;
	@Autowired
	private NgxCacheLocalCluster ngxs;
	@Autowired
	private SystemDbMapper systemMapper;
	@Autowired
	private GroupDbMapper groupMapper;
	@Autowired
	private ApiDbMapper apiMapper;
	@Autowired
	private NodeDbMapper nodeMapper;
	@Autowired
	private NodeLatencyDbMapper latencyMapper;
	@Autowired
	private SourceDbMapper sourceMapper;
	@Autowired
	private NgxNodeDbMapper ngxMapper;

	@RequestMapping("system/{id}")
	public View system(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.system(id));
		return view;
	}

	@RequestMapping("group/{id}")
	public View group(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.group(id));
		return view;
	}

	@RequestMapping("api/{id}")
	public View api(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.api(id));
		return view;
	}

	@RequestMapping("node/{id}")
	public View node(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.node(id));
		return view;
	}

	@RequestMapping("latency/{id}")
	public View latency(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.latency(id));
		return view;
	}

	@RequestMapping("source/{id}")
	public View source(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.source(id));
		return view;
	}

	@RequestMapping("ngx/{id}")
	public View ngx(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("action",
				configService.ngx(id));
		return view;
	}

	@RequestMapping("sync")
	public View sync(Model model) {
		configService.sync();
		model.addAttribute("ok", true);
		return view;
	}

	@RequestMapping("fsync")
	public View fsync(Model model) {
		Set<Long> sysid = new HashSet<>();
		for (SystemModel system : configData.systems()) {
			sysid.add(system.getId());
			configService.system(system.getId());
		}
		for (long id : Sets.difference(systemMapper.findIDs(), sysid))
			configService.system(id);

		Set<Long> grpid = new HashSet<>();
		for (GroupModel group : configData.groups()) {
			grpid.add(group.getId());
			configService.group(group.getId());
		}
		for (long id : Sets.difference(groupMapper.findIDs(), grpid))
			configService.group(id);

		Set<Long> apid = new HashSet<>();
		for (ApiModel api : configData.apis()) {
			apid.add(api.getId());
			configService.api(api.getId());
		}
		for (long id : Sets.difference(apiMapper.findIDs(), apid))
			configService.api(id);

		Set<Long> ndid = new HashSet<>();
		for (NodeModel node : configData.nodes()) {
			ndid.add(node.getId());
			configService.node(node.getId());
		}
		for (long id : Sets.difference(nodeMapper.findIDs(), ndid))
			configService.node(id);

		for (long id : latencyMapper.findIDs())
			configService.latency(id);

		Set<Long> srcid = new HashSet<>();
		for (SourceModel source : configData.sources()) {
			srcid.add(source.getId());
			configService.source(source.getId());
		}
		for (long id : Sets.difference(sourceMapper.findIDs(), srcid))
			configService.source(id);

		Set<Long> ngxid = new HashSet<>();
		for (NgxNodeModel ngx : ngxs.ngxs()) {
			ngxid.add(ngx.getId());
			configService.ngx(ngx.getId());
		}
		for (long id : Sets.difference(ngxMapper.findIDs(), ngxid))
			configService.ngx(id);

		model.addAttribute("ok", true);
		return view;
	}

}
