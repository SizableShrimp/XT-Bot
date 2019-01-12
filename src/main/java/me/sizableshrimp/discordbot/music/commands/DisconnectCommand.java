package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.VoiceChannel;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermissions;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DisconnectCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("disconnect", "leave").collect(Collectors.toSet());
    }

    @Override
    protected Mono run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        return filterLockedAndPermissions(event, MusicPermissions.DJ, MusicPermissions.ALONE)
                .flatMap(c -> {
                    Mono<Optional<VoiceChannel>> botConnected = Music.getBotConnectedVoiceChannel(event.getGuildId().get());
                    return botConnected.map(Optional::isPresent)
                            .flatMap(b -> {
                                if (!b) {
                                    return sendMessage("I am not connected to a voice channel.", c);
                                } else {
                                    Music.disconnectBotFromChannel(event.getGuildId().get());
                                    return botConnected.map(Optional::get).flatMap(vc -> sendMessage("Left `"+vc.getName()+"`", c));
                                }
                            });
                });
    }
}
