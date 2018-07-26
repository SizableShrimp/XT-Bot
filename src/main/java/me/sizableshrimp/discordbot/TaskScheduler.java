package me.sizableshrimp.discordbot;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;

import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MessageBuilder;

public class TaskScheduler extends AudioEventAdapter {	
	public void trackEnd(TrackEndEvent event) {
		IVoiceChannel channel = XTBot.client.getConnectedVoiceChannels().get(0);
		new MessageBuilder(XTBot.client).appendContent("\u200B"+"All Star has ended, leaving "+channel.getName()).withChannel(XTBot.client.getChannelByID(332985255151665152L)).build();
		channel.leave();
	}
}
