package me.sizableshrimp.discordbot;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class EventListener {
	@EventSubscriber
	public void onMessageEvent(MessageReceivedEvent event) {
		String message = event.getMessage().getContent();
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
		new MessageBuilder(XTBot.client).appendContent(/*add whitespace here*/message).withChannel(event.getChannel()).build();
	}
	
	public void sendEmbed(EmbedBuilder embed, MessageReceivedEvent event) throws DiscordException, MissingPermissionsException {
		RequestBuffer.request(() -> event.getChannel().sendMessage(embed.build()));
	}
}
