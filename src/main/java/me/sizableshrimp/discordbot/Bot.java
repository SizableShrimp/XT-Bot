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
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import me.sizableshrimp.discordbot.music.MusicEvents;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;

@SpringBootApplication
public class Bot {
	public static IDiscordClient client;
	private final static HashMap<Long, String> prefixes = new HashMap<Long, String>();
	public static final long firstOnline = System.currentTimeMillis();
	private static boolean isLive = false;
	private static long latestVideo;
	private static String latestStreamId;

	public static void main(String[] args) {
		SpringApplication.run(Bot.class, args);
		client = createClient(System.getenv("TOKEN"), true);
		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new EventListener());
		dispatcher.registerListener(new MusicEvents());
		IMessage[] messages = client.getChannelByID(341028279584817163L).getMessageHistory(10).asArray();
		for (IMessage message : messages) {
			if (message.getContent().contains(latestStreamId)) return;
		}
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				EventListener.sendMessage("Happy 420!", client.getChannelByID(332985255151665152L));
			}
		}, dailyMeme(), 24*60*60, TimeUnit.SECONDS);
		if (System.getenv("HEROKU").equals("true")) {
			scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						HttpsURLConnection connection = (HttpsURLConnection) new URL(System.getenv("URL")).openConnection();
						connection.setRequestMethod("GET");
						connection.connect();
						connection.getResponseCode();
						connection.disconnect();
					} catch (IOException e) {e.printStackTrace();}
				}
			}, 0, 5*60, TimeUnit.SECONDS);
		}
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCKrMGLGMhxIuMHQdHOf1YIw&eventType=live&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
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
						if (json.getJSONObject("pageInfo").getInt("totalResults") == 0 && isLive) {
							isLive = false;
							return;
						} else if (json.getJSONObject("pageInfo").getInt("totalResults") == 1 && !isLive) {
							JSONObject stream = json.getJSONArray("items").getJSONObject(0);
							for (IMessage message : client.getChannelByID(341028279584817163L).getMessageHistory(10).asArray()) {
								if (message.getContent().contains(stream.getJSONObject("id").getString("videoId"))) {
									isLive = true;
									return;
								}
							}
							EventListener.sendMessage("@everyone **"+stream.getJSONObject("snippet").getString("channelTitle")+"** is :red_circle:**LIVE**:red_circle:!\nhttps://www.youtube.com/watch?v="+stream.getJSONObject("id").getString("videoId"), client.getChannelByID(341028279584817163L));
							latestStreamId = stream.getJSONObject("id").getString("videoId");
							isLive = true;
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
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCKrMGLGMhxIuMHQdHOf1YIw&maxResults=1&order=date&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
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
							if (latestStreamId.equals(video.getJSONObject("id").getString("videoId"))) return;
							ZonedDateTime publishDate = Instant.parse(video.getJSONObject("snippet").getString("publishedAt")).atZone(ZoneId.of("US/Eastern"));
							if (publishDate.toInstant().toEpochMilli() > firstOnline && latestVideo != publishDate.toInstant().toEpochMilli()) {
								EventListener.sendMessage("@everyone **"+video.getJSONObject("snippet").getString("channelTitle")+"** uploaded **"+video.getJSONObject("snippet").getString("title")+"** on "+getTime(publishDate)+"\nhttps://www.youtube.com/watch?v="+video.getJSONObject("id").getString("videoId"), client.getChannelByID(341028279584817163L));
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
		String ordinal;
		switch (time.getDayOfMonth()) {
		case 1: case 21: case 31:
			ordinal = "st";
			break;
		case 2: case 22:
			ordinal = "nd";
			break;
		case 3: case 23:
			ordinal = "rd";
			break;
		default:
			ordinal = "th";
		}
		DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE, MMMM d'"+ordinal+"', yyyy h:mm a '"+time.getZone().getDisplayName(TextStyle.FULL, Locale.US)+"'");
		return format.format(time);
	}

	private static IDiscordClient createClient(String token, boolean login) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		try {
			return login ? clientBuilder.login() : clientBuilder.build();
		} catch (DiscordException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getPrefix(IGuild guild) {
		return (prefixes.get(guild.getLongID()));
	}

	public static void setPrefix(IGuild guild, String prefix) {
		prefixes.put(guild.getLongID(), prefix);
	}
}
