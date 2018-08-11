package me.sizableshrimp.discordbot.music;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
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
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

public class MusicEvents {
	public Music music;

	public MusicEvents() {
		music = new Music();
	}

	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		RequestBuffer.request(() -> {
			if (event.getAuthor().isBot()) return;
			String message = event.getMessage().getContent();
			GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
			AudioPlayer player = manager.player;
			TrackScheduler scheduler = manager.scheduler;
			IChannel channel = event.getChannel();
			if (message.toLowerCase().startsWith(XTBot.prefix+"music")) {
				EventListener.sendMessage("I can play music! My music commands are:```"+
						XTBot.prefix+"play [song] - Plays the song that you request.\n"+
						XTBot.prefix+"volume [new volume] or "+XTBot.prefix+"vol [new volume] - Changes the volume.\n"+
						XTBot.prefix+"pause - Pauses/unpauses the song.\n"+
						XTBot.prefix+"queue or "+XTBot.prefix+"q - Shows what is currently playing and what is queued up to go next.\n"+
						XTBot.prefix+"clear - Clears all the queued music.\n"+
						XTBot.prefix+"nowplaying or "+XTBot.prefix+"np - Shows what is currently playing.\n"+
						XTBot.prefix+"remove [number in queue to remove] - Removes the song in the queue at the number given.\n"+
						XTBot.prefix+"rewind - Rewinds the song by 10 seconds.\n"+
						XTBot.prefix+"fastforward or "+XTBot.prefix+"ff - Fast forwards the song by 10 seconds.\n"+
						XTBot.prefix+"goto [time in song] - Starts playing from a certain point in the song.\n"+
						XTBot.prefix+"skip - Requests to skip the song. If enough people have voted to skip, the next song will be played.\n"+
						XTBot.prefix+"forceskip - Forecfully skips to the next song.\n"+
						XTBot.prefix+"disconnect or "+XTBot.prefix+"leave - Disconnects from the voice channel and stops playing music.\n"+
						XTBot.prefix+"loop - Puts the song currently playing on/off repeat.```"+
						"__**Please note:**__ Some of the commands are for administrators only. Do not expect to be able to use all of them! If you are the only person in the voice channel with me, then you may use all commands.", channel);
				return;
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"play")) {
				if (message.split(" ").length >= 2) {
					IVoiceChannel voiceChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
					String query = message.substring(6);
					boolean isValid = true;
					try {new URL(query).toURI();} catch (MalformedURLException | URISyntaxException exception) {isValid = false;}
					if (!isValid) query = "ytsearch:"+query;
					if (event.getGuild().getConnectedVoiceChannel() != null && event.getGuild().getConnectedVoiceChannel() == voiceChannel) {
						music.loadAndPlay(channel, voiceChannel, query);
						return;
					} else if (event.getGuild().getConnectedVoiceChannel() == null && voiceChannel == null) {
						EventListener.sendMessage("Join a voice channel if you want me to play a song!", channel);
						return;
					} else if (event.getGuild().getConnectedVoiceChannel() != null && voiceChannel != event.getGuild().getConnectedVoiceChannel()){
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to add a song to the queue.", channel);
						return;
					} else if (event.getGuild().getConnectedVoiceChannel() == null & voiceChannel != null) {
						voiceChannel.join();
						music.loadAndPlay(channel, voiceChannel, query);
						return;
					}
				} else {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"play [song]```", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"volume") || message.startsWith(XTBot.prefix+"vol")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (message.split(" ").length == 2) { 
						try {
							Integer.valueOf(message.split(" ")[1]);
						} catch (NumberFormatException exception) {
							EventListener.sendMessage("Please enter a number between 0 and 100.", channel);
							return;
						}
						int volume = Integer.valueOf(message.split(" ")[1]);
						if (volume < 0 || volume > 100) {
							EventListener.sendMessage("Please enter a number between 0 and 100.", channel);
							return;
						}
						player.setVolume(volume);
						EventListener.sendMessage("Volume set to **"+volume+"%**", channel);
						return;
					} else {
						EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"volume [new volume]```", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"pause")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (manager.player.getPlayingTrack() != null) {
						if (player.isPaused()) {
							player.setPaused(false);
							EventListener.sendMessage(":arrow_forward: Music resumed.", channel);
							return;
						} else {
							player.setPaused(true);
							EventListener.sendMessage(":pause_button: Music paused.", channel);
							return;
						}
					} else {
						EventListener.sendMessage("There is no music to pause or unpause.", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"clear")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					scheduler.queue.clear();
					EventListener.sendMessage("Queue cleared.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"remove")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (scheduler.queue.isEmpty()) {
						EventListener.sendMessage("There is nothing in the queue to remove.", channel);
						return;
					}
					if (message.split(" ").length == 2) { 
						try {
							Integer.valueOf(message.split(" ")[1]);
						} catch (NumberFormatException exception) {
							EventListener.sendMessage("Please enter a number from the queue.", channel);
							return;
						}
						Integer queueNum = Integer.valueOf(message.split(" ")[1]);
						if (scheduler.queue.size() < queueNum || queueNum <= 0) {
							EventListener.sendMessage("Please enter a number from the queue.", channel);
							return;
						}
						AudioTrack selected = null;
						Integer num = 0;
						for (AudioTrack track : scheduler.queue) {
							num++;
							if (queueNum == num) selected = track;
						}
						if (selected == null) {
							EventListener.sendMessage("Please enter a number from the queue.", channel);
							return;
						}
						scheduler.queue.remove(selected);
						EventListener.sendMessage("Removed `"+selected.getInfo().title+"` from the queue.", channel);
						return;
					} else {
						EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"remove [number from queue]```", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"goto")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (player.getPlayingTrack() == null) {
						EventListener.sendMessage("There is nothing to change the time of.", channel);
						return;
					}
					if (message.split(" ").length == 2) {
						if (!message.contains(":")) {
							EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"goto [time in song]```Example: `"+XTBot.prefix+"goto 5:35`", channel);
							return;
						}
						String time = message.split(" ")[1];
						int colons = 0;
						for (char c : time.toCharArray()) if (c == ':') colons++;
						if (colons == 1 || colons == 2) {
							List<Integer> numbers = new ArrayList<Integer>();
							for (String s : time.split(":")) {
								try {
									Integer.valueOf(s);
								} catch (NumberFormatException e) {
									EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"goto [time in song]```Example: `"+XTBot.prefix+"goto 5:35`", channel);
									return;
								}
								numbers.add(Integer.valueOf(s));
							}
							Long millis = colons == 1 ? TimeUnit.MINUTES.toMillis(numbers.get(0)) + TimeUnit.SECONDS.toMillis(numbers.get(1)) : TimeUnit.HOURS.toMillis(numbers.get(0)) + TimeUnit.MINUTES.toMillis(numbers.get(1)) + TimeUnit.SECONDS.toMillis(numbers.get(2));
							AudioTrack track = player.getPlayingTrack();
							if (millis < 0L || millis > track.getDuration()) {
								EventListener.sendMessage("Specified time is out of range. Please choose a different time.", channel);
								return;
							}
							track.setPosition(millis);
							EventListener.sendMessage("Now playing at `"+time+"`", channel);
							return;
						} else {
							EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"goto [time in song]```Example: `"+XTBot.prefix+"goto 5:35`", channel);
							return;
						}
					} else {
						EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"goto [time in song]```Example: `"+XTBot.prefix+"goto 5:35`", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"skip")) {
				if (manager.player.getPlayingTrack() == null) {
					EventListener.sendMessage("There is nothing to skip.", channel);
					return;
				}
				if (manager.usersSkipping.contains(event.getAuthor())) {
					EventListener.sendMessage(":x: You have already requested to skip this song.", channel);
					return;
				}
				Integer wants = manager.wantsToSkip;
				Integer needed = manager.neededToSkip;
				if (wants+1 == needed) {
					music.skipTrack(channel);
					EventListener.sendMessage("Skipped song.", channel);
					return;
				}
				wants++;
				manager.wantsToSkip = wants;
				manager.usersSkipping.add(event.getAuthor());
				EventListener.sendMessage(wants.toString()+"/"+needed.toString()+" people have requested to skip this song.", channel);
				return;
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"forceskip")) {
				if (manager.player.getPlayingTrack() == null) {
					EventListener.sendMessage("There is nothing to skip.", channel);
					return;
				}
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					music.skipTrack(channel);
					EventListener.sendMessage("Skipped song.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"queue") || message.startsWith(XTBot.prefix+"q")) {
				AudioTrack playing = player.getPlayingTrack();
				BlockingQueue<AudioTrack> queue = scheduler.queue;
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("Queue");
				embed.withColor(242, 242, 242);
				if (playing == null) {
					embed.appendDesc("There is currently nothing playing.");
					EventListener.sendEmbed(embed, channel);
					return;
				}
				embed.appendDesc("__**Now Playing:**__\n"+"["+playing.getInfo().title+"]("+playing.getInfo().uri+") | `"+getLength(playing.getInfo().length)+"`");
				embed.appendDesc("\n\n__**Up Next:**__\n");
				if (queue.isEmpty()) {
					embed.appendDesc("\nThere is currently nothing up next.");
					EventListener.sendEmbed(embed, channel);
					return;
				}
				Integer number = 1;
				for (AudioTrack track : queue) {
					embed.appendDesc("\n"+number.toString()+". "+"["+track.getInfo().title+"]("+track.getInfo().uri+") | `"+getLength(track.getInfo().length)+"`");
					number++;
				}
				EventListener.sendEmbed(embed, channel);
				return;
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"nowplaying") || message.startsWith(XTBot.prefix+"np")) {
				AudioTrack playing = player.getPlayingTrack();
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("Now Playing");
				embed.withColor(242, 242, 242);
				if (playing == null) {
					embed.appendDesc("There is currently nothing playing.");
					EventListener.sendEmbed(embed, channel);
					return;
				}
				AudioTrackInfo info = playing.getInfo();
				embed.appendDesc("["+info.title+"]("+info.uri+")");
				embed.appendDesc("\n"+getLength(playing.getPosition())+" / "+getLength(info.length));
				EventListener.sendEmbed(embed, channel);
				return;
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"rewind")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (player.getPlayingTrack() == null) {
						EventListener.sendMessage("There is nothing to rewind.", channel);
						return;
					}
					player.getPlayingTrack().setPosition(player.getPlayingTrack().getPosition()-TimeUnit.SECONDS.toMillis(10));
					EventListener.sendMessage(":rewind: Skipped 10 seconds backwards.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"fastforward") || message.toLowerCase().startsWith(XTBot.prefix+"ff")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (player.getPlayingTrack() == null) {
						EventListener.sendMessage("There is nothing to fast forward.", channel);
						return;
					}
					player.getPlayingTrack().setPosition(player.getPlayingTrack().getPosition()+TimeUnit.SECONDS.toMillis(10));
					EventListener.sendMessage(":fast_forward: Skipped 10 seconds forwards.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"disconnect") || message.startsWith(XTBot.prefix+"leave")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					IVoiceChannel voiceChannel = event.getGuild().getConnectedVoiceChannel();
					if (voiceChannel == null) {
						EventListener.sendMessage("I am not connected to a voice channel.", voiceChannel);
						return;
					}
					voiceChannel.leave();
					EventListener.sendMessage("Left `"+voiceChannel.getName()+"`", channel);
					scheduler.queue.clear();
					manager.player.startTrack(null, false);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(XTBot.prefix+"loop")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (scheduler.isRepeating()) {
						scheduler.setRepeating(false);
						EventListener.sendMessage("Loop disabled.", channel);
						return;
					} else {
						scheduler.setRepeating(true);
						EventListener.sendMessage(":repeat: Loop enabled.", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
				}
			}
		});
	}

	@EventSubscriber
	public void onUserVoiceLeave(UserVoiceChannelLeaveEvent event) {
		RequestBuffer.request(() -> {
			if (event.getUser() == XTBot.client.getOurUser()) {
				GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
				manager.player.setVolume(music.DEFAULT_VOLUME);
				return;
			} else {
				IVoiceChannel channel = event.getVoiceChannel();
				if (event.getGuild().getConnectedVoiceChannel() == channel && channel.getConnectedUsers().size() == 1) {
					channel.leave();
					GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
					manager.scheduler.queue.clear();
					manager.player.startTrack(null, false);
				}
			}
		});
	}

	boolean isOne(MessageReceivedEvent event) {
		RequestBuffer.request(() -> {
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel() != null && state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(XTBot.client.getOurUser()) && users.contains(event.getAuthor())) return true;
				}
			}
			return false;
		});
		return false;
	}

	String getLength(Long length) {
		Long hours = 0L;
		if (TimeUnit.MILLISECONDS.toHours(length) > 0) hours = TimeUnit.MILLISECONDS.toHours(length);
		Long minutes = TimeUnit.MILLISECONDS.toMinutes(length) - TimeUnit.HOURS.toMinutes(hours);
		Long seconds = TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
		String convertedSeconds = seconds < 10L ? "0"+seconds.toString() : seconds.toString();
		String convertedMinutes = minutes < 10L ? "0"+minutes.toString() : minutes.toString();
		return hours != 0L ? hours.toString()+":"+convertedMinutes+":"+convertedSeconds : minutes.toString()+":"+convertedSeconds;
	}
}
