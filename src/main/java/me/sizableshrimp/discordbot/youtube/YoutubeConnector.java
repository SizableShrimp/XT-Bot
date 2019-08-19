package me.sizableshrimp.discordbot.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import me.sizableshrimp.discordbot.Bot;
import org.slf4j.LoggerFactory;

class YoutubeConnector {
    private static final String KEY = Bot.getConfig().getProperty("GOOGLE_KEY");
    private static YouTube youtube;

    private YoutubeConnector() {}

    public static void load() {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            youtube = new YouTube.Builder(httpTransport, JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("XT Bot")
                    .build();
        } catch (Exception e) {
            LoggerFactory.getLogger(YoutubeConnector.class).error("Error occurred while trying to initialize.", e);
        }
    }

    static Channel getChannelById(String channelId) {
        try {
            ChannelListResponse response = getChannelResponse(channelId);
            if (response != null && !response.getItems().isEmpty()) {
                return response.getItems().get(0);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(YoutubeConnector.class).error("Error occurred while trying to check valid id.", e);
        }
        return null;
    }

    static Channel getChannelByCustomUrl(String customUrl) {
        try {
            SearchListResponse response = youtube.search()
                    .list("id,snippet")
                    .setKey(KEY)
                    .setType("channel")
                    .setQ(customUrl)
                    .execute();
            for (SearchResult result : response.getItems()) {
                //search function does not provide customUrl property even if it exists
                String channelId = result.getSnippet().getChannelId();
                ChannelListResponse channelResponse = getChannelResponse(channelId);
                if (channelResponse == null)
                    continue;
                Channel channel = channelResponse.getItems().get(0);
                String other = channel.getSnippet().getCustomUrl();

                if (customUrl.equalsIgnoreCase(other)) return channel;
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(YoutubeConnector.class).error("Error occurred while trying to check valid custom url.", e);
        }
        return null;
    }

    private static ChannelListResponse getChannelResponse(String channelId) {
        try {
            return youtube.channels()
                    .list("snippet,contentDetails,statistics")
                    .setKey(KEY)
                    .setId(channelId)
                    .execute();
        } catch (Exception e) {
            LoggerFactory.getLogger(YoutubeConnector.class).error("Error occurred while trying to fetch channel.", e);
        }

        return null;
    }
}
