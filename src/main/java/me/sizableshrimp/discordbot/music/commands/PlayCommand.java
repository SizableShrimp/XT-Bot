package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.voice.AudioReceiver;
import discord4j.voice.VoiceConnection;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayCommand extends Command {
    @Override
    public String getUsage() {
        return "play [song]";
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("play").collect(Collectors.toSet());
    }

    @Override
    protected Mono run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        if (args.length < 1) return incorrectUsage(event);
        return filterLocked(event)
                .flatMap(b -> Music.getConnectedVoiceChannel(event.getMember().get()))
                .zipWith(Music.getBotConnectedVoiceChannel(event.getGuildId().get()))
                .flatMap(tuple -> {
                    VoiceChannel userConnected = tuple.getT1().orElse(null);
                    VoiceChannel botConnected = tuple.getT2().orElse(null);
                    String query = String.join(" ", args);
                    boolean isValid = true;
                    try {
                        new URL(query).toURI();
                    } catch (MalformedURLException | URISyntaxException e) {
                        isValid = false;
                    }
                    if (!isValid) query = "ytsearch:" + query;
                    String finalQuery = query;
                    boolean finalValid = isValid;
                    if (botConnected != null && botConnected.equals(userConnected)) {
                        return event.getMessage().getChannel().ofType(TextChannel.class)
                                .doOnNext(c -> Music.loadAndPlay(c, finalQuery, finalValid));
                    } else if (botConnected == null && userConnected == null) {
                        return event.getMessage().getChannel()
                                .flatMap(c -> sendMessage("Join a voice channel if you want me to play a song!", c));
                    } else if (botConnected != null && userConnected == null) {
                        return event.getMessage().getChannel()
                                .flatMap(c -> sendMessage("Join `" + botConnected.getName() + "` to add a song to the queue.", c));
                    } else if (botConnected == null) {
                        GuildMusicManager manager = Music.getGuildManager(event.getGuildId().get());
                        Mono<VoiceConnection> connection = userConnected.join(new Music.LavaplayerAudioProvider(manager.player), AudioReceiver.NO_OP);
                        return connection.doOnNext(vc -> Music.connections.put(event.getGuildId().get(), vc))
                                .then(event.getMessage().getChannel().ofType(TextChannel.class))
                                .doOnNext(c -> Music.loadAndPlay(c, finalQuery, finalValid));
                    }
                    return Mono.empty();
                });
    }
}
