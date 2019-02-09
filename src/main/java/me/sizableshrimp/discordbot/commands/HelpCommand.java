package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.EventListener;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.music.commands.AbstractMusicCommand;
import me.sizableshrimp.discordbot.music.commands.MusicCommand;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpCommand extends Command {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname% [command]",
                "Use `%prefix%help [command]` to find out more information about each command.");
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("help").collect(Collectors.toSet());
    }

    @Override
    public boolean isCommand(Message message) {
        return message.getUserMentionIds().contains(message.getClient().getSelfId().orElse(Snowflake.of(0)));
    }

    @Override
    public Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (args.length != 1) return displayHelp(event);
        String inputCmd = args[0].toLowerCase();
        Command selected = EventListener.getCommandNameMap().get(inputCmd);
        if (selected instanceof MusicCommand) return MusicCommand.displayMusicCommand(event);
        if (selected == null || selected instanceof AbstractMusicCommand) return displayHelp(event);
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(display(event, inputCmd, selected), c));
    }

    public static Consumer<EmbedCreateSpec> display(MessageCreateEvent event, Command command) {
        return display(event, Util.getCommandName(event.getMessage()), command);
    }

    public static Consumer<EmbedCreateSpec> display(MessageCreateEvent event, String inputCmd, Command command) {
        return display(event, inputCmd, command.getClass().getSimpleName().replace("Command", ""), command.getInfo());
    }

    private static Consumer<EmbedCreateSpec> display(MessageCreateEvent event, String inputCmd, String commandName, CommandInfo commandInfo) {
        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());

        String usage = commandInfo.getUsage().replace("%cmdname%", inputCmd);
        String description = commandInfo.getDescription().replace("%prefix%", prefix);
        String title = commandName + " Command";

        return embed -> {
            embed.setColor(new Color(255, 175, 175));
            embed.setAuthor(title, null, null);
            embed.addField("Usage", "`" + usage + "`", false);
            embed.addField("Description", description, false);
        };
    }

    private Mono<Message> displayHelp(MessageCreateEvent event) {
        List<String> aliases = EventListener.getCommands()
                .stream()
                .filter(cmd -> !(cmd instanceof AbstractMusicCommand))
                .flatMap(cmd -> cmd.getNames().stream())
                .collect(Collectors.toList());
        aliases.add("music");
        String commandNames = String.join(", ", aliases);
        Consumer<EmbedCreateSpec> spec = display(event, "help", "Help", getInfo())
                .andThen(embed -> embed.addField("Commands", commandNames, false));
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
