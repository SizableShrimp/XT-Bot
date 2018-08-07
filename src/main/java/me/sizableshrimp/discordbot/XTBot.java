package me.sizableshrimp.discordbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicEvents;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

@SpringBootApplication
public class XTBot {
	public static IDiscordClient client;
	public static String prefix = ",";
	public static long firstOnline;
	public static Music music;
	private static boolean isLive = false;
	private static long latestVideo;

	public static void main(String[] args) {
		SpringApplication.run(XTBot.class, args);
		client = BotClient.createClient(System.getenv("TOKEN"), true);
		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new EventListener());
		MusicEvents events = new MusicEvents();
		dispatcher.registerListener(events);
		music = events.music;
		firstOnline = System.currentTimeMillis();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				EventListener.sendMessage("Happy 420!", client.getChannelByID(332985255151665152L));
			}
		}, dailyMeme(), 24*60*60, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://botxt.herokuapp.com").openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					connection.getResponseCode();
					connection.disconnect();
				} catch (IOException e) {e.printStackTrace();}
			}
		}, 0, 5*60, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					//HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCKrMGLGMhxIuMHQdHOf1YIw&eventType=live&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCPI1R-pIs5vwiDFHS4ic40w&eventType=live&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						StringBuffer response = new StringBuffer();
						String inputLine;
						while ((inputLine = reader.readLine()) != null) response.append(inputLine);
						reader.close();
						connection.disconnect();
						JSONObject json = new JSONObject(response.toString());
						if (json.getJSONObject("pageInfo").getInt("totalResults") == 1) {
							JSONObject video = json.getJSONArray("items").getJSONObject(0);
							if (isLive == false) {
								//EventListener.sendMessage("@everyone **"+video.getJSONObject("snippet").getString("channelTitle")+"** is :red_circle:**LIVE**:red_circle:!\nhttps://www.youtube.com/watch?v="+video.getJSONObject("id").getString("videoId"), XTBot.client.getChannelByID(341028279584817163L));
								EventListener.sendMessage("@everyone **"+video.getJSONObject("snippet").getString("channelTitle")+"** is :red_circle:**LIVE**:red_circle:!\nhttps://www.youtube.com/watch?v="+video.getJSONObject("id").getString("videoId"), XTBot.client.getChannelByID(474641238390210562L));
								isLive = true;
							}
						} else {
							if (isLive == true) isLive = false;
						}
						return;
					}
					connection.disconnect();
					return;
				} catch (IOException | JSONException e) {e.printStackTrace();}
			}
		}, 0, 1*60, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCOWSGxYNYJEaUuqnh8Cpc-g&maxResults=1&order=date&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						StringBuffer response = new StringBuffer();
						String inputLine;
						while ((inputLine = reader.readLine()) != null) response.append(inputLine);
						reader.close();
						connection.disconnect();
						JSONObject json = new JSONObject(response.toString());
						if (json.getJSONObject("pageInfo").getInt("totalResults") >= 1) {
							JSONObject video = json.getJSONArray("items").getJSONObject(0);
							ZonedDateTime publishDate = Instant.parse(video.getJSONObject("snippet").getString("publishedAt")).atZone(ZoneId.of("US/Eastern"));
							if (publishDate.toInstant().toEpochMilli() > firstOnline && latestVideo != publishDate.toInstant().toEpochMilli()) {
								EventListener.sendMessage("@everyone **"+video.getJSONObject("snippet").getString("channelTitle")+"** uploaded **"+video.getJSONObject("snippet").getString("title")+"** on "+getTime(publishDate)+"\nhttps://www.youtube.com/watch?v="+video.getJSONObject("id").getString("videoId"), XTBot.client.getChannelByID(341028279584817163L));
								latestVideo = publishDate.toInstant().toEpochMilli();
							}
						}
						return;
					}
					connection.disconnect();
					return;
				} catch (IOException | JSONException e) {e.printStackTrace();}
			}
		}, 0, 3*60, TimeUnit.SECONDS);
	}

	private static long dailyMeme() {
		ZonedDateTime time = ZonedDateTime.now(ZoneId.of("US/Eastern"));
		ZonedDateTime tomorrow = time.withHour(16).withMinute(20).withSecond(0);
		if (time.compareTo(tomorrow) > 0) tomorrow = tomorrow.plusDays(1);
		return Duration.between(time, tomorrow).getSeconds();
	}
	
	private static String getTime(ZonedDateTime time) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE, MMMM d'"+getOrdinal(time.getDayOfMonth())+"', yyyy h:mm a '"+time.getZone().getDisplayName(TextStyle.FULL, Locale.US)+"'");
		return format.format(time);
	}

	private static String getOrdinal(int day) {
		switch (day) {
		case 1: case 21: case 31:
			return "st";
		case 2: case 22:
			return "nd";
		case 3: case 23:
			return "rd";
		default:
			return "th";
		}
	}
}
