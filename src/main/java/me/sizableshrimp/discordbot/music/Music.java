package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.sizableshrimp.discordbot.Bot;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Music {
    private static final List<Snowflake> lockedGuilds = new ArrayList<>();
    private static final AudioPlayerManager playerManager;
    private static final Map<Snowflake, GuildMusicManager> musicManagers;
    public static final Map<Snowflake, VoiceConnection> connections = new HashMap<>();
    private static final int DEFAULT_VOLUME = 35;

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

    public static synchronized GuildMusicManager getGuildManager(Snowflake guildId) {
        GuildMusicManager musicManager = musicManagers.get(guildId);
        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager, guildId);
            musicManager.player.setVolume(DEFAULT_VOLUME);
            musicManagers.put(guildId, musicManager);
        }

        return musicManager;
    }

    public static void loadAndPlay(final TextChannel channel, final String trackUrl, boolean validURL) {
        GuildMusicManager manager = Music.getGuildManager(channel.getGuildId());
        playerManager.loadItemOrdered(manager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                play(channel, manager, track, validURL).subscribe();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();
                if (firstTrack == null) firstTrack = playlist.getTracks().get(0);
                play(channel, manager, firstTrack, validURL).subscribe();
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
            }
        });
    }

    private static Mono<Message> play(TextChannel channel, GuildMusicManager musicManager, AudioTrack track, boolean validURL) {
        if (TimeUnit.MILLISECONDS.toSeconds(track.getDuration()) > TimeUnit.HOURS.toSeconds(10)) {
            return Bot.sendMessage(":x: Cannot play a song over 10 hours in length.", channel);
        }
        return musicManager.scheduler.queue(track, channel, validURL);
    }

    public static void skipTrack(Snowflake guildId) {
        GuildMusicManager musicManager = getGuildManager(guildId);
        musicManager.scheduler.nextTrack();
    }

    private static Mono<Optional<VoiceChannel>> getBotConnectedVoiceChannel(Guild guild) {
        return guild.getClient().getSelf()
                .flatMap(u -> u.asMember(guild.getId()))
                .flatMap(Music::getConnectedVoiceChannel);
    }

    public static Mono<Optional<VoiceChannel>> getBotConnectedVoiceChannel(Snowflake guildId) {
        return Bot.client.getGuildById(guildId).flatMap(Music::getBotConnectedVoiceChannel);
    }

    public static Mono<Optional<VoiceChannel>> getConnectedVoiceChannel(Member member) {
        return member.getVoiceState().flatMap(VoiceState::getChannel).map(Optional::of).defaultIfEmpty(Optional.empty());
    }

    public static String getTime(long millis) {
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
                .filterWhen(snowflake -> MusicPermissions.isDJ(member))
                .flatMap(snowflake -> Bot.sendMessage(":lock: Music is currently locked for normal members. Please try again later.", channel))
                .hasElement();
    }

    public static void disconnectBotFromChannel(Snowflake guildId) {
        Music.connections.get(guildId).disconnect();
        Music.connections.remove(guildId);
        GuildMusicManager manager = Music.getGuildManager(guildId);
        manager.scheduler.queue.clear();
        manager.player.startTrack(null, false);
        manager.player.setVolume(Music.DEFAULT_VOLUME);
        manager.player.setPaused(false);
        Music.lockedGuilds.remove(guildId);
    }


    private static Mono<Boolean> isMemberAlone(Member member, Snowflake channelId) {
        return member.getClient().getChannelById(channelId)
                .ofType(VoiceChannel.class)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .filterWhen(state -> state.getUser().flatMap(u -> u.asMember(member.getGuildId()).map(m -> m.equals(member))))
                .count()
                .map(it -> it == 1)
                .defaultIfEmpty(false);
    }

    static Mono<Boolean> isMemberAlone(Member member) {
        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(vc -> isMemberAlone(member, vc.getId()))
                .defaultIfEmpty(false);
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
