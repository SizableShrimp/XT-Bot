package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager {
  public AudioPlayer player;
  public TrackScheduler scheduler;

  public GuildMusicManager(AudioPlayerManager manager, IGuild guild) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild);
    player.addListener(scheduler);
  }

  public AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}
