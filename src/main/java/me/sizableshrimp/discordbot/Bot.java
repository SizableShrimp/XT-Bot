package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        DiscordConfiguration.login().subscribe();
    }

    /**
     * Schedules the main events that run on a timer
     *
     * @param client A client from any shard used to get channels by ID
     */
    public static void schedule(DiscordClient client) {
        YoutubeListener.schedule(client);

        Mono<Message> message420 = client.getChannelById(Snowflake.of(GREETING_CHANNEL))
                .ofType(TextChannel.class)
                .flatMap(Bot::create420Message);
        Flux.interval(Util.happy420(), Duration.ofDays(1))
                .flatMap(l -> message420)
                .subscribe();

        boolean heroku = System.getenv().containsKey("HEROKU");

        if (heroku && System.getenv("HEROKU").equalsIgnoreCase("true")) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
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

    private static Mono<Message> create420Message(MessageChannel channel) {
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        boolean ultimate420 = time.getMonth() == Month.APRIL && time.getDayOfMonth() == 20;
        String message = ultimate420 ? "@everyone Happy ULTIMATE 420!!!" : "Happy 420!";
        return Util.sendMessage(message, channel);
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
