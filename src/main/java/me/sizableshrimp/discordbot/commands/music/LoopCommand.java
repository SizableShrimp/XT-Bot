package me.sizableshrimp.discordbot.commands.music;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;

public class LoopCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname% (queue)",
                "Toggles between loop on or off. If you add `queue`, it will loop the entire queue.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Set.of("loop");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
                .flatMap(c -> loop(event, args, c));
    }

    private Mono<Message> loop(MessageCreateEvent event, String[] args, MessageChannel channel) {
        GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
        if (args.length == 1 && args[0].equalsIgnoreCase("queue")) {
            if (manager.scheduler.isRepeating()) {
                return sendMessage(":x: You must disable song looping first.", channel);
            }
            final boolean looping = manager.scheduler.isQueueRepeating();
            manager.scheduler.setQueueRepeating(!looping);
            if (looping) {
                return sendMessage("Queue loop disabled.", channel);
            } else {
                return sendMessage(":repeat: Queue loop enabled.", channel);
            }
        } else {
            if (manager.scheduler.isQueueRepeating()) {
                return sendMessage(":x: You must disable queue looping first.", channel);
            }
            final boolean looping = manager.scheduler.isRepeating();
            manager.scheduler.setRepeating(!looping);
            if (looping) {
                return sendMessage("Loop disabled.", channel);
            } else {
                return sendMessage(":repeat: Loop enabled.", channel);
            }
        }
    }
}
