package cn.portal.esb.coproc.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import cn.portal.esb.coproc.ssh.LogOperate;

@Controller
@RequestMapping("log")
public class LogController extends ControllerSupport {

	@Autowired
	private LogOperate logoperate;

	@RequestMapping("del/{day}/{filename:.+}")
	public View del(@PathVariable String day, @PathVariable String filename,
			Model model) {

		logoperate.DelLogFile(day, filename);
		model.addAttribute("ok", true);
		return view;
	}

	@RequestMapping("tar/{day}")
	public View tar(@PathVariable String day, Model model) {

		logoperate.TarLogFile(day);
		model.addAttribute("ok", true);
		return view;
	}

	@RequestMapping("test")
	public View tar(Model model) {

		model.addAttribute("ok", true);
		return view;
	}
}
