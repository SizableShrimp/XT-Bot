package me.sizableshrimp.discordbot;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

@SpringBootApplication
public class XTBot {
	public static IDiscordClient client;
	public static String prefix = "^";
	public static long firstOnline;

	public static void main(String[] args) {
		SpringApplication.run(XTBot.class, args);
		client = BotClient.createClient(System.getenv("TOKEN"), true);
		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new EventListener());
		firstOnline = System.currentTimeMillis();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				try {
					Runtime runtime = Runtime.getRuntime();
					runtime.exec("ping xt-bot42.herokuapp.com");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}, 0, (15 * 60 * 1000));
	}
}
