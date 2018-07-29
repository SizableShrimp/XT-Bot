package me.sizableshrimp.discordbot.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class TrackScheduler extends AudioEventAdapter {
	private AudioTrack lastTrack;
	private boolean repeating = false;
	protected final AudioPlayer player;
	protected final BlockingQueue<AudioTrack> queue;
	private final IGuild guild;
	Music music;

	public TrackScheduler(AudioPlayer player, IGuild guild, Music music) {
		this.player = player;
		this.guild = guild;
		this.queue = new LinkedBlockingQueue<>();
		this.music = music;
	}

	public void queue(AudioTrack track, IChannel channel) {
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		if (!player.startTrack(track, true)) {
			queue.offer(track);
			EventListener.sendMessage(track.getInfo().title+" added to queue.", channel);
			return;
		}
		EventListener.sendMessage("Now playing "+track.getInfo().title, channel);
	}

	public void nextTrack() {
		// Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the player.
		player.startTrack(queue.poll(), false);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		lastTrack = track;
		if (endReason.mayStartNext) {
			if (repeating) {
				player.startTrack(lastTrack.makeClone(), false);
			} else {
				nextTrack();
			}
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		music.wantsToSkip.put(music.musicManagers.get(guild.getLongID()), 0);
		Long number = Math.round(guild.getConnectedVoiceChannel().getConnectedUsers().size()/2D);
		music.neededToSkip.put(music.musicManagers.get(guild.getLongID()), number.intValue());
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {

	}

	@Override
	public void onPlayerResume(AudioPlayer player) {

	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {

	}
	
	public void setRepeating(boolean isRepeating) {
		repeating = isRepeating;
	}
	
	public boolean isRepeating() {
		return repeating;
	}
}
