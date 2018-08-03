package me.sizableshrimp.discordbot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {

	@RequestMapping("/")
	@ResponseBody
	public String home() {
		return "The XT Discord Bot is currently running.";
	}

	@RequestMapping(
			value = "/hook", 
			method = RequestMethod.POST,
			consumes = "text/plain")
	public void videoHook(@RequestBody String payload) throws Exception {
		EventListener.newVideo(payload);
	}
}
