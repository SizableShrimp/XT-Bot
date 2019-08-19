package me.sizableshrimp.discordbot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
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

public class NowPlayingCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "Shows information about the currently playing song.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.NONE);
    }

    @Override
    public Set<String> getNames() {
        return Set.of("nowplaying", "np");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel()
                .filterWhen(c -> hasPermission(event))
                .flatMap(c -> {
                    GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
                    AudioTrack playing = manager.player.getPlayingTrack();
                    Consumer<EmbedCreateSpec> spec = embed -> {
                        embed.setAuthor("Now Playing", null, null);
                        embed.setColor(new Color(242, 242, 242));
                    };
                    if (playing == null) {
                        spec = spec.andThen(embed -> embed.setDescription("There is currently nothing playing."));
                        return sendEmbed(spec, c);
                    }
                    AudioTrackInfo info = playing.getInfo();
                    String description = String.format("[%s](%s) %n%s / %s", info.title, info.uri,
                            Music.getTrackTime(playing.getPosition()), Music.getTrackTime(info.length));
                    return sendEmbed(spec.andThen(embed -> embed.setDescription(description)), c);
                });

    }
}
