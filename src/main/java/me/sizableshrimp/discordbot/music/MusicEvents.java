package me.sizableshrimp.discordbot.music;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

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
	public Music music;

	public MusicEvents() {
		music = new Music();
	}

	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
		AudioPlayer player = manager.player;
		TrackScheduler scheduler = manager.scheduler;
		if (message.toLowerCase().startsWith(XTBot.prefix+"music")) {
			EventListener.sendMessage("I can play music! My music commands are:```"+XTBot.prefix+"play [song] - Plays the song that you request.\n"+XTBot.prefix+"volume [new volume] or "+XTBot.prefix+"vol [new volume] - Changes the volume.\n"+XTBot.prefix+"pause - Pauses/unpauses the song.\n"+XTBot.prefix+"queue or "+XTBot.prefix+"q - Shows what is currently playing and what is queued up to go next.\n"+XTBot.prefix+"clear - Clears all the queued music.\n"+XTBot.prefix+"nowplaying or "+XTBot.prefix+"np\n"+XTBot.prefix+"remove [number in queue to remove] - Removes the song in the queue at the number given.\n"+XTBot.prefix+"skip - Requests to skip the song. If enough people have voted to skip, the next song will be played.\n"+XTBot.prefix+"forceskip - Forecfully skips to the next song.\n"+XTBot.prefix+"disconnect - Disconnects from the voice channel and stops playing music.\n"+XTBot.prefix+"loop - Puts the song currently playing on/off repeat.```__**Please note:**__ Some of the commands are for administrators only. Do not expect to be able to use all of them! If you are the only person in the channel with a bot, then you may use all commands.", event.getChannel());
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"play")) {
			if (message.split(" ").length == 2) {
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
					channel.join();
					music.loadAndPlay(event.getChannel(), channel, query);
					return;
				}
			} else {
				EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"play [song]```", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"volume") || message.startsWith(XTBot.prefix+"vol")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (message.split(" ").length == 2) { 
					try {
						Integer.valueOf(message.split(" ")[1]);
					} catch (NumberFormatException exception) {
						EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
						return;
					}
					int volume = Integer.valueOf(message.split(" ")[1]);
					if (volume < 0 || volume > 100) {
						EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
						return;
					}
					player.setVolume(volume);
					EventListener.sendMessage("Volume set to **"+volume+"%**", event.getChannel());
					return;
				} else {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"volume [new volume]```", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"pause")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (music.isPlaying(event.getGuild())) {
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
					EventListener.sendMessage("There is no music to pause or unpause.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"clear")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				scheduler.queue.clear();
				EventListener.sendMessage("Queue cleared.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"remove")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (scheduler.queue.isEmpty()) {
					EventListener.sendMessage("There is nothing in the queue to remove.", event.getChannel());
					return;
				}
				if (message.split(" ").length == 2) { 
					try {
						Integer.valueOf(message.split(" ")[1]);
					} catch (NumberFormatException exception) {
						EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
						return;
					}
					Integer queueNum = Integer.valueOf(message.split(" ")[1]);
					if (scheduler.queue.size() < queueNum || queueNum <= 0) {
						EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
						return;
					}
					AudioTrack selected = null;
					Integer num = 0;
					for (AudioTrack track : scheduler.queue) {
						num++;
						if (queueNum == num) {
							selected = track;
						}
					}
					if (selected == null) {
						EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
						return;
					}
					scheduler.queue.remove(selected);
					EventListener.sendMessage("Removed "+selected.getInfo().title+" from the queue.", event.getChannel());
					return;
				} else {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"remove [number from queue]```", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"skip")) {
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
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"forceskip")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				music.skipTrack(event.getChannel());
				music.wantsToSkip.remove(manager);
				music.neededToSkip.remove(manager);
				EventListener.sendMessage("Skipped song.", event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"queue") || message.startsWith(XTBot.prefix+"q")) {
			AudioTrack playing = player.getPlayingTrack();
			BlockingQueue<AudioTrack> queue = scheduler.queue;
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Queue");
			if (playing == null) {
				embed.appendDesc("There is currently nothing playing.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			embed.appendDesc("__**Now Playing:**__\n"+"["+playing.getInfo().title+"]("+playing.getInfo().uri+") | `"+playing.getInfo().length+"`");
			embed.appendDesc("\n\n__**Up Next:**__\n");
			embed.withColor(242, 242, 242);
			if (queue.isEmpty()) {
				embed.appendDesc("\nThere is currently nothing up next.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			Integer number = 1;
			for (AudioTrack track : queue) {
				embed.appendDesc("\n"+number.toString()+". "+"["+track.getInfo().title+"]("+track.getInfo().uri+") | `"+track.getInfo().length+"`");
				number++;
			}
			EventListener.sendEmbed(embed, event.getChannel());
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"nowplaying") || message.startsWith(XTBot.prefix+"np")) {
			AudioTrack playing = player.getPlayingTrack();
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Now Playing");
			if (playing == null) {
				embed.appendDesc("There is currently nothing playing.");
				EventListener.sendEmbed(embed, event.getChannel());
				return;
			}
			AudioTrackInfo info = playing.getInfo();
			embed.appendDesc("["+info.title+"]("+info.uri+")");
			embed.appendDesc("\n"+getLength(System.currentTimeMillis()-music.trackStartTime)+" / "+getLength(info.length));
			embed.withColor(242, 242, 242);
			EventListener.sendEmbed(embed, event.getChannel());
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"disconnect") || message.startsWith(XTBot.prefix+"leave")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				IVoiceChannel channel = event.getGuild().getConnectedVoiceChannel();
				if (channel == null) {
					EventListener.sendMessage("I am not connected to a voice channel.", event.getChannel());
					return;
				}
				channel.leave();
				scheduler.queue.clear();
				manager.player.stopTrack();
				EventListener.sendMessage("Left "+channel.getName(), event.getChannel());
				return;
			} else {
				EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"loop")) {
			boolean isOne = isOne(event);
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne == true) {
				if (scheduler.isRepeating()) {
					scheduler.setRepeating(false);
					EventListener.sendMessage("Loop stopped.", event.getChannel());
					return;
				} else {
					scheduler.setRepeating(true);
					EventListener.sendMessage(":repeat: Song looped.", event.getChannel());
					return;
				}
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

	String getLength(Long length) {
		Long minutes = TimeUnit.MILLISECONDS.toMinutes(length);
		Long seconds = TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(minutes);
		String convertedSeconds;
		if (seconds < 10L) {
			convertedSeconds = "0"+seconds.toString();
		} else {
			convertedSeconds = seconds.toString();
		}
		return minutes.toString()+":"+convertedSeconds;
	}
}
