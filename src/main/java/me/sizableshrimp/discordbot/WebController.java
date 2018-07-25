package me.sizableshrimp.discordbot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
	
	@RequestMapping("/")
	@ResponseBody
	public String home() {
		return "The XT Discord Bot is currently running.";
	}
}
