package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class GuildMusicManager {
  public final AudioPlayer player;
  public final TrackScheduler scheduler;
  Mono<Integer> neededToSkip;
  int wantsToSkip;
  final List<Snowflake> usersSkipping = new ArrayList<>();

  public GuildMusicManager(AudioPlayerManager manager, Snowflake guildId, DiscordClient client) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guildId, client);
    player.addListener(scheduler);
  }
}
