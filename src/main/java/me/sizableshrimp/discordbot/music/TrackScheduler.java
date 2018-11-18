package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.Discord4J;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
	private boolean repeating = false;
    private final AudioPlayer player;
    final BlockingQueue<AudioTrack> queue;
	private final IGuild guild;
	private Music music;

    TrackScheduler(AudioPlayer player, IGuild guild, Music music) {
		this.player = player;
		this.guild = guild;
		this.queue = new LinkedBlockingQueue<>();
		this.music = music;
	}

//    void queue(AudioTrack track, IChannel channel) {
//		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
//		// something is playing, it returns false and does nothing. In that case the player was already playing so this
//		// track goes to the queue instead.
//		boolean isPlaying = player.startTrack(track, true);
//		if (!isPlaying) {
//			queue.offer(track);
//			EventListener.sendMessage("`"+track.getInfo().title+"` added to queue.", channel);
//        } else {
//			EventListener.sendMessage("Now playing `"+track.getInfo().title+"`\n"+track.getInfo().uri, channel);
//        }
//    }

    void queue(AudioTrack track, IChannel channel, boolean validURL) {
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		boolean isPlaying = player.startTrack(track, true);
		if (!isPlaying) {
			queue.offer(track);
			if (validURL) {
				EventListener.sendMessage("`"+track.getInfo().title+"` added to queue.\n<"+track.getInfo().uri+">", channel);
			} else {
				EventListener.sendMessage("`"+track.getInfo().title+"` added to queue.\n"+track.getInfo().uri+"", channel);
			}
        } else {
			if (validURL) {
				EventListener.sendMessage("Now playing `"+track.getInfo().title+"`\n<"+track.getInfo().uri+">", channel);
			} else {
				EventListener.sendMessage("Now playing `"+track.getInfo().title+"`\n"+track.getInfo().uri, channel);
			}
        }
    }

    void nextTrack() {
		// Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the player.
		player.startTrack(queue.poll(), false);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason.mayStartNext) {
			if (repeating) {
				player.startTrack(track.makeClone(), false);
			} else {
				nextTrack();
			}
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		if (guild.getConnectedVoiceChannel() == null) {
            Discord4J.LOGGER.warn("Music track is trying to play while bot is not connected to a voice channel.");
			return;
		}
        GuildMusicManager manager = music.getGuildMusicManager(guild);
        manager.neededToSkip = (int) Math.ceil((guild.getConnectedVoiceChannel().getConnectedUsers().size() - 1) / 2D);
		manager.usersSkipping.clear();
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

    void setRepeating(boolean isRepeating) {
		repeating = isRepeating;
	}

    boolean isRepeating() {
		return repeating;
	}
}
