package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager {
  public AudioPlayer player;
  public TrackScheduler scheduler;
  public long trackStartTime;
  public int neededToSkip;
  public int wantsToSkip;

  public GuildMusicManager(AudioPlayerManager manager, IGuild guild, Music music) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild, music);
    player.addListener(scheduler);
  }

  public AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}
