package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;

import java.util.HashSet;
import java.util.Set;

public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final Set<Snowflake> usersSkipping = new HashSet<>();

    public GuildMusicManager(AudioPlayerManager manager, Snowflake guildId, DiscordClient client) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player, guildId, client);
        player.addListener(scheduler);
    }
}
