package me.sizableshrimp.discordbot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NowPlayingCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("nowplaying", "np").collect(Collectors.toSet());
    }

    @Override
    protected Mono run(MessageCreateEvent event, String[] args) {
        GuildMusicManager manager = Music.getGuildManager(event.getGuildId().get());
        AudioTrack playing = manager.player.getPlayingTrack();
        EmbedCreateSpec embed = new EmbedCreateSpec();
        embed.setAuthor("Now Playing", null, null);
        embed.setColor(new Color(242, 242, 242));
        if (playing == null) {
            embed.setDescription("There is currently nothing playing.");
            return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
        }
        AudioTrackInfo info = playing.getInfo();
        String description = String.format("[%s](%s) %n%s / %s", info.title, info.uri, Music.getTime(playing.getPosition()), Music.getTime(info.length));
        embed.setDescription(description);
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
    }
}
