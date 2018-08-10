package me.sizableshrimp.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

public class BotClient {
	public static IDiscordClient createClient(String token, boolean login) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		try {
			return login ? clientBuilder.login() : clientBuilder.build();
		} catch (DiscordException e) {
			e.printStackTrace();
			return null;
		}
	}
}
