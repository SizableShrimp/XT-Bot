package me.sizableshrimp.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.Music;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventListener {
    private static Map<String, Command> names = new HashMap<>();
    private static Set<Command> commands = new HashSet<>();

    private EventListener() {}

    static {
        Reflections reflections = new Reflections(EventListener.class.getPackage().getName());
        Set<Class<? extends Command>> set = reflections.getSubTypesOf(Command.class);
        for (Class<? extends Command> clazz : set) {
            try {
                Command command = clazz.getDeclaredConstructor().newInstance();
                commands.add(command);
                command.getNames().forEach(name -> names.put(name, command));
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                System.out.println("An error occurred when trying to load commands. Ending process...");
                System.exit(-1);
            }
        }
    }

    static Mono onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getContent().isPresent() || !event.getMessage().getAuthorId().isPresent())
            return Mono.empty();
        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());
        Command command = null;
        if (event.getMessage().getContent().get().startsWith(prefix)) {
            String content = event.getMessage().getContent().get();
            String commandName = content.substring(1).split(" ")[0].toLowerCase();
            command = names.get(commandName);
        }
        if (command == null) {
            command = commands.stream()
                    .filter(cmd -> cmd.isCommand(event.getMessage()))
                    .findAny().orElse(null);
        }
        if (command != null) return command.run(event);
        return Mono.empty();
    }

    //reset the music player if user leaves channel and bot is the only one in it
    static Mono<Snowflake> onVoiceStateUpdate(VoiceStateUpdateEvent event) {
        if (Music.connections.get(event.getCurrent().getGuildId()) == null) return Mono.empty();
        DiscordClient client = event.getClient();

        return Mono.justOrEmpty(event.getOld().flatMap(VoiceState::getChannelId))
                .filterWhen(old -> event.getCurrent().getChannel().hasElement().map(connected -> !connected)) //user left channel
                .filterWhen(old -> Util.isBotInVoiceChannel(client, old))
                .filterWhen(old -> Util.isBotAlone(client, old))
                .doOnNext(ignored -> Music.disconnectBotFromChannel(event.getClient(), event.getCurrent().getGuildId()));
    }
}
