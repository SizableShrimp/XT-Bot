package me.sizableshrimp.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MissingPermissionsException;

public class Music {
  private static final Logger log = LoggerFactory.getLogger(Music.class);
  protected static HashMap<AudioPlayer, IGuild> runningPlayers = new HashMap<AudioPlayer, IGuild>();
  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  public Music() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
    long guildId = guild.getLongID();
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

    return musicManager;
  }

  public void loadAndPlay(final IChannel channel, final String trackUrl) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        play(channel.getGuild(), musicManager, track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }
        play(channel.getGuild(), musicManager, firstTrack);
      }

      @Override
      public void noMatches() {
        sendMessageToChannel(channel, "Nothing found by " + trackUrl);
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        sendMessageToChannel(channel, "Could not play: " + exception.getMessage());
      }
    });
  }

  private void play(IGuild guild, GuildMusicManager musicManager, AudioTrack track) {
//    connectToFirstVoiceChannel(guild.getAudioManager());
	runningPlayers.put(musicManager.scheduler.player, guild);
    musicManager.scheduler.queue(track);
  }

  private void skipTrack(IChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();

    sendMessageToChannel(channel, "Skipped to next track.");
  }

  private void sendMessageToChannel(IChannel channel, String message) {
    try {
      channel.sendMessage(message);
    } catch (Exception e) {
      log.warn("Failed to send message {} to {}", message, channel.getName(), e);
    }
  }

  private static void connectToFirstVoiceChannel(IAudioManager audioManager) {
    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      if (voiceChannel.isConnected()) {
        return;
      }
    }

    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      try {
        voiceChannel.join();
      } catch (MissingPermissionsException e) {
        log.warn("Cannot enter voice channel {}", voiceChannel.getName(), e);
      }
    }
  }
}
