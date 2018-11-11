package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import sx.blah.discord.handle.obj.IGuild;

import java.util.ArrayList;
import java.util.List;

class GuildMusicManager {
    AudioPlayer player;
    TrackScheduler scheduler;
    int neededToSkip;
    final List<Long> usersSkipping = new ArrayList<>();

    GuildMusicManager(AudioPlayerManager manager, IGuild guild, Music music) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild, music);
    player.addListener(scheduler);
  }

    AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}
