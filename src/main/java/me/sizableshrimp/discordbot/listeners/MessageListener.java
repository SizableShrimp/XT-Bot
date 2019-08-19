package me.sizableshrimp.discordbot.listeners;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.loading.CommandLoader;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessageListener extends EventListener<MessageCreateEvent> {
    private static Set<Command> commands;
    private static Map<String, Command> names = new HashMap<>();

    public MessageListener() {
        super(MessageCreateEvent.class);
    }

    static {
        commands = new CommandLoader<>(Command.class).loadClasses();
        for (Command command : commands) {
            command.getNames().forEach(name -> names.put(name, command));
        }
    }

    @Override
    protected Mono<Void> execute(MessageCreateEvent event) {
        String commandName = Util.getCommandName(event.getMessage());
        Command command = names.get(commandName);

        if (command == null) {
            command = commands.stream()
                    .filter(cmd -> cmd.isCommand(event.getMessage()))
                    .findAny().orElse(null);
        }

        if (command != null) {
            return command.run(event).then();
        }

        return Mono.empty();
    }

    @Override
    public void register(EventDispatcher dispatcher) {
        dispatcher.on(type)
                .filterWhen(e -> e.getMessage().getChannel().map(c -> c instanceof GuildMessageChannel))
                .filterWhen(e -> canSendMessages(e.getMessage()))
                .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(false))
                .flatMap(this::execute)
                .onErrorContinue((error, event) ->
                        Bot.LOGGER.error("Event listener had an uncaught exception!", error))
                .subscribe();
    }

    private Mono<Boolean> canSendMessages(Message message) {
        Mono<Snowflake> id = Mono.justOrEmpty(message.getClient().getSelfId());
        return message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(c -> id.flatMap(c::getEffectivePermissions))
                .map(set -> set.asEnumSet().contains(Permission.SEND_MESSAGES));
    }

    public static Map<String, Command> getCommandNameMap() {
        return names;
    }

    public static Set<Command> getCommands() {
        return commands;
    }
}
