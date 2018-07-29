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

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
	private AudioTrack lastTrack;
	private boolean repeating = false;
	protected final AudioPlayer player;
	protected final BlockingQueue<AudioTrack> queue;
	private final IGuild guild;

	/**
	 * @param player The audio player this scheduler uses
	 */
	public TrackScheduler(AudioPlayer player, IGuild guild) {
		this.player = player;
		this.guild = guild;
		this.queue = new LinkedBlockingQueue<>();
	}

	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 *
	 * @param track The track to play or add to queue.
	 */
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

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
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
		Music music = new Music();
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
