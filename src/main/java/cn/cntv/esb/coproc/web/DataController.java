package cn.portal.esb.coproc.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import cn.portal.esb.coproc.model.NgxNodeModel;
import cn.portal.esb.coproc.model.SourceModel;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.service.NgxCacheLocalCluster;
import cn.portal.esb.coproc.util.MapperUtils;

import com.google.common.collect.ImmutableList;

@Controller
@RequestMapping("data")
public class DataController extends ControllerSupport {

	@Autowired
	private ConfigData configData;
	@Autowired
	private NgxCacheLocalCluster ngxsCluster;

	@RequestMapping("system")
	public View system(Model model) {
		List<?> systems = (List<?>) MapperUtils.toMap(configData.systems(),
				ImmutableList.of("groups.system", "groups.apis.group",
						"nodes.system"));
		model.addAttribute("ok", true).addAttribute("systems", systems)
				.addAttribute("count", systems.size());
		return view;
	}

	@RequestMapping("system/{id}")
	public View system(@PathVariable long id, Model model) {
		Object system = MapperUtils.toMap(configData.system(id), ImmutableList
				.of("groups.system", "groups.apis.group", "nodes.system"));
		model.addAttribute("ok", true).addAttribute("system", system);
		return view;
	}

	@RequestMapping("group")
	public View group(Model model) {
		List<?> groups = (List<?>) MapperUtils.toMap(configData.groups(),
				ImmutableList.of("system", "apis.group"));
		model.addAttribute("ok", true).addAttribute("groups", groups)
				.addAttribute("count", groups.size());
		return view;
	}

	@RequestMapping("group/{id}")
	public View group(@PathVariable long id, Model model) {
		Object group = MapperUtils.toMap(configData.group(id),
				ImmutableList.of("system", "apis.group"));
		model.addAttribute("ok", true).addAttribute("group", group);
		return view;
	}

	@RequestMapping("api")
	public View api(Model model) {
		List<?> apis = (List<?>) MapperUtils.toMap(configData.apis(),
				ImmutableList.of("group"));
		model.addAttribute("ok", true).addAttribute("apis", apis)
				.addAttribute("count", apis.size());
		return view;
	}

	@RequestMapping("api/{id}")
	public View api(@PathVariable long id, Model model) {
		Object api = MapperUtils.toMap(configData.api(id),
				ImmutableList.of("group"));
		model.addAttribute("ok", true).addAttribute("api", api);
		return view;
	}

	@RequestMapping("node")
	public View node(Model model) {
		List<?> nodes = (List<?>) MapperUtils.toMap(configData.nodes(),
				ImmutableList.of("system"));
		model.addAttribute("ok", true).addAttribute("nodes", nodes)
				.addAttribute("count", nodes.size());
		return view;
	}

	@RequestMapping("node/{id}")
	public View node(@PathVariable long id, Model model) {
		Object node = MapperUtils.toMap(configData.node(id),
				ImmutableList.of("system"));
		model.addAttribute("ok", true).addAttribute("node", node);
		return view;
	}

	@RequestMapping("source")
	public View source(Model model) {
		List<SourceModel> sources = configData.sources();
		model.addAttribute("ok", true).addAttribute("sources", sources)
				.addAttribute("count", sources.size());
		return view;
	}

	@RequestMapping("source/{id}")
	public View source(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("source",
				configData.source(id));
		return view;
	}

	@RequestMapping("ngx")
	public View ngx(Model model) {
		List<NgxNodeModel> ngxs = ngxsCluster.ngxs();
		model.addAttribute("ok", true).addAttribute("ngxs", ngxs)
				.addAttribute("count", ngxs.size());
		return view;
	}

	@RequestMapping("ngx/{id}")
	public View ngx(@PathVariable long id, Model model) {
		model.addAttribute("ok", true).addAttribute("ngx", ngxsCluster.ngx(id));
		return view;
	}

}
