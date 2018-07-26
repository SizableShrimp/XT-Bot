package me.sizableshrimp.discordbot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;

public class EventListener {
	@EventSubscriber
	public void onMessageEvent(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		if (message.startsWith(XTBot.prefix+"help") || event.getMessage().getMentions().contains(XTBot.client.getOurUser())) {
			sendMessage("Hello! I am XT Bot. I don't do much yet because I am still in development. Commands:\n`"+XTBot.prefix+"hey`\n`"+XTBot.prefix+"info`\n`"+XTBot.prefix+"settings`\nMore commands will be coming in the future!", event);
			return;
		}
		if (message.startsWith(XTBot.prefix+"allstar")) {
			IVoiceChannel channel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
			if (channel == null) {
				sendMessage("Join a voice channel if you want me to play All Star!", event);
				return;
			}
			channel.join();
			playAllStar(event);
			sendMessage("Joined "+channel.getName()+" and playing All Star", event);
		}
		if (message.startsWith(XTBot.prefix+"info")) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Information");
			embed.appendDesc("This bot is built with [Spring Boot 2.0.3](https://spring.io/projects/spring-boot) and hosted on [Heroku](https://dashboard.heroku.com). It is coded in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.");
			embed.appendField("Author", "SizableShrimp", true);
			embed.appendField("Discord4J Version", "2.10.1", true);
			embed.appendField("Prefix", XTBot.prefix, false);
			embed.appendField("Uptime", getUptime(), false);
			new MessageBuilder(XTBot.client).appendContent("To find out my commands, use `"+XTBot.prefix+"help`").withEmbed(embed.build()).withChannel(event.getChannel()).build();
		}
		if (message.startsWith(XTBot.prefix+"hey")) {
			sendMessage("Hello! :smile:", event);
			return;
		}
		if (message.startsWith(XTBot.prefix+"settings prefix")) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				if (message.split(" ").length != 3) {
					sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"settings prefix [new prefix]```", event);
					return;
				} else {
					String newPrefix = message.split(" ")[2];
					if (newPrefix.length() != 1) {
						sendMessage(":x: A prefix can only be 1 character long.", event);
						return;
					}
					if (newPrefix.toUpperCase() != newPrefix) {
						sendMessage(":x: A prefix cannot be a letter.", event);
						return;
					}
					XTBot.prefix = newPrefix;
					sendMessage(":white_check_mark: Prefix successfully changed to `"+XTBot.prefix+"`", event);
					return;
				}
			} else {
				sendMessage(":x: Insufficient permission.", event);
				return;
			}
		}
		if (message.startsWith(XTBot.prefix+"settings") && message.split(" ").length == 1) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("XT Bot Settings");
				embed.appendField(":exclamation: **Prefix**", "`"+XTBot.prefix+"settings prefix [new prefix]`", true);
				sendEmbed(embed, event);
				return;
			} else {
				sendMessage(":x: Insufficient permission.", event);
				return;
			}
		}
	}

	private void sendMessage(String message, MessageReceivedEvent event) throws DiscordException, MissingPermissionsException {
		new MessageBuilder(XTBot.client).appendContent("\u200B"+message).withChannel(event.getChannel()).build();
	}

	private void sendEmbed(EmbedBuilder embed, MessageReceivedEvent event) throws DiscordException, MissingPermissionsException {
		new MessageBuilder(XTBot.client).withEmbed(embed.build()).withChannel(event.getChannel()).build();
	}

	private String getUptime() {
		Long uptime = System.currentTimeMillis()-XTBot.firstOnline;
		Long days = TimeUnit.MILLISECONDS.toDays(uptime);
		Long hours = TimeUnit.MILLISECONDS.toHours(uptime) - TimeUnit.DAYS.toHours(days);
		Long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.HOURS.toMinutes(hours) - TimeUnit.DAYS.toMinutes(days);
		Long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.DAYS.toSeconds(days);
		List<String> formats = new ArrayList<String>();
		if (days > 0) {
			String string;
			if (days == 1) {
				string = days.toString()+" day";
			} else {
				string = days.toString()+" days";
			}
			formats.add(string);
		}
		if (hours > 0) {
			String string;
			if (hours == 1) {
				string = hours.toString()+" hour";
			} else {
				string = hours.toString()+" hours";
			}
			formats.add(string);
		}
		if (minutes > 0) {
			String string;
			if (minutes == 1) {
				string = minutes.toString()+" minute";
			} else {
				string = minutes.toString()+" minutes";
			}
			formats.add(string);
		}
		if (seconds > 0) {
			String string;
			if (seconds == 1) {
				string = seconds.toString()+" second";
			} else {
				string = seconds.toString()+" seconds";
			}
			formats.add(string);
		}
		String result;
		if (formats.size() == 2) {
			result = formats.get(0)+" and "+formats.get(1);
		} else if (formats.size() == 3) {
			result = formats.get(0)+", "+formats.get(1)+", and "+formats.get(2);
		} else if (formats.size() == 4) {
			result = formats.get(0)+", "+formats.get(1)+", "+formats.get(2)+", and "+formats.get(3);
		} else {
			result = formats.get(0);
		}
		return result;
	}

	public void playAllStar(MessageReceivedEvent event) {
		AudioPlayerManager manager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(manager);
		AudioPlayer audio = manager.createPlayer();
		TaskScheduler scheduler = new TaskScheduler();
		audio.addListener(scheduler);
		manager.loadItem("L_jWHffIx5E", new AudioLoadResultHandler() {
			@Override
			public void loadFailed(FriendlyException exception) {
				sendMessage("There was an error playing this song. Please try again later.", event);
			}

			@Override
			public void noMatches() {
				//no matches found
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				//playlist was loaded
			}

			@Override
			public void trackLoaded(AudioTrack track) {
				audio.playTrack(track);
			}
		});
	}
}
