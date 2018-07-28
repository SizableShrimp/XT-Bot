package me.sizableshrimp.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class Music {
  private final AudioPlayerManager playerManager;
  protected final Map<Long, GuildMusicManager> musicManagers;
  public static final int DEFAULT_VOLUME = 35;
  protected static HashMap<AudioPlayer, IGuild> runningPlayers = new HashMap<AudioPlayer, IGuild>();
  public final HashMap<GuildMusicManager, Integer> wantsToSkip = new HashMap<GuildMusicManager, Integer>();
  public final HashMap<GuildMusicManager, Integer> neededToSkip = new HashMap<GuildMusicManager, Integer>();

  public Music() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
    playerManager.registerSourceManager(new BandcampAudioSourceManager());
    playerManager.registerSourceManager(new VimeoAudioSourceManager());
    playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
    playerManager.registerSourceManager(new HttpAudioSourceManager());
    playerManager.registerSourceManager(new LocalAudioSourceManager());
  }

  synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
    long guildId = guild.getLongID();
    GuildMusicManager musicManager = musicManagers.get(guildId);
    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager, guild);
      musicManager.player.setVolume(DEFAULT_VOLUME);
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
        EventListener.sendMessage("I could not find a song that contained: " + trackUrl, channel);
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        EventListener.sendMessage("An error occured while trying to play the song. Please try again later.", channel);
        exception.printStackTrace();
      }
    });
  }

  private void play(IGuild guild, GuildMusicManager musicManager, AudioTrack track) {
//    connectToFirstVoiceChannel(guild.getAudioManager());
	runningPlayers.put(musicManager.scheduler.player, guild);
    musicManager.scheduler.queue(track);
  }

  protected void skipTrack(IChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();
  }
}
