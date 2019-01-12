package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermissions;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoopCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("loop").collect(Collectors.toSet());
    }

    @Override
    protected Mono<?> run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        return filterLockedAndPermissions(event, MusicPermissions.DJ, MusicPermissions.ALONE)
                .flatMap(c -> {
                    GuildMusicManager manager = Music.getGuildManager(event.getGuildId().get());
                    final boolean looping = manager.scheduler.isRepeating();
                    manager.scheduler.setRepeating(!looping);
                    if (looping) {
                        return sendMessage("Loop disabled.", c);
                    } else {
                        return sendMessage(":repeat: Loop enabled.", c);
                    }
                });
    }
}
