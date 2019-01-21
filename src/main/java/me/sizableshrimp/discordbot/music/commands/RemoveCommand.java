package me.sizableshrimp.discordbot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermissions;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoveCommand extends Command {
    @Override
    public String getUsage() {
        return "remove [number from queue]";
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("remove").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (!event.getMember().isPresent()) return Mono.empty();
        return filterLockedAndPermissions(event, MusicPermissions.DJ, MusicPermissions.ALONE)
                .flatMap(c -> remove(c, event, args));
    }

    private Mono<Message> remove(MessageChannel channel, MessageCreateEvent event, String[] args) {
        GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
        if (manager.scheduler.queue.isEmpty()) {
            return sendMessage("There is nothing in the queue to remove.", channel);
        }
        if (args.length != 1) {
            return incorrectUsage(event);
        }
        try {
            Integer.valueOf(args[0]);
        } catch (NumberFormatException exception) {
            return sendMessage("Please enter a number from the queue.", channel);
        }
        int queueNum = Integer.parseInt(args[0]);
        if (manager.scheduler.queue.size() < queueNum || queueNum <= 0) {
            return sendMessage("Please enter a number from the queue.", channel);
        }
        AudioTrack selected = null;
        int num = 0;
        for (AudioTrack track : manager.scheduler.queue) {
            num++;
            if (queueNum == num) {
                selected = track;
                break;
            }
        }
        if (selected == null) {
            return sendMessage("Please enter a number from the queue.", channel);
        }
        manager.scheduler.queue.remove(selected);
        return sendMessage("Removed `" + selected.getInfo().title + "` from the queue.", channel);
    }
}
