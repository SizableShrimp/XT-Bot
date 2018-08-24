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
import me.sizableshrimp.discordbot.Main;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

public class MusicEvents {
	public Music music;
	private List<IGuild> lockedGuilds = new ArrayList<IGuild>();

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
			if (message.toLowerCase().startsWith(Main.prefix+"music")) {
				EventListener.sendMessage("I can play music! My music commands are:```[] = required  () = optional\n"+
						Main.prefix+"play [song] - Plays the song that you request.\n"+
						Main.prefix+"volume (new volume) or "+Main.prefix+"vol (new volume) - Changes the volume or tells the current volume.\n"+
						Main.prefix+"pause - Pauses/unpauses the song.\n"+
						Main.prefix+"queue or "+Main.prefix+"q - Shows what is currently playing and what is queued up to go next.\n"+
						Main.prefix+"clear - Clears all the queued music.\n"+
						Main.prefix+"nowplaying or "+Main.prefix+"np - Shows what is currently playing.\n"+
						Main.prefix+"remove [number in queue to remove] - Removes the song in the queue at the number given.\n"+
						Main.prefix+"rewind - Rewinds the song by 10 seconds.\n"+
						Main.prefix+"fastforward or "+Main.prefix+"ff - Fast forwards the song by 10 seconds.\n"+
						Main.prefix+"goto [time in song] - Starts playing from a certain point in the song.\n"+
						Main.prefix+"skip - Requests to skip the song. If enough people have voted to skip, the next song will be played.\n"+
						Main.prefix+"forceskip - Forecfully skips to the next song.\n"+
						Main.prefix+"disconnect or "+Main.prefix+"leave - Disconnects from the voice channel and stops playing music.\n"+
						Main.prefix+"loop - Puts the song currently playing on/off repeat.```"+
						Main.prefix+"lock - Locks the bot to admins only.```"+
						"__**Please note:**__ Some of the commands are for administrators only. Do not expect to be able to use all of them! If you are the only person in the voice channel with me, then you may use all commands (except"+Main.prefix+"lock).", channel);
				return;
			} else if (message.toLowerCase().startsWith(Main.prefix+"play")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
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
					} else if (event.getGuild().getConnectedVoiceChannel() != null && voiceChannel != event.getGuild().getConnectedVoiceChannel()) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to add a song to the queue.", channel);
						return;
					} else if (event.getGuild().getConnectedVoiceChannel() == null & voiceChannel != null) {
						voiceChannel.join();
						music.loadAndPlay(channel, voiceChannel, query);
						return;
					}
				} else {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"play [song]```", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"volume") || message.startsWith(Main.prefix+"vol")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to change/look at the volume.", channel);
						return;
					}
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
						EventListener.sendMessage("Volume set to **"+volume+"%** from **"+player.getVolume()+"**%", channel);
						return;
					} else if (message.split(" ").length == 1) {
						EventListener.sendMessage("Volume is at **"+player.getVolume()+"%**", channel);
						return;
					} else {
						EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"volume [new volume] or "+Main.prefix+"volume```", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"pause")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to pause/unpause the music.", channel);
						return;
					}
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"clear")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to clear the queue.", channel);
						return;
					}
					scheduler.queue.clear();
					EventListener.sendMessage("Queue cleared.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"remove")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to remove a song from the queue.", channel);
						return;
					}
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
						EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"remove [number from queue]```", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"goto")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to change the time of the current song.", channel);
						return;
					}
					if (player.getPlayingTrack() == null) {
						EventListener.sendMessage("There is nothing to change the time of.", channel);
						return;
					}
					if (message.split(" ").length == 2) {
						if (!message.contains(":")) {
							EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"goto [time in song]```Example: `"+Main.prefix+"goto 5:35`", channel);
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
									EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"goto [time in song]```Example: `"+Main.prefix+"goto 5:35`", channel);
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
							EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"goto [time in song]```Example: `"+Main.prefix+"goto 5:35`", channel);
							return;
						}
					} else {
						EventListener.sendMessage("Incorrect usage. Please use: ```"+Main.prefix+"goto [time in song]```Example: `"+Main.prefix+"goto 5:35`", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"skip")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (event.getGuild().getConnectedVoiceChannel() == null) {
					EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
					return;
				}
				if(!isConnectedToSame(event)) {
					EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to request to skip the current song.", channel);
					return;
				}
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"forceskip")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to forceskip the current song.", channel);
						return;
					}
					if (manager.player.getPlayingTrack() == null) {
						EventListener.sendMessage(":x: There is nothing to skip.", channel);
						return;
					}
					music.skipTrack(channel);
					EventListener.sendMessage("Skipped song.", channel);
					return;
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"queue") || message.startsWith(Main.prefix+"q")) {
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"nowplaying") || message.startsWith(Main.prefix+"np")) {
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"rewind")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to rewind.", channel);
						return;
					}
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"fastforward") || message.toLowerCase().startsWith(Main.prefix+"ff")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to fast forward.", channel);
						return;
					}
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"disconnect") || message.startsWith(Main.prefix+"leave")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to make me disconnect.", channel);
						return;
					}
					IVoiceChannel voiceChannel = event.getGuild().getConnectedVoiceChannel();
					if (voiceChannel == null) {
						EventListener.sendMessage("I am not connected to a voice channel.", channel);
						return;
					}
					if (voiceChannel != event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel()) {
						EventListener.sendMessage(":x: You must be connected to `"+voiceChannel.getName()+"` to make me leave!", channel);
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
			} else if (message.toLowerCase().startsWith(Main.prefix+"loop")) {
				if (!channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) && isLocked(event.getGuild(), channel)) return;
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS) || isOne(event)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if(!isConnectedToSame(event)) {
						EventListener.sendMessage("Join `"+event.getGuild().getConnectedVoiceChannel()+"` to loop the song.", channel);
						return;
					}
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
					return;
				}
			} else if (message.toLowerCase().startsWith(Main.prefix+"lock")) {
				if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_CHANNELS)) {
					if (event.getGuild().getConnectedVoiceChannel() == null) {
						EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
						return;
					}
					if (lockedGuilds.contains(event.getGuild())) {
						lockedGuilds.remove(event.getGuild());
						EventListener.sendMessage(":white_check_mark: Music unlocked.", channel);
						return;
					} else {
						lockedGuilds.add(event.getGuild());
						EventListener.sendMessage(":lock: Music locked.", channel);
						return;
					}
				} else {
					EventListener.sendMessage(":x: Insufficient permission. You can do this command if you have the **Manage Channels** permission.", channel);
					return;
				}
			}
		});
	}

	@EventSubscriber
	public void onUserVoiceLeave(UserVoiceChannelLeaveEvent event) {
		RequestBuffer.request(() -> {
			IVoiceChannel channel = event.getVoiceChannel();
			if (event.getGuild().getConnectedVoiceChannel() == channel && channel.getConnectedUsers().size() == 1) {
				GuildMusicManager manager = music.getGuildAudioPlayer(event.getGuild());
				manager.scheduler.queue.clear();
				manager.player.startTrack(null, false);
				manager.player.setVolume(music.DEFAULT_VOLUME);
				manager.player.setPaused(false);
				lockedGuilds.remove(event.getGuild());
				channel.leave();
			}
		});
	}

	private boolean isOne(MessageReceivedEvent event) {
		RequestBuffer.request(() -> {
			if (event.getAuthor().getVoiceStateForGuild(event.getGuild()) != null) {
				IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
				if (state.getChannel() != null && state.getChannel().getConnectedUsers().size() == 2) {
					List<IUser> users = state.getChannel().getConnectedUsers();
					if (users.contains(Main.client.getOurUser()) && users.contains(event.getAuthor())) return true;
				}
			}
			return false;
		});
		return false;
	}

	private String getLength(Long length) {
		Long hours = 0L;
		if (TimeUnit.MILLISECONDS.toHours(length) > 0) hours = TimeUnit.MILLISECONDS.toHours(length);
		Long minutes = TimeUnit.MILLISECONDS.toMinutes(length) - TimeUnit.HOURS.toMinutes(hours);
		Long seconds = TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
		String convertedSeconds = seconds < 10L ? "0"+seconds.toString() : seconds.toString();
		String convertedMinutes = minutes < 10L ? "0"+minutes.toString() : minutes.toString();
		return hours != 0L ? hours.toString()+":"+convertedMinutes+":"+convertedSeconds : minutes.toString()+":"+convertedSeconds;
	}

	private boolean isConnectedToSame(MessageReceivedEvent event) {
		return (event.getGuild().getConnectedVoiceChannel() != null && event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel() != event.getGuild().getConnectedVoiceChannel());
	}
	
	private boolean isLocked(IGuild guild, IChannel channel) {
		if (lockedGuilds.contains(guild)) {
			EventListener.sendMessage(":lock: Music is currently locked for normal members. Please try again later.", channel);
			return true;
		}
		return false;
	}
}
