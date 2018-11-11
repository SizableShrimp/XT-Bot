package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class Music {
	private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    final int DEFAULT_VOLUME = 35;

    Music() {
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		playerManager.registerSourceManager(new YoutubeAudioSourceManager());
		playerManager.registerSourceManager(new HttpAudioSourceManager());
		playerManager.registerSourceManager(new LocalAudioSourceManager());
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
	}

    synchronized GuildMusicManager getGuildMusicManager(IGuild guild) {
		long guildId = guild.getLongID();
		GuildMusicManager musicManager = musicManagers.get(guildId);
		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager, guild, this);
			musicManager.player.setVolume(DEFAULT_VOLUME);
			musicManagers.put(guildId, musicManager);
		}
		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());
		return musicManager;
	}

    void loadAndPlay(final IChannel channel, final String trackUrl, boolean validURL) {
        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				play(channel, musicManager, track, validURL);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack firstTrack = playlist.getSelectedTrack();
				if (firstTrack == null) firstTrack = playlist.getTracks().get(0);
				play(channel, musicManager, firstTrack, validURL);
			}

			@Override
			public void noMatches() {
				EventListener.sendMessage("I could not find a song that contained: " + trackUrl, channel);
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				exception.printStackTrace();
                EventListener.sendMessage("An error occurred while trying to play the song. Please try again later.", channel);
			}
		});
	}

    private void play(IChannel channel, GuildMusicManager musicManager, AudioTrack track, boolean validURL) {
		if (TimeUnit.MILLISECONDS.toSeconds(track.getDuration()) > TimeUnit.HOURS.toSeconds(10)) {
			EventListener.sendMessage(":x: Cannot play a song over 10 hours in length.", channel);
			return;
		}
		musicManager.scheduler.queue(track, channel, validURL);
	}
}
