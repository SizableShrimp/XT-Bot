package me.sizableshrimp.discordbot.commands.music;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.EventListener;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.commands.Command;
import me.sizableshrimp.discordbot.music.MusicPermission;
import reactor.core.publisher.Mono;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicCommand extends AbstractMusicCommand {
    @Override
    public CommandInfo getInfo() {
        return getStaticInfo();
    }

    private static CommandInfo getStaticInfo() {
        return new CommandInfo("music [command]", "Music section of the bot.");
    }

    @Override
    public Set<MusicPermission> getRequiredPermissions() {
        return null;
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("music").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (args.length != 1) return displayMusicCommand(event);
        String inputCmd = args[0].toLowerCase();
        if (inputCmd.equals("music")) return displayMusicCommand(event);
        Command selected = EventListener.getCommandNameMap().get(inputCmd);
        if (!(selected instanceof AbstractMusicCommand)) return displayMusicCommand(event); //automatically checks null
        Consumer<EmbedCreateSpec> spec = displayMusic(event, inputCmd, (AbstractMusicCommand) selected);
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }

    static Consumer<EmbedCreateSpec> displayMusic(MessageCreateEvent event, AbstractMusicCommand command) {
        return displayMusic(event, Util.getCommandName(event.getMessage()), command);
    }

    private static Consumer<EmbedCreateSpec> displayMusic(MessageCreateEvent event, String inputCmd, AbstractMusicCommand command) {
        return displayMusic(event, inputCmd, command.getClass().getSimpleName().replace("Command", ""),
                command.getInfo(), command.getRequiredPermissions());
    }

    private static Consumer<EmbedCreateSpec> displayMusic(MessageCreateEvent event, String inputCmd, String commandName,
                                                          CommandInfo commandInfo, Set<MusicPermission> permissions) {
        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());

        String usage = commandInfo.getUsage().replace("%cmdname%", inputCmd);
        String description = commandInfo.getDescription().replace("%prefix%", prefix);
        String title = commandName + " Command - Music";
        String perms = permissions == null ? null :
                String.join(", ", permissions.stream().map(Enum::toString).collect(Collectors.toSet()));


        return embed -> {
            embed.setColor(new Color(15, 255, 145));
            embed.setAuthor(title, null, null);
            embed.addField("Usage", "`" + usage + "`", false);
            embed.addField("Description", description, false);
            if (perms != null) embed.addField("Required Perms", perms, false);
        };
    }

    public static Mono<Message> displayMusicCommand(MessageCreateEvent event) {
        List<String> aliases = EventListener.getCommands()
                .stream()
                .filter(cmd -> cmd instanceof AbstractMusicCommand)
                .flatMap(cmd -> cmd.getNames().stream())
                .collect(Collectors.toList());
        String commandNames = String.join(", ", aliases);
        String permissions = "DJ - Can use all commands, moderator of music\n" +
                "ALONE - Can use most commands ONLY when alone with the bot in a voice channel\n" +
                "NONE - No permissions required and can use anytime";
        Consumer<EmbedCreateSpec> spec = displayMusic(event, "music", "Music", getStaticInfo(), null)
                .andThen(embed -> {
                    embed.addField("Permissions", permissions, false);
                    embed.addField("Commands", commandNames, false);
                });
        return event.getMessage().getChannel().flatMap(c -> sendEmbed(spec, c));
    }
}
