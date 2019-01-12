package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.Bot;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
	private boolean repeating = false;
	private final AudioPlayer player;
	public final BlockingQueue<AudioTrack> queue;
	private final Snowflake guildId;

	TrackScheduler(AudioPlayer player, Snowflake guildId) {
		this.player = player;
		this.guildId = guildId;
		this.queue = new LinkedBlockingQueue<>();
	}

	Mono<Message> queue(AudioTrack track, TextChannel channel, boolean validURL) {
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		boolean isPlaying = player.startTrack(track, true);
		if (!isPlaying) {
			queue.offer(track);
			if (validURL) {
				return Bot.sendMessage("`"+track.getInfo().title+"` added to queue.\n<"+track.getInfo().uri+">", channel);
			} else {
				return Bot.sendMessage("`"+track.getInfo().title+"` added to queue.\n"+track.getInfo().uri+"", channel);
			}
		} else {
			if (validURL) {
				return Bot.sendMessage("Now playing `"+track.getInfo().title+"`\n<"+track.getInfo().uri+">", channel);
			} else {
				return Bot.sendMessage("Now playing `"+track.getInfo().title+"`\n"+track.getInfo().uri, channel);
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
		Music.getBotConnectedVoiceChannel(guildId).hasElement()
				.doOnNext(b -> {
					if (b) {
						GuildMusicManager manager = Music.getGuildManager(guildId);
						manager.wantsToSkip = 0;
						manager.neededToSkip = Music.getBotConnectedVoiceChannel(guildId)
								.map(Optional::get)
								.flatMapMany(VoiceChannel::getVoiceStates)
								.count()
								.map(number -> (int) Math.round((number - 1) / 2D));
						manager.usersSkipping.clear();
					} else {
						System.out.println("Music track is trying to play while bot is not connected to a voice channel. Stopping track.");
						player.stopTrack();
					}
				}).subscribe();
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
