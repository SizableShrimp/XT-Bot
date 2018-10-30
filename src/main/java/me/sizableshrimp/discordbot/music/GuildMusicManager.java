package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.List;

class GuildMusicManager {
    AudioPlayer player;
    TrackScheduler scheduler;
    int neededToSkip;
    int wantsToSkip;
    final List<IUser> usersSkipping = new ArrayList<>();

    GuildMusicManager(AudioPlayerManager manager, IGuild guild, Music music) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild, music);
    player.addListener(scheduler);
  }

    AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}
