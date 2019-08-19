package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.Value;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.commands.utility.HelpCommand;
import reactor.core.publisher.Mono;

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

    /**
     * Used to run a command on conditions other than name, like tagging the bot, etc.
     *
     * @param message The message to use as an input.
     * @return True if the command meets a condition to run other than a matching name. False otherwise. (This should
     * not check if the command name or aliases match.)
     */
    public boolean isCommand(Message message) {
        return false;
    }

    public Mono run(MessageCreateEvent event) {
        if (event.getMessage().getContent().isEmpty() || event.getMember().isEmpty()) {
            return Mono.empty();
        }
        String[] args = event.getMessage().getContent().get().split(" ");
        args = Arrays.copyOfRange(args, 1, args.length);
        return run(event, args);
    }

    protected abstract Mono run(MessageCreateEvent event, String[] args);


    //helper functions
    protected Mono<Message> incorrectUsage(MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(HelpCommand.display(event, this), c));
    }

    protected static Mono<Message> sendMessage(String message, MessageChannel channel) {
        return Util.sendMessage(message, channel);
    }

    protected static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return Util.sendEmbed(embed, channel);
    }

    protected static Mono<Message> sendMessage(String message, Consumer<? super EmbedCreateSpec> embed, MessageChannel channel) {
        return Util.sendEmbed(message, embed, channel);
    }
}
