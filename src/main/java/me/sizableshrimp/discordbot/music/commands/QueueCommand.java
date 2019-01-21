package me.sizableshrimp.discordbot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueueCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("queue", "q").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
        AudioTrack playing = manager.player.getPlayingTrack();
        EmbedCreateSpec embed = new EmbedCreateSpec();
        embed.setAuthor("Queue", null, null);
        embed.setColor(new Color(242, 242, 242));
        if (playing == null) {
            embed.setDescription("There is currently nothing playing.");
            return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
        }
        StringBuilder description = new StringBuilder();
        description.append(String.format("__**Now Playing:**__%n[%s](%s) | `%s`", playing.getInfo().title, playing.getInfo().uri, Music.getTime(playing.getInfo().length)));
        description.append("\n\n__**Up Next:**__\n");
        if (manager.scheduler.queue.isEmpty()) {
            description.append("\nThere is currently nothing up next.");
            embed.setDescription(description.toString());
            return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
        }
        int number = 1;
        for (AudioTrack track : manager.scheduler.queue) {
            description.append(String.format("%n%d. [%s](%s) | `%s`", number, track.getInfo().title, track.getInfo().uri, Music.getTime(track.getInfo().length)));
            number++;
        }
        embed.setDescription(description.toString());
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
    }
}
