package me.sizableshrimp.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class Util {
    private Util() {}

    public static Mono<Message> sendMessage(String string, MessageChannel channel) {
        return channel.createMessage("\u200B" + string);
    }

    public static Mono<Message> sendEmbed(Consumer<? super EmbedCreateSpec> spec, MessageChannel channel) {
        return channel.createMessage(message -> message.setEmbed(spec));
    }

    /**
     * Returns a lowercase {@code String} representation of the command in the specified message, if present.
     * The returned {@code String} is empty if the message content is empty or the message does not start with the prefix of the guild.
     *
     * @param message The {@link Message} used to find the command name.
     * @return The command, if present.
     */
    public static String getCommandName(Message message, Snowflake guildId) {
        String prefix = Bot.getPrefix(message.getClient(), guildId);
        return getCommandName(message, prefix);
    }

    /**
     * Returns a lowercase {@code String} representation of the command in the specified message, if present.
     * The returned {@code String} is empty if the message content is empty or the message does not start with the prefix.
     *
     * @param message The {@link Message} used to find the command name.
     * @param prefix The prefix.
     * @return The command, if present.
     */
    public static String getCommandName(Message message, String prefix) {
        return message.getContent()
                .filter(content -> content.startsWith(prefix))
                .map(content -> content.substring(prefix.length()).split(" ")[0].toLowerCase())
                .orElse("");
    }

    public static Mono<Boolean> isBotInVoiceChannel(DiscordClient client, Snowflake voiceChannelId) {
        return client.getChannelById(voiceChannelId)
                .ofType(VoiceChannel.class)
                .flatMap(Util::isBotInVoiceChannel);
    }

    public static Mono<Boolean> isBotInVoiceChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getVoiceStates()
                .any(vs -> voiceChannel.getClient().getSelfId().map(vs.getUserId()::equals).orElse(false));
    }

    public static Mono<Boolean> isBotAlone(DiscordClient client, Snowflake guildId) {
        return client.getSelf()
                .flatMap(u -> u.asMember(guildId))
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .map(count -> count == 1);
    }

    public static Mono<Boolean> isMemberAloneWithBot(Member member) {
        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .filterWhen(Util::isBotInVoiceChannel) //bot is in voice channel
                .flatMapMany(VoiceChannel::getVoiceStates)
                .count()
                .map(it -> it == 2) //includes bot
                .defaultIfEmpty(false);
    }

    /**
     * Gets the Duration until 4:20 Eastern Time
     *
     * @return Duration until 4:20 Eastern Time
     */
    static Duration happy420() {
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime tomorrow = time.withHour(16).withMinute(20).withSecond(0);
        if (time.compareTo(tomorrow) > 0) tomorrow = tomorrow.plusDays(1);
        return Duration.between(time, tomorrow);
    }

    /**
     * Converts the given time to a String representation
     *
     * @param time The time to convert
     * @return A String representation which includes the day, month, and year
     */
    static String getTime(ZonedDateTime time) {
        String ordinal;
        switch (time.getDayOfMonth()) {
            case 1:
            case 21:
            case 31:
                ordinal = "st";
                break;
            case 2:
            case 22:
                ordinal = "nd";
                break;
            case 3:
            case 23:
                ordinal = "rd";
                break;
            default:
                ordinal = "th";
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE, MMMM d'" + ordinal + "', yyyy");
        return format.format(time);
    }
}
