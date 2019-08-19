package me.sizableshrimp.discordbot.commands.fun;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.discordbot.commands.Command;
import reactor.core.publisher.Mono;

import java.util.Set;

public class HeyCommand extends Command {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "A small command used to say hello to the bot.");
    }

    @Override
    public Set<String> getNames() {
        return Set.of("hey", "hello");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getMessage().getChannel().flatMap(c -> sendMessage("Hello! :smile:", c));
    }

}
