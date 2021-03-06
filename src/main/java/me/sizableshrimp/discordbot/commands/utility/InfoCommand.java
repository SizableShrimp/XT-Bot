package me.sizableshrimp.discordbot.commands.utility;

import discord4j.common.GitProperties;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.commands.Command;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class InfoCommand extends Command {

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname%",
                "Displays information about the bot including uptime, author, and how the bot was made.");
    }

    @Override
    public Set<String> getNames() {
        return Set.of("info");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        return event.getClient().getUserById(Snowflake.of(Bot.getConfig().getProperty("OWNER_ID")))
                .map(u -> u.getUsername() + "#" + u.getDiscriminator())
                .map(InfoCommand::getEmbed)
                .zipWith(event.getMessage().getChannel())
                .flatMap(tuple -> sendMessage("To learn my commands, use `" + Bot.getPrefix() + "help`", tuple.getT1(), tuple.getT2()));
    }

    private static Consumer<EmbedCreateSpec> getEmbed(String owner) {
        String description = "This bot was built with [Spring Boot](https://spring.io/projects/spring-boot). It was programmed in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.";

        return embed -> {
            embed.addField("Author", owner, true);
            embed.setAuthor("Information", null, null);
            embed.setDescription(description);
            embed.addField("Discord4J Version",
                    GitProperties.getProperties().getProperty(GitProperties.APPLICATION_VERSION), true);
            embed.addField("Prefix", Bot.getPrefix(), false);
            embed.addField("Uptime", getUptime(), false);
        };
    }

    private static String getUptime() {
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - Bot.getFirstOnline());
        List<String> list = new ArrayList<>();

        if (duration.toDays() > 0) {
            list.add(duration.toDays() == 1 ? duration.toDays() + " day" : duration.toDays() + " days");
        }
        duration = duration.minusDays(duration.toDays());

        if (duration.toHours() > 0) {
            list.add(duration.toHours() == 1 ? duration.toHours() + " hour" : duration.toHours() + " hours");
        }
        duration = duration.minusHours(duration.toHours());

        if (duration.toMinutes() > 0) {
            list.add(duration.toMinutes() == 1 ? duration.toMinutes() + " minute" : duration.toMinutes() + " minutes");
        }
        duration = duration.minusMinutes(duration.toMinutes());

        if (duration.getSeconds() > 0) {
            list.add(duration.getSeconds() == 1 ? duration.getSeconds() + " second" : duration.getSeconds() + " seconds");
        }

        if (list.isEmpty()) {
            return "Less than a second";
        }

        return String.join(", ", list);
    }
}
