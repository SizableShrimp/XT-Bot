package me.sizableshrimp.discordbot.music;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

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
	protected boolean songLooped = false;
	protected boolean queueLooped = false;
	//TODO add volume command (for admins or 1 person in channel) DONE
	//TODO add pause command (for admins or 1 person in channel) DONE
	//TODO add clear queue command (for admins or 1 person in channel) DONE
	//TODO add resume command (for admins or 1 person in channel) DONE
	//TODO add loop command (for admins or 1 person in channel) DONE
	//TODO add shuffle command (for admins or 1 person in channel) DONE
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		if (message.startsWith(XTBot.prefix+"play")) {
			IVoiceChannel channel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
			if (event.getGuild().getConnectedVoiceChannel() == channel) {
				Music music = new Music();
				music.loadAndPlay(event.getChannel(), message.substring(5, message.length()));
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() == null && channel == null) {
				EventListener.sendMessage("Join a voice channel if you want me to play a song!", event.getChannel());
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() != null && channel != event.getGuild().getConnectedVoiceChannel()){
				EventListener.sendMessage("Join "+event.getGuild().getConnectedVoiceChannel()+" to add a song to the queue.", event.getChannel());
				return;
			} else if (event.getGuild().getConnectedVoiceChannel() == null & channel != null) {
				channel.join();
				Music music = new Music();
				music.loadAndPlay(event.getChannel(), message.substring(5, message.length()));
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"volume") || message.startsWith(XTBot.prefix+"vol")) {
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (message.split(" ").length == 2) { 
					try {
						Integer.valueOf(message.split(" ")[1]);
					} catch (NumberFormatException exception) {
						EventListener.sendMessage("Please enter a number between 0 and 150.", event.getChannel());
						return;
					}
					Integer volume = Integer.valueOf(message.split(" ")[1]);
					if (volume < 0 || volume > 150) {
						EventListener.sendMessage("Please enter a number between 0 and 150.", event.getChannel());
						return;
					}
					Music music = new Music();					
					GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
					manager.player.setVolume(volume);
					EventListener.sendMessage("Volume set to "+volume.toString()+"%", event.getChannel());
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
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Music music = new Music();					
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
				if (manager.player.isPaused()) {
					manager.player.setPaused(false);
					EventListener.sendMessage("Music resumed.", event.getChannel());
					return;
				} else {
					manager.player.setPaused(true);
					EventListener.sendMessage("Music paused.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"resume")) {
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Music music = new Music();					
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
				if (manager.player.isPaused()) {
					manager.player.setPaused(false);
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
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Music music = new Music();					
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
				manager.scheduler.queue.clear();
				EventListener.sendMessage("Queue cleared.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"skip")) {
			Music music = new Music();
			GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
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
				Music music = new Music();
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
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
			Music music = new Music();
			GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
			AudioTrack playing = manager.player.getPlayingTrack();
			BlockingQueue<AudioTrack> queue = manager.scheduler.queue;
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
			Music music = new Music();
			GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
			AudioTrack playing = manager.player.getPlayingTrack();
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
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Music music = new Music();					
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
				if (manager.scheduler.isRepeating()) {
					manager.scheduler.setRepeating(false);
					EventListener.sendMessage("Loop stopped.", event.getChannel());
					return;
				} else {
					manager.scheduler.setRepeating(true);
					EventListener.sendMessage("Song looped.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"shuffle")) {
			boolean isOne = false;
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) {
						isOne = true;
					}
				}
			}
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				Music music = new Music();					
				GuildMusicManager manager = music.musicManagers.get(event.getGuild().getLongID());
				Collections.shuffle((List<?>) manager.scheduler.queue);
				EventListener.sendMessage("Queue shuffled.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		}
	}
}
