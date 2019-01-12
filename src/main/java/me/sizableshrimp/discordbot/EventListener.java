package me.sizableshrimp.discordbot;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.commands.MusicCommand;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class EventListener {
    private static List<Command> commands = new ArrayList<>();

    static {
        try {
            ClassPath classpath = ClassPath.from(Bot.class.getClassLoader());
            ImmutableSet<ClassPath.ClassInfo> classes = new ImmutableSet.Builder<ClassPath.ClassInfo>()
                    .addAll(classpath.getTopLevelClasses(Command.class.getPackage().getName()))
                    .addAll(classpath.getTopLevelClasses(MusicCommand.class.getPackage().getName()))
                    .build();
            for (ClassPath.ClassInfo classInfo : classes) {
                Class<?> clazz = classInfo.load();
                if (Modifier.isAbstract(clazz.getModifiers())) continue;
                if (!Command.class.isAssignableFrom(clazz)) continue;
                commands.add((Command) clazz.getDeclaredConstructor().newInstance());
            }
        } catch (IOException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("An error occurred when trying to load commands. Ending process...");
            System.exit(-1);
        }
    }

    Mono onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getContent().isPresent() || !event.getMessage().getAuthorId().isPresent())
            return Mono.empty();
        if (!event.getMessage().getContent().get().startsWith(Bot.prefix)) return Mono.empty();

        String commandName = event.getMessage().getContent().get().split(" ")[0].substring(1).toLowerCase();
        for (Command command : commands) {
            if (command.getNames().contains(commandName) || command.isCommand(event.getMessage())) {
                return command.run(event);
            }
        }

        return Mono.empty();
    }

    Mono<Void> onReady(ReadyEvent event) {
        Bot.schedule(event.getClient());
        return event.getClient().updatePresence(Presence.online(Activity.playing("a random thing")));
    }

    //reset the music player if user leaves channel and bot is the only one in it
    Mono<Snowflake> onVoiceStateUpdate(VoiceStateUpdateEvent event) {
        if (Music.connections.get(event.getCurrent().getGuildId()) == null) return Mono.empty();
        DiscordClient client = event.getClient();

        return Mono.justOrEmpty(event.getOld().flatMap(VoiceState::getChannelId))
                .filterWhen(old -> event.getCurrent().getChannel().hasElement().map(connected -> !connected)) //user left channel
                .filterWhen(old -> isBotInVoiceChannel(client, old))
                .filterWhen(old -> isBotAlone(client, old))
                .doOnNext(ignored -> Music.disconnectBotFromChannel(event.getCurrent().getGuildId()));
    }

    private Mono<Boolean> isBotInVoiceChannel(DiscordClient client, Snowflake voiceChannelId) {
        return client.getChannelById(voiceChannelId)
                .ofType(VoiceChannel.class)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .any(vs -> client.getSelfId().map(vs.getUserId()::equals).orElse(false));
    }

    private Mono<Boolean> isBotAlone(DiscordClient client, Snowflake voiceChannelId) {
        return client.getChannelById(voiceChannelId)
                .ofType(VoiceChannel.class)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .map(it -> it == 1);
    }
}
