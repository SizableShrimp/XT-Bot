package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Bot {
    public static DiscordClient client;
    public static final String prefix = "/";
    public static final long FIRST_ONLINE = System.currentTimeMillis();

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CHANNEL_ID = "UCKrMGLGMhxIuMHQdHOf1YIw";
    private static final long YOUTUBE_CHANNEL = 341028279584817163L;
    private static final long GREETING_CHANNEL = 332985255151665152L;
    private static boolean isLive = false;
    private static String latestVideo;

    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        client = new DiscordClientBuilder(System.getenv("TOKEN")).build();
        EventDispatcher dispatcher = client.getEventDispatcher();
        EventListener listener = new EventListener();
        dispatcher.on(MessageCreateEvent.class)
                .filterWhen(e -> e.getMessage().getChannel().map(c -> c.getType() == Channel.Type.GUILD_TEXT)) //don't want DM commands
                .filterWhen(e -> e.getMessage().getAuthor().filter(u -> !u.isBot()).hasElement()) //don't want webhooks
                .flatMap(listener::onMessageCreate)
                .onErrorContinue((error, event) -> LoggerFactory.getLogger(Bot.class).error("Event listener had an uncaught exception!", error))
                .subscribe();
        dispatcher.on(ReadyEvent.class)
                .take(1)
                .flatMap(listener::onReady)
                .subscribe();
        dispatcher.on(VoiceStateUpdateEvent.class)
                .flatMap(listener::onVoiceStateUpdate)
                .subscribe();
        client.login().block();
    }

    public static Mono<Message> sendMessage(String string, MessageChannel channel) {
        return channel.createMessage("\u200B" + string);
    }

    public static Mono<Message> sendEmbed(EmbedCreateSpec embed, MessageChannel channel) {
        return channel.createMessage(message -> message.setEmbed(embed));
    }


    public static synchronized void schedule(DiscordClient client) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduler.scheduleAtFixedRate(() -> client.getChannelById(Snowflake.of(GREETING_CHANNEL))
                .ofType(TextChannel.class)
                .flatMap(c -> sendMessage("Happy 420!", c))
                .subscribe(), dailyMeme(), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS /*dailyMeme() returns seconds*/);
        boolean heroku = System.getenv().containsKey("HEROKU");
        if (heroku) {
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
        scheduler.scheduleAtFixedRate(() -> checkStreaming().and(checkNewVideo())
                .onErrorResume(throwable -> {
                    //TODO remove if statement after testing is over
                    if (throwable instanceof ClientException) {
                        int code = ((ClientException) throwable).getStatus().code();
                        if (code == 403 || code == 404) return Mono.empty();
                    }
                    LoggerFactory.getLogger(Bot.class).error("YouTube listener had an uncaught exception!", throwable);
                    return Mono.empty();
                }).subscribe(), 0, 60, TimeUnit.SECONDS);
    }

    private static Mono<Message> checkStreaming() {
        return Mono.fromCallable(() -> {
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + CHANNEL_ID + "&eventType=live&type=video&key=" + System.getenv("GOOGLE_KEY")).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            try (InputStream stream = connection.getInputStream()) {
                JsonNode json = mapper.readTree(stream);
                int totalResults = json.get("pageInfo").get("totalResults").intValue();
                if (totalResults == 0 && isLive) {
                    isLive = false;
                } else if (totalResults == 1 && !isLive) {
                    isLive = true;
                    return json.withArray("items").get(0);
                }
                return null;
            }
        }).flatMap(json -> client.getChannelById(Snowflake.of(YOUTUBE_CHANNEL))
                .cast(TextChannel.class)
                .flatMap(c -> c.getMessagesBefore(Snowflake.of(Instant.now()))
                        .take(10)
                        .any(msg -> msg.getContent().map(content -> content.contains(json.get("id").get("videoId").textValue())).orElse(false))
                        .filter(posted -> !posted) //only want to send message if not already posted previously
                        .flatMap(ignored -> sendMessage(String.format("@everyone **%s** is :red_circle:**LIVE**:red_circle:! %nhttps://www.youtube.com/watch?v=%s",
                                json.get("snippet").get("channelTitle").textValue(),
                                json.get("id").get("videoId").textValue()), c))));
    }

    private static Mono<Message> checkNewVideo() {
        return Mono.fromCallable(() -> {
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + CHANNEL_ID + "&maxResults=1&order=date&type=video&key=" + System.getenv("GOOGLE_KEY")).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream stream = connection.getInputStream()) {
                    JsonNode json = mapper.readTree(stream);
                    if (json.get("pageInfo").get("totalResults").intValue() > 0) {
                        return json.withArray("items").get(0);
                    }
                }
            }
            return null;
        }).flatMap(json -> {
            ZonedDateTime publishDate = Instant.parse(json.get("snippet").get("publishedAt").textValue()).atZone(ZoneId.of("US/Eastern"));
            return client.getChannelById(Snowflake.of(YOUTUBE_CHANNEL)).cast(TextChannel.class)
                    .filter(c -> !latestVideo.equals(json.get("id").get("videoId").textValue()))
                    .flatMap(c -> c.getMessagesBefore(Snowflake.of(Instant.now()))
                            .take(10)
                            .any(msg -> msg.getContent().map(content -> content.contains(json.get("id").get("videoId").textValue())).orElse(false))
                            .doOnNext(ignored -> latestVideo = json.get("id").get("videoId").textValue())
                            .filter(posted -> !posted) //only want to send message if not already posted previously
                            .flatMap(ignored -> sendMessage(String.format("@everyone **%s** uploaded **%s** on %s %nhttps://www.youtube.com/watch?v=%s",
                                    json.get("snippet").get("channelTitle").textValue(),
                                    json.get("snippet").get("title").textValue(),
                                    getTime(publishDate),
                                    json.get("id").get("videoId").textValue()), c)));
        });
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
            case 1:
            case 21:
            case 31:
                ordinal = "st";
                break;
            case 2:
            case 22:
                ordinal = "nd";
                break;
            case 3:
            case 23:
                ordinal = "rd";
                break;
            default:
                ordinal = "th";
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE, MMMM d'" + ordinal + "', yyyy");
        return format.format(time);
    }
}
