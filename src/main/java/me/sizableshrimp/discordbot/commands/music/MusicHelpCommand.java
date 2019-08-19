package me.sizableshrimp.discordbot.commands.music;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.commands.utility.HelpCommand;
import me.sizableshrimp.discordbot.listeners.MessageListener;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MusicHelpCommand extends MusicCommand {
    @Override
    public CommandInfo getInfo() {
        return getStaticInfo();
    }

    private static CommandInfo getStaticInfo() {
        return new CommandInfo("%cmdname% [command]", "Use `%prefix%music [command]` to find out more information about each music command.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return Set.of();
    }

    @Override
    public Set<String> getNames() {
        return Set.of("music");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (args.length != 1) {
            return displayMusicHelpCommand(event);
        }
        String inputCmd = args[0].toLowerCase();
        if (inputCmd.equals("music")) {
            return displayMusicHelpCommand(event);
        }
        Command selected = MessageListener.getCommandNameMap().get(inputCmd);
        if (!(selected instanceof MusicCommand)) {
            return displayMusicHelpCommand(event); //automatically checks null
        }
        Consumer<EmbedCreateSpec> spec = displayMusic(inputCmd, (MusicCommand) selected);
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }

    static Consumer<EmbedCreateSpec> displayMusic(String inputCmd, MusicCommand command) {
        String name = command.getClass().getSimpleName().replace("Command", "");
        return displayMusic(inputCmd, name, command);
    }

    private static Consumer<EmbedCreateSpec> displayMusic(String inputCmd, String name, MusicCommand command) {
        Set<MusicPermission> permissions = command.getRequiredPermissions();

        String title = name + " Command - Music";
        String perms = permissions == null ? null :
                String.join(", ", permissions.stream().map(Enum::toString).collect(Collectors.toSet()));

        return HelpCommand.display(inputCmd, command)
                .andThen(embed -> {
                    embed.setColor(new Color(15, 255, 145));
                    embed.setAuthor(title, null, null);
                    if (perms != null) {
                        embed.addField("Required Perms", perms, false);
                    }
                });
    }

    public static Mono<Message> displayMusicHelpCommand(MessageCreateEvent event) {
        List<String> aliases = MessageListener.getCommands()
                .stream()
                .filter(cmd -> cmd instanceof MusicCommand)
                .flatMap(cmd -> cmd.getNames().stream())
                .collect(Collectors.toList());
        String commandNames = String.join(", ", aliases);
        String permissions = "DJ - Can use all commands, moderator of music\n" +
                "ALONE - Can use most commands ONLY when alone with the bot in a voice channel\n" +
                "NONE - No permissions required and can use anytime";
        Consumer<EmbedCreateSpec> spec = displayMusic("music", "Music", (MusicCommand) MessageListener.getCommandNameMap().get("music"))
                .andThen(embed -> {
                    embed.addField("Permissions", permissions, false);
                    embed.addField("Commands", commandNames, false);
                });
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
