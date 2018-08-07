package me.sizableshrimp.discordbot.music;

import java.util.ArrayList;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class GuildMusicManager {
  public AudioPlayer player;
  public TrackScheduler scheduler;
  public long trackStartTime;
  public int neededToSkip;
  public int wantsToSkip;
  public final List<IUser> usersSkipping = new ArrayList<IUser>();

  public GuildMusicManager(AudioPlayerManager manager, IGuild guild, Music music) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild, music);
    player.addListener(scheduler);
  }

  public AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}
