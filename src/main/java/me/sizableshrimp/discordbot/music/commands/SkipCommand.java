package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkipCommand extends AbstractMusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "If the user is a `DJ` or alone with the bot, using this command will immediately" +
                        "skip and go to the next song. Otherwise, this will request to skip the current song." +
                        "Once a majority of the default users have voted to skip, the next song will be played.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("skip").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel().cast(TextChannel.class)
                .flatMap(c -> hasPermission(event, false)
                        .filter(hasPermission -> hasPermission)
                        .doOnNext(b -> Music.skipTrack(event.getClient(), event.getGuildId().get()))
                        .flatMap(b -> sendMessage(":white_check_mark: Song skipped.", c))
                        .switchIfEmpty(Music.voteSkip(event.getMember().get(), c)));
    }
}
