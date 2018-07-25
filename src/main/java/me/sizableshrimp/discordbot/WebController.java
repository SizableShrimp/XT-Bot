package me.sizableshrimp.discordbot;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {
	
	@RequestMapping("/")
	@ResponseBody
	public String home() {
		return "The XT Discord Bot is currently running.";
	}
	
	@RequestMapping("/stopidledyno.txt")
	public Resource stopidledyno() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext();
		Resource resource = ctx.getResource("classpath:me/sizableshrimp/discordbot/stopidledyno.txt");
		ctx.close();
		return resource;
	}
}
