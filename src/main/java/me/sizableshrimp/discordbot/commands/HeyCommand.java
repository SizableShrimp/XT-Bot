package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeyCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("hey").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel().flatMap(c -> sendMessage("Hello! :smile:", c));
    }

}
