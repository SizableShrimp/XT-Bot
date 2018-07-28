package me.sizableshrimp.discordbot.music;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.sizableshrimp.discordbot.EventListener;
import me.sizableshrimp.discordbot.XTBot;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class MusicEvents {
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		Music music = new Music();
		GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
		AudioPlayer player = manager.player;
		TrackScheduler scheduler = manager.scheduler;
		if (message.startsWith(XTBot.prefix+"play")) {
			if (message.split(" ").length != 2) {
				EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"play [song]```", event.getChannel());
				return;
			}
			IVoiceChannel channel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
			String query = message.split(" ")[1];
			if (!query.startsWith("http")) query = "ytsearch:"+query;
			if (event.getGuild().getConnectedVoiceChannel() != null && event.getGuild().getConnectedVoiceChannel() == channel) {
				music.loadAndPlay(event.getChannel(), channel, query);
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() == null && channel == null) {
				EventListener.sendMessage("Join a voice channel if you want me to play a song!", event.getChannel());
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() != null && channel != event.getGuild().getConnectedVoiceChannel()){
				EventListener.sendMessage("Join "+event.getGuild().getConnectedVoiceChannel()+" to add a song to the queue.", event.getChannel());
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() == null & channel != null) {
				music.loadAndPlay(event.getChannel(), channel, query);
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"volume") || message.startsWith(XTBot.prefix+"vol")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (message.split(" ").length == 2) { 
					try {
						Integer.valueOf(message.split(" ")[1]);
					} catch (NumberFormatException exception) {
						EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
						return;
					}
					Integer volume = Integer.valueOf(message.split(" ")[1]);
					if (volume < 0 || volume > 100) {
						EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
						return;
					}
					volume = Math.max(0, Math.min(100, volume));
					player.setVolume(volume);
					EventListener.sendMessage("Volume set to **"+volume.toString()+"%**", event.getChannel());
					return;
				} else {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"volume [new volume]```", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"pause")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (player.isPaused()) {
					player.setPaused(false);
					EventListener.sendMessage("Music resumed.", event.getChannel());
					return;
				} else {
					player.setPaused(true);
					EventListener.sendMessage("Music paused.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"resume")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (player.isPaused()) {
					player.setPaused(false);
					EventListener.sendMessage("Music resumed.", event.getChannel());
					return;
				} else {
					EventListener.sendMessage("The music is currently playing.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"clear")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				scheduler.queue.clear();
				EventListener.sendMessage("Queue cleared.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"skip")) {
			Integer wants = music.wantsToSkip.get(manager);
			Integer needed = music.neededToSkip.get(manager);
			if (wants+1 == needed) {
				music.skipTrack(event.getChannel());
				music.wantsToSkip.remove(manager);
				music.neededToSkip.remove(manager);
				EventListener.sendMessage("Skipped song.", event.getChannel());
				return;
			}
			music.wantsToSkip.put(manager, wants+1);
			EventListener.sendMessage(wants.toString()+"/"+needed.toString()+" people have requested to skip this song.", event.getChannel());
			return;
		}
		if (message.startsWith(XTBot.prefix+"forceskip")) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS)) {
				music.skipTrack(event.getChannel());
				music.wantsToSkip.remove(manager);
				music.neededToSkip.remove(manager);
				EventListener.sendMessage("Skipped song.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"queue") || message.startsWith(XTBot.prefix+"q")) {
			AudioTrack playing = player.getPlayingTrack();
			BlockingQueue<AudioTrack> queue = scheduler.queue;
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Queue");
			if (playing == null) {
				embed.appendDesc("There is currently nothing playing.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			embed.appendDesc("**Now Playing**\n"+playing.getInfo().title+" - "+playing.getInfo().author);
			embed.appendDesc("\n");
			if (queue.isEmpty()) {
				embed.appendDesc("\nThere is currently nothing in the queue.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			Integer number = 1;
			for (AudioTrack track : queue) {
				embed.appendDesc("\n"+number.toString()+". "+track.getInfo().title+" - "+track.getInfo().author);
			}
			EventListener.sendEmbed(embed, event.getChannel());
			return;
		}
		if (message.startsWith(XTBot.prefix+"nowplaying") || message.startsWith(XTBot.prefix+"np")) {
			AudioTrack playing = player.getPlayingTrack();
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Now Playing");
			if (playing == null) {
				embed.appendDesc("There is currently nothing playing.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			embed.appendDesc(playing.getInfo().title+" - "+playing.getInfo().author);
			EventListener.sendEmbed(embed, event.getChannel());
			return;
		}
		if (message.startsWith(XTBot.prefix+"loop")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (scheduler.isRepeating()) {
					scheduler.setRepeating(false);
					EventListener.sendMessage("Loop stopped.", event.getChannel());
					return;
				} else {
					scheduler.setRepeating(true);
					EventListener.sendMessage("Song looped.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"shuffle")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Collections.shuffle((List<?>) scheduler.queue);
				EventListener.sendMessage("Queue shuffled.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
	}

	boolean isOne(MessageReceivedEvent event) {
		if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
			IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
			if (state.getChannel() != null && state.getChannel().getConnectedUsers().size() == 2) {
				List<IUser> users = state.getChannel().getConnectedUsers();
				if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) return true;
			}
		}
		return false;
	}

}