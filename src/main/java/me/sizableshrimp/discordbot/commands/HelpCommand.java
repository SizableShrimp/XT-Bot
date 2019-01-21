package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.Bot;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpCommand extends Command {
    @Override
    public boolean isCommand(Message message) {
        return message.getUserMentionIds().contains(message.getClient().getSelfId().orElse(Snowflake.of(0)));
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("help").collect(Collectors.toSet());
    }

    @Override
    public Mono<Message> run(MessageCreateEvent event, String[] args) {
        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());
        return event.getMessage().getChannel().flatMap(c -> sendMessage("Hello! I am XT Bot. My commands are:\n```"+
                prefix+"hey\n"+
                prefix+"info\n"+
                prefix+"music\n"+
                prefix+"newname\n"+
                prefix+"fortnite or "+prefix+"ftn\n```", c));
    }
}
