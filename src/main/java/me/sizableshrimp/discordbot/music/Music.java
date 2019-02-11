package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.sizableshrimp.discordbot.Util;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Music {
    private static final List<Snowflake> lockedGuilds = new ArrayList<>();
    public static final AudioPlayerManager playerManager;
    private static final Map<Snowflake, GuildMusicManager> musicManagers;
    public static final Map<Snowflake, VoiceConnection> connections = new HashMap<>();

    private Music() {}

    static {
        musicManagers = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public static synchronized GuildMusicManager getGuildManager(DiscordClient client, Snowflake guildId) {
        return musicManagers.computeIfAbsent(guildId, k -> new GuildMusicManager(playerManager, guildId, client));
    }

    public static void skipTrack(DiscordClient client, Snowflake guildId) {
        GuildMusicManager musicManager = getGuildManager(client, guildId);
        musicManager.scheduler.nextTrack();
    }

    public static Mono<Integer> getBotVoiceChannelMajority(DiscordClient client, Snowflake guildId) {
        return getBotConnectedVoiceChannel(client, guildId)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .map(num -> (int) Math.ceil((num - 1) / 2d)); //excludes bot
    }

    public static Mono<VoiceChannel> getBotConnectedVoiceChannel(DiscordClient client, Snowflake guildId) {
        return client.getSelf()
                .flatMap(u -> u.asMember(guildId))
                .flatMap(Music::getConnectedVoiceChannel);
    }

    public static Mono<VoiceChannel> getConnectedVoiceChannel(Member member) {
        return member.getVoiceState().flatMap(VoiceState::getChannel);
    }

    public static String getTrackTime(long millis) {
        StringBuilder builder = new StringBuilder();
        Duration duration = Duration.ofMillis(millis);

        if (duration.toHours() > 0) builder.append(String.format("%02d:", duration.toHours()));
        duration = duration.minusHours(duration.toHours());
        builder.append(String.format("%02d:", duration.toMinutes()));
        duration = duration.minusMinutes(duration.toMinutes());
        builder.append(String.format("%02d", duration.getSeconds()));

        return builder.toString();
    }

    public static Mono<Boolean> locked(Member member, TextChannel channel) {
        return channel.getGuild().map(Guild::getId)
                .filter(lockedGuilds::contains) //only want locked guilds
                .filterWhen(snowflake -> MusicPermission.getPermission(member).map(perm -> perm == MusicPermission.NONE))
                .flatMap(snowflake -> Util.sendMessage(":lock: Music is currently locked for normal members. Please try again later.", channel))
                .hasElement();
    }

    public static void disconnectBotFromChannel(Snowflake guildId) {
        VoiceConnection connection = connections.get(guildId);
        if (connection != null) connection.disconnect();
        connections.remove(guildId);
        musicManagers.remove(guildId);
        lockedGuilds.remove(guildId);
    }

    public static class LavaplayerAudioProvider extends AudioProvider {

        private final AudioPlayer player;
        private final MutableAudioFrame frame = new MutableAudioFrame();

        public LavaplayerAudioProvider(AudioPlayer player) {
            super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
            this.player = player;
            this.frame.setBuffer(getBuffer());
        }

        @Override
        public boolean provide() {
            boolean didProvide = player.provide(frame);
            if (didProvide) getBuffer().flip();
            return didProvide;
        }
    }
}
