package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.Value;
import me.sizableshrimp.discordbot.Util;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

public abstract class Command {
    @Value
    protected static class CommandInfo {
        String usage;
        String description;
    }

    public abstract CommandInfo getInfo();

    public abstract Set<String> getNames();

    public boolean isCommand(Message message) {
        return false;
    }

    public Mono run(MessageCreateEvent event) {
        if (!event.getMessage().getContent().isPresent()) return Mono.empty();
        String[] args = event.getMessage().getContent().get().split(" ");
        args = Arrays.copyOfRange(args, 1, args.length);
        return run(event, args);
    }

    protected abstract Mono run(MessageCreateEvent event, String[] args);


    //helper functions
    protected Mono<Message> incorrectUsage(MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(HelpCommand.display(event, this), c));
    }

    protected static Mono<Message> sendMessage(String string, MessageChannel channel) {
        return Util.sendMessage(string, channel);
    }

    protected static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> spec, MessageChannel channel) {
        return Util.sendEmbed(spec, channel);
    }

    protected static Mono<Void> deleteLater(int seconds, Message... messages) {
        Mono<Void> delete = Flux.just(messages).flatMap(Message::delete).then();
        return Mono.delay(Duration.ofSeconds(seconds)).then(delete);
    }
}
