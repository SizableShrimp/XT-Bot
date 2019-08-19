package me.sizableshrimp.discordbot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;

public class RemoveCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("remove [number from queue]",
                "Removes the song from the queue at the number given.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Set.of("remove");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
                .flatMap(c -> remove(c, event, args));
    }

    private Mono<Message> remove(MessageChannel channel, MessageCreateEvent event, String[] args) {
        GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
        if (manager.scheduler.audioQueue.isEmpty()) {
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
        if (manager.scheduler.audioQueue.size() < queueNum || queueNum <= 0) {
            return sendMessage("Please enter a number from the queue.", channel);
        }

        AudioTrack selected = null;
        int num = 0;
        for (AudioTrack track : manager.scheduler.audioQueue) {
            num++;
            if (queueNum == num) {
                selected = track;
                break;
            }
        }
        if (selected == null) {
            return sendMessage("Please enter a number from the queue.", channel);
        }
        manager.scheduler.audioQueue.remove(selected);
        return sendMessage("Removed `" + selected.getInfo().title + "` from the queue.", channel);
    }
}
