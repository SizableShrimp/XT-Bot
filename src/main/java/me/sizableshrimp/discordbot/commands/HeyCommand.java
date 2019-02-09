package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeyCommand extends Command {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "A small command used to say hello to the bot.");
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("hey", "hello").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel().flatMap(c -> sendMessage("Hello! :smile:", c));
    }

}
