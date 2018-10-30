package me.sizableshrimp.discordbot;

import me.sizableshrimp.discordbot.music.MusicEvents;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import javax.net.ssl.HttpsURLConnection;
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

@SpringBootApplication
public class Bot {
	public static IDiscordClient client;
	private final static HashMap<Long, String> prefixes = new HashMap<>();
	static final long firstOnline = System.currentTimeMillis();
	private static boolean isLive = false;
	private static long latestVideo;

    //test
    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        client = new ClientBuilder().withToken(System.getenv("TOKEN")).login();
        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(new EventListener());
        dispatcher.registerListener(new MusicEvents());
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduler.scheduleAtFixedRate(() -> EventListener.sendMessage("Happy 420!", client.getChannelByID(332985255151665152L)), dailyMeme(), 24*60*60, TimeUnit.SECONDS);
        boolean heroku = false;
        try {
            String s = System.getenv("HEROKU");
            if (s != null) heroku = true;
        } catch (NullPointerException ignored) {}
        if (heroku) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(System.getenv("URL")).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    connection.getResponseCode();
                    connection.disconnect();
                } catch (IOException e) {e.printStackTrace();}
            }, 0, 10*60, TimeUnit.SECONDS);
        }
        scheduler.scheduleAtFixedRate(() -> {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCKrMGLGMhxIuMHQdHOf1YIw&eventType=live&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
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
                        isLive = true;
                    }
                    return;
                }
                connection.disconnect();
            } catch (IOException | JSONException e) {e.printStackTrace();}
        }, 0, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=UCKrMGLGMhxIuMHQdHOf1YIw&maxResults=1&order=date&type=video&key="+System.getenv("GOOGLE_KEY")).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = reader.readLine()) != null) response.append(inputLine);
                    reader.close();
                    connection.disconnect();
                    JSONObject json = new JSONObject(response.toString());
                    if (json.getJSONObject("pageInfo").getInt("totalResults") >= 1) {
                        JSONObject video = json.getJSONArray("items").getJSONObject(0);
                        ZonedDateTime publishDate = Instant.parse(video.getJSONObject("snippet").getString("publishedAt")).atZone(ZoneId.of("US/Eastern"));
                        if (publishDate.toInstant().toEpochMilli() > firstOnline && latestVideo != publishDate.toInstant().toEpochMilli()) {
                            for (IMessage message : client.getChannelByID(341028279584817163L).getMessageHistory(10).asArray()) {
                                if (message.getContent().contains(video.getJSONObject("id").getString("videoId"))) {
                                    return;
                                }
                            }
                            EventListener.sendMessage("@everyone **"+video.getJSONObject("snippet").getString("channelTitle")+"** uploaded **"+video.getJSONObject("snippet").getString("title")+"** on "+getTime(publishDate)+"\nhttps://www.youtube.com/watch?v="+video.getJSONObject("id").getString("videoId"), client.getChannelByID(341028279584817163L));
                            latestVideo = publishDate.toInstant().toEpochMilli();
                        }
                    }
                    return;
                }
                connection.disconnect();
            } catch (IOException | JSONException e) {e.printStackTrace();}
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

	public static String getPrefix(IGuild guild) {
		return prefixes.get(guild.getLongID());
	}

	static void setPrefix(IGuild guild, String prefix) {
		prefixes.put(guild.getLongID(), prefix);
		//updateGuild(guild.getLongID(), prefix);
	}
	
	static void removePrefix(IGuild guild) {
		prefixes.remove(guild.getLongID());
		//removeGuild(guild.getLongID());
	}
	
//	public static void insertGuild(Long id, String prefix) {
//		String url = System.getenv("SQL_URL");
//		String user = System.getenv("SQL_USER");
//		String password = System.getenv("SQL_PASSWORD");
//		try {
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			Connection con = DriverManager.getConnection(url, user, password);
//			PreparedStatement pst = con.prepareStatement("INSERT INTO prefixes(guild_id, prefix) VALUES(?, ?)");
//			pst.setLong(1, id);
//			pst.setString(2, prefix);
//			pst.executeUpdate();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public static void updateGuild(Long id, String prefix) {
//		String url = System.getenv("SQL_URL");
//		String user = System.getenv("SQL_USER");
//		String password = System.getenv("SQL_PASSWORD");
//		try {
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			Connection con = DriverManager.getConnection(url, user, password);
//			PreparedStatement pst = con.prepareStatement("UPDATE prefixes SET prefix = ? WHERE guild_id = ?");
//			pst.setString(1, prefix);
//			pst.setLong(2, id);
//			pst.executeUpdate();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public static void removeGuild(Long id) {
//		String url = System.getenv("SQL_URL");
//		String user = System.getenv("SQL_USER");
//		String password = System.getenv("SQL_PASSWORD");
//		try {
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			Connection con = DriverManager.getConnection(url, user, password);
//			PreparedStatement pst = con.prepareStatement("DELETE FROM prefixes WHERE guild_id=?");
//			pst.setLong(1, id);
//			pst.executeUpdate();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public static String retrieveSQLPrefix(Long id) {
//		String url = System.getenv("SQL_URL");
//		String user = System.getenv("SQL_USER");
//		String password = System.getenv("SQL_PASSWORD");
//		try {
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			Connection con = DriverManager.getConnection(url, user, password);
//			PreparedStatement pst = con.prepareStatement("SELECT * FROM prefixes WHERE guild_id=?");
//			pst.setLong(1, id);
//			ResultSet set = pst.executeQuery();
//			if (set.next()) return set.getString(2);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
}
