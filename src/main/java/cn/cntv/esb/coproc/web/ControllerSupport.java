package cn.portal.esb.coproc.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

public abstract class ControllerSupport {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	protected View view;

	@ExceptionHandler
	public ModelAndView exception(Exception e) {
		logger.warn("unhandled exception", e);
		return new ModelAndView(view).addObject("ok", false).addObject("msg",
				e.getClass().getName() + ": " + e.getMessage());
	}

}
