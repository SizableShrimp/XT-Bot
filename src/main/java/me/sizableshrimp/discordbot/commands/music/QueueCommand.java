package me.sizableshrimp.discordbot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueueCommand extends AbstractMusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "Shows information about the songs in the queue.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.NONE);
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("queue", "q").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
                .flatMap(c -> {
                    GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
                    AudioTrack playing = manager.player.getPlayingTrack();
                    Consumer<EmbedCreateSpec> spec = embed -> {
                        embed.setAuthor("Queue", null, null);
                        embed.setColor(new Color(242, 242, 242));
                    };
                    if (playing == null) {
                        return sendEmbed(spec.andThen(embed -> embed.setDescription("There is currently nothing playing.")), c);
                    }
                    StringBuilder description = new StringBuilder();
                    description.append(String.format("__**Now Playing:**__%n[%s](%s) | `%s`", playing.getInfo().title, playing.getInfo().uri, Music.getTrackTime(playing.getInfo().length)));
                    description.append("\n\n__**Up Next:**__\n");
                    if (manager.scheduler.queue.isEmpty()) {
                        description.append("\nThere is currently nothing up next.");
                        return sendEmbed(spec.andThen(embed -> embed.setDescription(description.toString())), c);
                    }
                    int number = 1;
                    for (AudioTrack track : manager.scheduler.queue) {
                        description.append(String.format("%n%d. [%s](%s) | `%s`", number, track.getInfo().title, track.getInfo().uri, Music.getTrackTime(track.getInfo().length)));
                        number++;
                    }
                    return sendEmbed(spec.andThen(embed -> embed.setDescription(description.toString())), c);
                });
    }
}
