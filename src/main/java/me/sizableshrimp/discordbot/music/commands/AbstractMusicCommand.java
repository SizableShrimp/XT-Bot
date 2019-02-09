package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Set;

public abstract class AbstractMusicCommand extends Command {
    /**
     * Returns a set of music permissions.
     * For a user to have permission to run this command, they must have one of the permissions in the returned set.
     *
     * @return A set of required music permissions
     */
    public abstract Set<MusicPermission> getRequiredPermissions();

    /**
     * Returns a Mono which emits true if the user of the event has all the necessary permissions, otherwise emits false
     *
     * @param event The event used for obtaining the member and channel
     * @return true if the user has the required music permissions, otherwise false
     */
    Mono<Boolean> hasPermission(MessageCreateEvent event) {
        return hasPermission(event, true);
    }

    Mono<Boolean> hasPermission(MessageCreateEvent event, boolean displayNoPermission) {
        return event.getMessage().getChannel().cast(TextChannel.class)
                .flatMap(c -> MusicPermission.requirePermissions(event.getMember().get(), c, getRequiredPermissions(), displayNoPermission));
    }

    @Override
    public Mono run(MessageCreateEvent event) {
        if (!event.getMessage().getContent().isPresent() || !event.getMember().isPresent()) return Mono.empty();
        String[] temp = event.getMessage().getContent().get().split(" ");
        String[] args = Arrays.copyOfRange(temp, 1, temp.length);
        MusicPermission.getPermission(event.getMember().get()).subscribe(System.out::println);

        Mono<VoiceChannel> botConnected = Music.getBotConnectedVoiceChannel(event.getClient(), event.getGuildId().get());
        Mono<VoiceChannel> userConnected = Music.getConnectedVoiceChannel(event.getMember().get());
        Mono<Boolean> sameChannel = botConnected
                .flatMap(voiceChannel -> userConnected.map(voiceChannel::equals))
                .defaultIfEmpty(false);
        Mono<Message> fallback = event.getMessage().getChannel()
                .flatMap(c -> sendMessage(":x: You must be connected to the same voice channel to use this command!", c));

        return locked(event)
                .filter(locked -> !locked)
                .flatMap(ignored -> sameChannel
                        .filter(same -> this instanceof MusicCommand || this instanceof PlayCommand || same)
                        .flatMap(same -> run(event, args).thenReturn(same)) //don't let empty monos from run trigger empty fallback
                        .switchIfEmpty(fallback));
    }

    private Mono<Boolean> locked(MessageCreateEvent event) {
        return event.getMessage().getChannel().cast(TextChannel.class)
                .flatMap(c -> Music.locked(event.getMember().get(), c));
    }

    @Override
    protected Mono<Message> incorrectUsage(MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(MusicCommand.displayMusic(event, this), c));
    }
}
