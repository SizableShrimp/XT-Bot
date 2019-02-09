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
}