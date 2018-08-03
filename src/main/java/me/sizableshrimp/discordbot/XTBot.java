package me.sizableshrimp.discordbot;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicEvents;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.MessageBuilder;

@SpringBootApplication
public class XTBot {
	public static IDiscordClient client;
	public static String prefix = ",";
	public static long firstOnline;
	public static Music music;

	public static void main(String[] args) {
		SpringApplication.run(XTBot.class, args);
		client = BotClient.createClient(System.getenv("TOKEN"), true);
		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new EventListener());
		MusicEvents events = new MusicEvents();
		dispatcher.registerListener(events);
		music = events.music;
		firstOnline = System.currentTimeMillis();
		dailyMeme();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					URL siteURL = new URL("https://botxt.herokuapp.com");
					HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					System.out.println("Heroku dyno idling refreshed. Response code: "+Integer.toString(connection.getResponseCode()));
					connection.disconnect();
				} catch (IOException e) {e.printStackTrace();}
			}
		}, 0, 5*60, TimeUnit.SECONDS);
	}
	
	private static void dailyMeme() {
		ZonedDateTime time = ZonedDateTime.now(ZoneId.of("US/Eastern"));
		ZonedDateTime tomorrow;
		tomorrow = time.withHour(16).withMinute(20).withSecond(0);
		if (time.compareTo(tomorrow) > 0) tomorrow = tomorrow.plusDays(1);
		Duration duration = Duration.between(time, tomorrow);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				new MessageBuilder(XTBot.client).appendContent("\u200B"+"Happy 420!").withChannel(client.getChannelByID(332985255151665152L)).build();
			}
		}, duration.getSeconds(), 24*60*60, TimeUnit.SECONDS);
	}
}
