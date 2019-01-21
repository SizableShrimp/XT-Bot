package me.sizableshrimp.discordbot.music.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.commands.Command;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicCommand extends Command {
    @Override
    public Set<String> getNames() {
        return Stream.of("music").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        String prefix = Bot.getPrefix(event.getClient(), event.getGuildId().get());
        return event.getMessage().getChannel().flatMap(c -> sendMessage("I can play music! My music commands are:```[] - required   () - optional  * - DJs or people alone with bot only  ** - DJs only\n" +
                prefix + "play [song] - Plays the song that you request.\n" +
                "* " + prefix + "volume (new volume) or " + prefix + "vol (new volume) - Changes the volume or tells the current volume.\n" +
                "* " + prefix + "pause or " + prefix + "p - Pauses/resumes the song.\n" +
                prefix + "queue or " + prefix + "q - Shows what is currently playing and what is queued up to go next.\n" +
                "* " + prefix + "clear - Clears all the queued music.\n" +
                prefix + "nowplaying or " + prefix + "np - Shows what is currently playing.\n" +
                "* " + prefix + "remove [number in queue to remove] - Removes the song in the queue at the number given.\n" +
                //"* " + prefix + "rewind - Rewinds the song by 10 seconds.\n" +
                //"* " + prefix + "fastforward or " + prefix + "ff - Fast forwards the song by 10 seconds.\n" +
                "* " + prefix + "goto [time in song] - Starts playing from a certain point in the song.\n" +
                //TODO work in progress  //"* " + prefix + "goto song [number in queue] - Goes to the specified song in the queue\n" +
                //prefix + "skip - Requests to skip the song. If enough people have voted to skip, the next song will be played.\n" +
                //"* " + prefix + "forceskip - Forcefully skips to the next song.\n" +
                "* " + prefix + "disconnect or " + prefix + "leave - Disconnects from the voice channel and stops playing music.\n" +
                "* " + prefix + "loop - Puts the song currently playing on/off repeat.\n" +
                "** " + prefix + "lock - Locks the bot to DJs only.```" +
                "If you are the only person in a voice channel with XT Bot, you may use normal commands and commands that have a * at the beginning. If you are a DJ (have the **Manage Channels** permission or a role called \"DJ\"), you may use all commands on this list.\n" +
                "*Note: You do not need to include brackets or parenthesis when using commands*", c));
    }
}
