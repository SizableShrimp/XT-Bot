package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermissions;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PauseCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("pause", "p").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        return filterLockedAndPermissions(event, MusicPermissions.DJ, MusicPermissions.ALONE)
                .flatMap(c -> {
                    GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
                    if (manager.player.getPlayingTrack() != null) {
                        if (manager.player.isPaused()) {
                            manager.player.setPaused(false);
                            return sendMessage(":arrow_forward: Music resumed.", c);
                        } else {
                            manager.player.setPaused(true);
                            return sendMessage(":pause_button: Music paused.", c);
                        }
                    } else {
                        return sendMessage("There is no music to pause or unpause.", c);
                    }
                });
    }
}
