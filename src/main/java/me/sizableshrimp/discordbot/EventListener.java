package me.sizableshrimp.discordbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.sizableshrimp.discordbot.music.Music;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;

public class EventListener {
	public static HashMap<IGuild, IChannel> startedChannel = new HashMap<IGuild, IChannel>();

	@EventSubscriber
	public void onMessageEvent(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		if (message.toLowerCase().startsWith(XTBot.prefix+"help") || (!event.getMessage().mentionsEveryone() && !event.getMessage().mentionsHere() && event.getMessage().getMentions().contains(XTBot.client.getOurUser()))) {
			sendMessage("Hello! I am XT Bot. I don't do much yet because I am still in development. Commands:\n`"+XTBot.prefix+"hey`\n`"+XTBot.prefix+"info`\n`"+XTBot.prefix+"settings`\n`"+XTBot.prefix+"allstar`\nMore commands will be coming in the future!", event.getChannel());
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"allstar")) {
			Music music = new Music();
			if (music.isPlaying(event.getGuild())) {
				sendMessage("I am already playing All Star in "+event.getGuild().getConnectedVoiceChannel().getName(), event.getChannel());
				return;
			}
			IVoiceChannel channel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
			if (channel == null) {
				sendMessage("Join a voice channel if you want me to play All Star!", event.getChannel());
				return;
			}
			channel.join();
			playAllStar(event, channel);
			sendMessage("Joined "+channel.getName()+" and playing All Star", event.getChannel());
			startedChannel.put(event.getGuild(), event.getChannel());
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"info")) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Information");
			embed.appendDesc("This bot is built with [Spring Boot 2.0.3](https://spring.io/projects/spring-boot) and hosted on [Heroku](https://dashboard.heroku.com). It is coded in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.");
			embed.appendField("Author", "SizableShrimp", true);
			embed.appendField("Discord4J Version", "2.10.1", true);
			embed.appendField("Prefix", XTBot.prefix, false);
			embed.appendField("Uptime", getUptime(), false);
			new MessageBuilder(XTBot.client).appendContent("To find out my commands, use `"+XTBot.prefix+"help`").withEmbed(embed.build()).withChannel(event.getChannel()).build();
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"hey")) {
			sendMessage("Hello! :smile:", event.getChannel());
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"fortnite") || message.toLowerCase().startsWith(XTBot.prefix+"ftn")) {
			if (message.split(" ").length == 3) {
				String platform = message.split(" ")[1].toLowerCase();
				if (!(platform.equals("pc") || platform.equals("ps4") || platform.equals("xbox"))) {
					EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"fortnite [pc|ps4|xbox] [username]```", event.getChannel());
					return;
				}
				String embedPlatform;
				if (platform == "ps4") {
					embedPlatform = "PS4";
					platform = "psn";
				} else if (platform == "xbox") {
					embedPlatform = "Xbox";
					platform = "xb1";
				} else {
					embedPlatform = "PC";
				}
				String username = message.split(" ")[2];
				try {
					URL siteURL = new URL("https://api.fortnitetracker.com/v1/profile/"+platform+"/"+username);
					HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
					connection.setRequestMethod("GET");
					connection.setRequestProperty("TRN-Api-Key", System.getenv("FORTNITE_API"));
					connection.connect();
					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					System.out.println("Fortnite stats information:");
					for (String line : (String[]) reader.lines().toArray()) {
						System.out.println(line);
					}
					EventListener.sendMessage("Check system logs for stats. (Temporary)", event.getChannel());
					return;
//					if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//						String reply = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
//						System.out.println("Fortnite stats information:\n"+reply);
//						String solo = "";
//						String duo = "";
//						String squad = "";
//						EmbedBuilder embed = new EmbedBuilder();
//						embed.withAuthorName(username+" - "+embedPlatform);
//						embed.appendField("Solo", solo, true);
//						embed.appendField("Duo", duo, true);
//						embed.appendField("Squad", squad, true);
//						embed.withFooterText("fortnitetracker.com");
//						sendEmbed(embed, event.getChannel());
//						return;
//					}
//					EventListener.sendMessage("The user specified does not exist. Please try someone else.", event.getChannel());
//					return;
				} catch (IOException exception) {
					exception.printStackTrace();
					EventListener.sendMessage("An error occured when trying to retrieve Fortnite stats. Please try agian later.", event.getChannel());
					return;
				}
			} else {
				EventListener.sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"fortnite [pc|ps4|xbox] [username]```", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"settings prefix")) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				if (message.split(" ").length != 3) {
					sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"settings prefix [new prefix]```", event.getChannel());
					return;
				} else {
					String newPrefix = message.split(" ")[2];
					if (newPrefix.length() != 1) {
						sendMessage(":x: A prefix can only be 1 character long.", event.getChannel());
						return;
					}
					if (newPrefix.toUpperCase() != newPrefix) {
						sendMessage(":x: A prefix cannot be a letter.", event.getChannel());
						return;
					}
					XTBot.prefix = newPrefix;
					sendMessage(":white_check_mark: Prefix successfully changed to `"+XTBot.prefix+"`", event.getChannel());
					return;
				}
			} else {
				sendMessage(":x: Insufficient permission.", event.getChannel());
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"settings") && message.split(" ").length == 1) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("XT Bot Settings");
				embed.appendField(":exclamation: **Prefix**", "`"+XTBot.prefix+"settings prefix [new prefix]`", true);
				sendEmbed(embed, event.getChannel());
				return;
			} else {
				sendMessage(":x: Insufficient permission.", event.getChannel());
				return;
			}
		}
	}

	@EventSubscriber
	public void onUserVoiceLeave(UserVoiceChannelLeaveEvent event) {
		IVoiceChannel channel = event.getVoiceChannel();
		if (event.getGuild().getConnectedVoiceChannel() == channel) {
			if (channel.getConnectedUsers().size() == 1) channel.leave();
		}
	}

	public static void sendMessage(String message, IChannel channel) throws DiscordException, MissingPermissionsException {
		channel.sendMessage("\u200B"+message);
	}

	public static void sendEmbed(EmbedBuilder embed, IChannel channel) throws DiscordException, MissingPermissionsException {
		channel.sendMessage(embed.build());
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

	public void playAllStar(MessageReceivedEvent event, IVoiceChannel channel) {
		Music music = new Music();
		music.loadAndPlay(event.getChannel(), channel, "https://archive.org/download/AllStar/SmashMouth-AllStar_64kb.mp3");
	}
}
