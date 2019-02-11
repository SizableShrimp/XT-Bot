package me.sizableshrimp.discordbot;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.Music;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventListener {
    private static Map<String, Command> names = new HashMap<>();
    private static Set<Command> commands = new HashSet<>();

    private EventListener() {
    }

    static {
        Reflections reflections = new Reflections(EventListener.class.getPackage().getName());
        Set<Class<? extends Command>> set = reflections.getSubTypesOf(Command.class);
        for (Class<? extends Command> clazz : set) {
            try {
                if (Modifier.isAbstract(clazz.getModifiers())) continue;
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
        if (!event.getMessage().getContent().isPresent() || !event.getMessage().getAuthor().isPresent())
            return Mono.empty();

        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());
        Command command = null;
        if (event.getMessage().getContent().get().startsWith(prefix)) {
            String commandName = Util.getCommandName(event.getMessage());
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
    static Mono<Snowflake> onVoiceChannelLeave(VoiceStateUpdateEvent event) {
        if (Music.connections.get(event.getCurrent().getGuildId()) == null) return Mono.empty();
        if (!event.getOld().isPresent()) return Mono.empty(); //not possible for user to have left channel
        Snowflake guildId = event.getCurrent().getGuildId();

        return Mono.justOrEmpty(event.getOld().flatMap(VoiceState::getChannelId))
                .filterWhen(old -> event.getCurrent().getChannel().hasElement().map(connected -> !connected)) //user left channel
                .doOnNext(old -> Music.getGuildManager(event.getClient(), guildId).usersSkipping.remove(event.getCurrent().getUserId()))
                .filterWhen(old -> Util.isBotInVoiceChannel(event.getClient(), old))
                .filterWhen(old -> Util.isBotAlone(event.getClient(), guildId))
                .doOnNext(ignored -> Music.disconnectBotFromChannel(event.getCurrent().getGuildId()));
    }

    public static Map<String, Command> getCommandNameMap() {
        return new HashMap<>(names);
    }

    public static Set<Command> getCommands() {
        return new HashSet<>(commands);
    }
}
