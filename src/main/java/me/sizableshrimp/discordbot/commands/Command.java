package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.music.Music;
import me.sizableshrimp.discordbot.music.MusicPermissions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

public abstract class Command {
    /**
     * Check if a message is a command of this type and is runnable/valid
     * @param message The {@link Message} from a {@link MessageCreateEvent}
     * @return A boolean if the message is a command of this type
     */
    public boolean isCommand(Message message) {
        return false;
    }

    public Mono run(MessageCreateEvent event) {
        String[] args = event.getMessage().getContent().orElse("").split(" ");
        args = Arrays.copyOfRange(args, 1, args.length);
        return run(event, args);
    }

    public abstract Set<String> getNames();

    protected abstract Mono run(MessageCreateEvent event, String[] args);

    public String getUsage() {
        return "";
    }



    //helper functions
    protected Mono<Message> incorrectUsage(MessageCreateEvent event) {
        return event.getMessage().getChannel()
                .flatMap(c -> sendMessage("Incorrect usage. Please use: ```" + Bot.prefix + getUsage() + "```", c));
    }

    protected static Mono<Message> sendMessage(String string, MessageChannel channel) {
        return Bot.sendMessage(string, channel);
    }

    protected static Mono<Message> sendEmbed(EmbedCreateSpec embed, MessageChannel channel) {
        return Bot.sendEmbed(embed, channel);
    }

    protected static Mono<Void> deleteLater(int seconds, Message... messages) {
        Mono<Void> delete = Flux.just(messages).flatMap(Message::delete).then();
        return Mono.delay(Duration.ofSeconds(seconds)).then(delete);
    }

    protected static Mono<TextChannel> filterLockedAndPermissions(MessageCreateEvent event, MusicPermissions... permissions) {
        return filterLocked(event)
                .filterWhen(c -> MusicPermissions.requirePermissions(event.getMember().get(), c, permissions));
    }

    protected static Mono<TextChannel> filterLocked(MessageCreateEvent event) {
        return event.getMessage().getChannel().ofType(TextChannel.class)
                .filterWhen(c -> event.getMessage().getAuthorAsMember().flatMap(member -> Music.locked(member, c)).map(b -> !b));
    }
}
