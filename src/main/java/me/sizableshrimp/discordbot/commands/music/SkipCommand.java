package me.sizableshrimp.discordbot.commands.music;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
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
                        .switchIfEmpty(voteSkip(event.getMember().get(), c)));
    }

    private static Mono<Message> voteSkip(Member member, TextChannel channel) {
        GuildMusicManager musicManager = Music.getGuildManager(member.getClient(), channel.getGuildId());
        Set<Snowflake> usersSkipping = musicManager.usersSkipping;
        if (usersSkipping.contains(member.getId())) {
            return Util.sendMessage(":x: You have already voted to skip.", channel);
        }

        Mono<Integer> majority = Music.getBotVoiceChannelMajority(member.getClient(), channel.getGuildId());
        usersSkipping.add(member.getId());
        Mono<Message> added = majority
                .flatMap(num -> Util.sendMessage(musicManager.usersSkipping.size() + "/" + num + " users requesting to skip.", channel));

        return majority
                .filter(maj -> musicManager.usersSkipping.size() >= maj)
                .doOnNext(ignored -> Music.skipTrack(member.getClient(), channel.getGuildId()))
                .flatMap(ignored -> Util.sendMessage(":white_check_mark: Song skipped.", channel))
                .switchIfEmpty(added);
    }
}
