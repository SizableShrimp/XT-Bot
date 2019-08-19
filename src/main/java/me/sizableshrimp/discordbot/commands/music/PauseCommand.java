package me.sizableshrimp.discordbot.commands.music;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;

public class PauseCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%", "Toggles between pausing/resuming the song.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Set.of("pause", "p");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
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
