package me.sizableshrimp.discordbot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import me.sizableshrimp.discordbot.music.GuildMusicManager;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GotoCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname% [time in song]",
                "Starts playing from a certain point in the song.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return EnumSet.of(MusicPermission.DJ, MusicPermission.ALONE);
    }

    @Override
    public Set<String> getNames() {
        return Set.of("goto");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel().cast(TextChannel.class)
                .filterWhen(c -> hasPermission(event))
                .flatMap(channel -> goTo(channel, event, args));
    }

    private Mono<Message> goTo(TextChannel channel, MessageCreateEvent event, String[] args) {
        String message = event.getMessage().getContent().orElse("");
        GuildMusicManager manager = Music.getGuildManager(event.getClient(), event.getGuildId().get());
        if (manager.player.getPlayingTrack() == null) {
            return sendMessage("There is nothing to change the time of.", channel);
        }
        if (args.length != 1 || !message.contains(":")) {
            return incorrectUsage(event);
        }
        String time = args[0];
        int colons = 0;
        for (char c : time.toCharArray()) {
            if (c == ':') {
                colons++;
            }
        }
        if (colons != 1 && colons != 2) {
            return incorrectUsage(event);
        }
        List<Integer> numbers = new ArrayList<>();
        for (String s : time.split(":")) {
            try {
                Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return incorrectUsage(event);
            }
            numbers.add(Integer.valueOf(s));
        }
        long millis = colons == 1
                ? TimeUnit.MINUTES.toMillis(numbers.get(0)) + TimeUnit.SECONDS.toMillis(numbers.get(1))
                : TimeUnit.HOURS.toMillis(numbers.get(0)) + TimeUnit.MINUTES.toMillis(numbers.get(1)) + TimeUnit.SECONDS.toMillis(numbers.get(2));
        AudioTrack track = manager.player.getPlayingTrack();
        if (millis < 0L || millis > track.getDuration()) {
            return sendMessage("Specified time is out of range. Please choose a different time.", channel);
        }
        track.setPosition(millis);
        return sendMessage("Now playing at `" + time + "`", channel);
    }
}
