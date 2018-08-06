package me.sizableshrimp.discordbot;

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

	/*@RequestMapping(
			value = "/hook", 
			method = RequestMethod.POST,
			consumes = "application/json")
	public void videoHook(@RequestBody Map<String, String> payload, @RequestHeader(value="Verified") String verified) throws Exception {
		if (verified.equals(System.getenv("VERIFIED_KEY"))) {
			EventListener.newVideo(payload);
		}
	}*/
}