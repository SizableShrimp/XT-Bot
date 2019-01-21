package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.JsonNode;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class YoutubeListener {
    private static final String YOUTUBE_CHANNEL_ID = "UCKrMGLGMhxIuMHQdHOf1YIw";
    private static final long NOTIFICATION_CHANNEL = 490759923735592989L;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + YOUTUBE_CHANNEL_ID;
    private static final String VIDEO_URL = BASE_URL + "&maxResults=1&order=date&type=video&key=" + System.getenv("GOOGLE_KEY");
    private static final String STREAMING_URL = BASE_URL + "&eventType=live&type=video&key=" + System.getenv("GOOGLE_KEY");

    private YoutubeListener() {
    }

    static void schedule(DiscordClient client) {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> checkStreaming(client)
                        .then(checkNewVideo(client))
                        .onErrorResume(throwable -> {
                            if (throwable instanceof ClientException) {
                                int code = ((ClientException) throwable).getStatus().code();
                                if (code == 403 || code == 404) return Mono.empty();
                            }
                            LoggerFactory.getLogger(Bot.class).error("YouTube listener had an uncaught exception!", throwable);
                            return Mono.empty();
                        }).subscribe(), 0, 60, TimeUnit.SECONDS);
    }

    private static Mono<Message> checkStreaming(DiscordClient client) {
        return getYoutubeJson(STREAMING_URL)
                .flatMap(json -> sendNotification(client, json, String.format("@everyone **%s** is :red_circle:**LIVE**:red_circle:! %nhttps://www.youtube.com/watch?v=%s",
                        json.get("snippet").get("channelTitle").textValue(),
                        json.get("id").get("videoId").textValue())));
    }

    private static Mono<Message> checkNewVideo(DiscordClient client) {
        return getYoutubeJson(VIDEO_URL)
                .flatMap(json -> {
                    ZonedDateTime publishDate = Instant.parse(json.get("snippet").get("publishedAt").textValue()).atZone(ZoneId.of("US/Eastern"));
                    return sendNotification(client, json, String.format("@everyone **%s** uploaded **%s** on %s %nhttps://www.youtube.com/watch?v=%s",
                            json.get("snippet").get("channelTitle").textValue(),
                            json.get("snippet").get("title").textValue(),
                            Util.getTime(publishDate),
                            json.get("id").get("videoId").textValue()));
                });
    }

    private static Mono<JsonNode> getYoutubeJson(String url) {
        return Mono.fromCallable(() -> {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream stream = connection.getInputStream()) {
                    JsonNode json = Bot.mapper.readTree(stream);
                    if (json.get("pageInfo").get("totalResults").intValue() > 0) {
                        return json.withArray("items").get(0);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Sends a notification to the notification channel if not posted already
     *
     * @param client  The client to get the notification channel from
     * @param json    The json to use for checking if it has already been posted
     * @param message The text to send if not posted
     * @return A Mono which emits a {@link Message} created from the provided text
     */
    private static Mono<Message> sendNotification(DiscordClient client, JsonNode json, String message) {
        return client.getChannelById(Snowflake.of(NOTIFICATION_CHANNEL))
                .cast(TextChannel.class)
                .filterWhen(c -> notPosted(c, json))
                .flatMap(c -> Util.sendMessage(message, c));
    }

    /**
     * Returns true if the past 10 messages do NOT contain the videoId, otherwise false
     *
     * @param channel The channel to check the past 10 messages
     * @param json    The Youtube JSON to get the videoId
     * @return A {@link Mono} which emits true if the past 10 messages do NOT contain the videoId, otherwise false
     */
    private static Mono<Boolean> notPosted(MessageChannel channel, JsonNode json) {
        return channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(10)
                .any(msg -> msg.getContent().map(content -> content.contains(json.get("id").get("videoId").textValue())).orElse(false))
                .map(posted -> !posted);
    }
}
