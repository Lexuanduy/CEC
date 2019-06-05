package fcs.cec.opencec.server;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	@GetMapping(value = "/")
	public String welcome(Model model) throws InterruptedException, ExecutionException {
		return "views/dashboard";
	}

	@GetMapping(value = "/support")
	public String support(Model model) throws InterruptedException, ExecutionException {
		return "support/support";
	}

	@GetMapping(value = "/volunteer")
	public String volunteer(Model model) throws InterruptedException, ExecutionException {
		return "volunteer/volunteer";
	}
	
	@GetMapping(value = "/privacy")
	public String privacy() throws InterruptedException, ExecutionException {
		return "privacy/policy";
	}
}
