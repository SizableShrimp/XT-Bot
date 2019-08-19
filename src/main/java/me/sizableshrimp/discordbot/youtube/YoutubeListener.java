package me.sizableshrimp.discordbot.youtube;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import discord4j.core.DiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.route.Routes;
import lombok.Data;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.Util;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YoutubeListener {
    private static final String YOUTUBE_CHANNEL_ID = "UCKrMGLGMhxIuMHQdHOf1YIw";
    private static final long NOTIFICATION_CHANNEL = 474641238390210562L; //341028279584817163L;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String PARAMS = "&channelId=" + YOUTUBE_CHANNEL_ID + "&key=" + Bot.getConfig().getProperty("GOOGLE_KEY");
    private static final String VIDEO_URL = BASE_URL + "search?part=snippet&maxResults=1&order=date&type=video" + PARAMS;
    private static final String STREAMING_URL = BASE_URL + "search?part=snippet&eventType=live&type=video" + PARAMS;
    private static final String STATISTICS_URL = BASE_URL + "channels?part=statistics%2Csnippet" + PARAMS.replace("channelId", "id");
    private static final OkHttpClient OK_HTTP = new OkHttpClient();

    private YoutubeListener() {}

    public static void schedule(DiscordClient client) {
        //YoutubeConnector.load();
        Flux.interval(Duration.ofMinutes(5))
                .flatMap(l -> checkStreaming(client)
                        .then(checkNewVideo(client))
                        .then(updateStatistics(client))
                        .onErrorResume(throwable -> {
                            if (throwable instanceof ClientException) {
                                int code = ((ClientException) throwable).getStatus().code();
                                if (code == 403 || code == 404) return Mono.empty();
                            }
                            LoggerFactory.getLogger(Bot.class).error("YouTube listener had an uncaught exception!", throwable);
                            return Mono.empty();
                        }))
                .subscribe();
    }

    private static Mono<Void> checkStreaming(DiscordClient client) {
        return getYoutubeJson(STREAMING_URL)
                .flatMap(json -> sendNotification(client, json, String.format("**%s** is :red_circle:**LIVE**:red_circle:! %nhttps://www.youtube.com/watch?v=%s",
                        json.get("snippet").get("channelTitle").textValue(),
                        json.get("id").get("videoId").textValue())));
    }

    private static Mono<Void> checkNewVideo(DiscordClient client) {
        return getYoutubeJson(VIDEO_URL)
                .flatMap(json -> {
                    ZonedDateTime publishDate = Instant.parse(json.get("snippet").get("publishedAt").textValue()).atZone(ZoneId.of("US/Eastern"));
                    return sendNotification(client, json, String.format("**%s** uploaded **%s** on %s %nhttps://www.youtube.com/watch?v=%s",
                            json.get("snippet").get("channelTitle").textValue(),
                            json.get("snippet").get("title").textValue(),
                            Util.getTime(publishDate),
                            json.get("id").get("videoId").textValue()));
                });
    }

    private static Mono<Void> updateStatistics(DiscordClient client) {
        return getYoutubeJson(STATISTICS_URL)
                .zipWith(client.getChannelById(Snowflake.of(NOTIFICATION_CHANNEL))
                        .cast(GuildMessageChannel.class)
                        .flatMap(GuildMessageChannel::getGuild)
                        .flatMap(YoutubeListener::getStatisticsCategory))
                .flatMap(tuple -> {
                    JsonNode json = tuple.getT1();
                    Statistics stats;
                    try {
                        stats = Bot.MAPPER.treeToValue(json.path("statistics"), Statistics.class);
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                    Category category = tuple.getT2();
                    return getStatsChannel(category, "Channel Name")
                            .flatMap(channel -> setStats(channel, json.path("snippet").path("title").textValue()))
                            .then(getStatsChannel(category, "Subscribers")
                                    .flatMap(channel -> setStats(channel, stats.subscriberCount)))
                            .then(getStatsChannel(category, "Views")
                                    .flatMap(channel -> setStats(channel, stats.viewCount)))
                            .then(getStatsChannel(category, "Posted Videos")
                                    .flatMap(channel -> setStats(channel, stats.videoCount)))
                            .then();
                });
    }

    private static Mono<VoiceChannel> setStats(VoiceChannel voiceChannel, int stat) {
        return setStats(voiceChannel, String.format("%,d", stat));
    }

    private static Mono<VoiceChannel> setStats(VoiceChannel voiceChannel, String str) {
        String name = voiceChannel.getName();
        int index = name.lastIndexOf(':');
        if (index == -1) index = name.length();
        String prefix = voiceChannel.getName().substring(0, index);
        String newName = prefix + ": " + str;
        if (newName.equals(name)) return Mono.empty();
        return voiceChannel.edit(edit -> edit.setName(newName));
    }

    private static Mono<VoiceChannel> getStatsChannel(Category category, String startsWith) {
        return category.getChannels()
                .ofType(VoiceChannel.class)
                .skipUntil(channel -> channel.getName().startsWith(startsWith))
                .next()
                .switchIfEmpty(category.getGuild().flatMap(guild -> guild.createVoiceChannel(createVoiceChannel -> {
                    createVoiceChannel.setParentId(category.getId());
                    createVoiceChannel.setName(startsWith);
                    createVoiceChannel.setPosition(0);
                    PermissionOverwrite overwrite =
                            PermissionOverwrite.forRole(category.getGuildId() /*@everyone role*/, PermissionSet.none(), PermissionSet.of(Permission.CONNECT));
                    createVoiceChannel.setPermissionOverwrites(Stream.of(overwrite).collect(Collectors.toSet()));
                })));
    }

    private static Mono<Category> getStatisticsCategory(Guild guild) {
        return guild.getChannels()
                .ofType(Category.class)
                .skipUntil(category -> category.getName().equals("Youtube Statistics"))
                .next()
                .switchIfEmpty(guild.createCategory(createCategory -> {
                    createCategory.setName("Youtube Statistics");
                    createCategory.setPosition(-1); //discord is weird
                }));
    }

    private static Mono<JsonNode> getYoutubeJson(String url) {
        return Mono.fromCallable(() -> {
            JsonNode json = Bot.MAPPER.readTree(new URL(url));
            if (json.get("pageInfo").get("totalResults").intValue() > 0) {
                return json.withArray("items").get(0);
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
    private static Mono<Void> sendNotification(DiscordClient client, JsonNode json, String message) {
        return client.getChannelById(Snowflake.of(NOTIFICATION_CHANNEL))
                .cast(GuildMessageChannel.class)
                .filterWhen(c -> notPosted(c, json))
                .flatMap(YoutubeListener::getYoutubeWebhook)
                .map(webhook -> Routes.BASE_URL + Routes.WEBHOOK_EXECUTE.newRequest(webhook.getId().asString(), webhook.getToken()).getCompleteUri())
                .flatMap(uri -> Mono.fromCallable(() -> OK_HTTP.newCall(new Request.Builder()
                        .url(uri)
                        .post(new FormBody.Builder()
                                .add("content", message)
                                .build())
                        .build()).execute()))
                .doOnNext(Response::close)
                .then();
    }

    private static Mono<Webhook> getYoutubeWebhook(GuildMessageChannel channel) {
        return channel.getWebhooks()
                .skipUntil(webhook -> webhook.getName().map(name -> name.equals("YouTube")).orElse(false))
                .next()
                .switchIfEmpty(channel.createWebhook(spec -> {
                    spec.setAvatar(Image.ofRaw(getProfilePicture(), Image.Format.PNG));
                    spec.setName("YouTube");
                }));
    }

    private static byte[] profilePicture;

    private static byte[] getProfilePicture() {
        if (profilePicture == null) {
            URL url = YoutubeListener.class.getResource("/youtube.png");

            try {
                Path path = Path.of(url.toURI());
                profilePicture = Files.readAllBytes(path);
            } catch (Exception e) {
                e.printStackTrace();
                profilePicture = new byte[0];
            }
        }

        return profilePicture;
    }

    /**
     * Returns true if the past 10 messages do NOT contain the video id or title, false otherwise.
     *
     * @param channel The channel to check the past 10 messages.
     * @param json    The Youtube JSON to get the videoId.
     * @return A {@link Mono} which emits true if the past 10 messages do NOT contain the video id or title, false otherwise.
     */
    private static Mono<Boolean> notPosted(MessageChannel channel, JsonNode json) {
        String title = json.get("snippet").get("title").textValue();
        String videoId = json.get("id").get("videoId").textValue();

        return channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(10)
                .any(msg -> sameTitle(msg, title) || msg.getContent().map(content -> content.contains(videoId)).orElse(false))
                .map(posted -> !posted);
    }

    /**
     * Returns true if the message has an embed and the embed title is the same as the title provided.
     *
     * @param message The message to check.
     * @param title   The title to compare
     * @return True if the message has an embed and the titles match, false otherwise.
     */
    private static boolean sameTitle(Message message, String title) {
        //useful in case of a stream restarting or re-upload
        var embeds = message.getEmbeds();
        if (embeds.isEmpty()) return false;

        return embeds.get(0).getTitle()
                .map(title::equals)
                .orElse(false);
    }

    @Data
    private static class Statistics {
        int viewCount;
        int commentCount;
        int subscriberCount;
        boolean hiddenSubscriberCount;
        int videoCount;
    }
}
