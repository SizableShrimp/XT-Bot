package me.sizableshrimp.discordbot.commands.utility;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.commands.music.MusicCommand;
import me.sizableshrimp.discordbot.commands.music.MusicHelpCommand;
import me.sizableshrimp.discordbot.listeners.MessageListener;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HelpCommand extends Command {
    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname% [command]",
                "Use `%prefix%help [command]` to find out more information about each command.");
    }

    @Override
    public Set<String> getNames() {
        return Set.of("help");
    }

    @Override
    public boolean isCommand(Message message) {
        return message.getUserMentionIds().contains(message.getClient().getSelfId().orElse(Snowflake.of(0)));
    }

    @Override
    public Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (args.length != 1) {
            return displayHelp(event);
        }
        String inputCmd = args[0].toLowerCase();
        Command selected = MessageListener.getCommandNameMap().get(inputCmd);
        if (selected instanceof MusicCommand) {
            return MusicHelpCommand.displayMusicHelpCommand(event);
        }
        if (selected == null) {
            return displayHelp(event);
        }
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(display(inputCmd, selected), c));
    }

    public static Consumer<EmbedCreateSpec> display(MessageCreateEvent event, Command command) {
        return display(Util.getCommandName(event.getMessage()), command);
    }

    public static Consumer<EmbedCreateSpec> display(String inputCmd, Command command) {
        String commandName = command.getClass().getSimpleName().replace("Command", "");
        CommandInfo commandInfo = command.getInfo();
        Set<String> names = command.getNames();

        String usage = Bot.getPrefix() + commandInfo.getUsage().replace("%cmdname%", inputCmd);
        String description = commandInfo.getDescription().replace("%prefix%", Bot.getPrefix());
        String title = commandName + " Command";
        String aliases = String.join(", ", names);

        return embed -> {
            embed.setColor(new Color(255, 175, 175));
            embed.setAuthor(title, null, null);
            embed.addField("Usage", "`" + usage + "`", false);
            embed.addField("Description", description, false);
            if (names.size() > 1) {
                embed.addField("Aliases", aliases, false);
            }
        };
    }

    private Mono<Message> displayHelp(MessageCreateEvent event) {
        List<String> aliases = MessageListener.getCommands()
                .stream()
                .filter(cmd -> {
                    if (cmd instanceof MusicHelpCommand) return true;
                    return !(cmd instanceof MusicCommand);
                })
                .flatMap(cmd -> cmd.getNames().stream())
                .collect(Collectors.toList());
        String commandNames = String.join(", ", aliases);
        Consumer<EmbedCreateSpec> spec = display("help", this)
                .andThen(embed -> embed.addField("Commands", commandNames, false));
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
