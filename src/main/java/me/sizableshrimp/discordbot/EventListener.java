package me.sizableshrimp.discordbot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
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
			sendMessage("Hello! I am XT Bot. I don't do much yet because I am still in development. Commands:\n`"+XTBot.prefix+"hey`\n`"+XTBot.prefix+"info`\n`"+XTBot.prefix+"settings`More commands will be coming in the future!", event);
			return;
		}
		if (message.startsWith(XTBot.prefix+"info")) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Information");
			embed.appendDesc("This bot is built with [Spring Boot 2.0.3](https://spring.io/projects/spring-boot) and hosted on [Heroku](https://dashboard.heroku.com). It is coded in Java using the Discord4J library.");
			embed.appendField("Author", "SizableShrimp", true);
			embed.appendField("Discord4J Version", "2.10.1", true);
			embed.appendField("Prefix", XTBot.prefix, false);
			DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			embed.appendField("Uptime", formatter.format(System.currentTimeMillis()-XTBot.firstOnline), false);
			new MessageBuilder(XTBot.client).appendContent("To find out my commands, use `"+XTBot.prefix+"help`").withEmbed(embed.build()).withChannel(event.getChannel()).build();
		}
		if (message.startsWith(XTBot.prefix+"hey")) {
			sendMessage("Hello! :smile:", event);
			return;
		}
		if (message.startsWith(XTBot.prefix+"settings prefix")) {
			if (event.getChannel().getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				if (message.split(" ").length != 3) {
					sendMessage("Incorrect usage. Correct usage: ```"+XTBot.prefix+"settings prefix [new prefix]```", event);
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

	public void sendMessage(String message, MessageReceivedEvent event) throws DiscordException, MissingPermissionsException {
		new MessageBuilder(XTBot.client).appendContent("\u200B"+message).withChannel(event.getChannel()).build();
	}

	public void sendEmbed(EmbedBuilder embed, MessageReceivedEvent event) throws DiscordException, MissingPermissionsException {
		new MessageBuilder(XTBot.client).withEmbed(embed.build()).withChannel(event.getChannel()).build();
	}
}
