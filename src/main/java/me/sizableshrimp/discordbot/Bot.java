package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Bot {
    public static final ObjectMapper mapper = new ObjectMapper();
    private static long firstOnline;
    private static final long GREETING_CHANNEL = 332985255151665152L;

    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        DiscordConfiguration.login();
    }

    /**
     * Schedules the main events that run on a timer
     *
     * @param client A client from any shard used to get channels by ID
     */
    public static void schedule(DiscordClient client) {
        YoutubeListener.schedule(client);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(() -> client.getChannelById(Snowflake.of(GREETING_CHANNEL))
                .ofType(TextChannel.class)
                .flatMap(c -> Util.sendMessage("Happy 420!", c))
                .subscribe(), Util.happy420().toMillis(), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        boolean heroku = System.getenv().containsKey("HEROKU");
        if (heroku && System.getenv("HEROKU").equalsIgnoreCase("true")) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(System.getenv("URL")).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    connection.getResponseCode();
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.MINUTES);
        }
    }

    public static long getFirstOnline() {
        return firstOnline;
    }

    static void setFirstOnline(long millis) {
        firstOnline = millis;
    }

    public static String getPrefix(DiscordClient client, Snowflake guildId) {
        //TODO add support for changing prefix later
        if (System.getenv().containsKey("PREFIX")) return System.getenv("PREFIX");
        return ",";
    }
}
