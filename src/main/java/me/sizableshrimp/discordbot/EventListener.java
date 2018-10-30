package me.sizableshrimp.discordbot;

//import me.sizableshrimp.discordbot.party.PartyEvents;

import org.ajbrown.namemachine.Gender;
import org.ajbrown.namemachine.NameGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.PermissionUtils;
import sx.blah.discord.util.RequestBuffer;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import sx.blah.discord.handle.impl.events.ReadyEvent;

public class EventListener {
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		RequestBuffer.request(() -> {
			if (event.getAuthor().isBot() || event.getChannel().isPrivate()) return;
			String message = event.getMessage().getContent();
			IChannel channel = event.getChannel();
			String[] args = Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length);
			if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"help") || (!message.contains("@everyone") && !message.contains("@here") && event.getMessage().getMentions().contains(Bot.client.getOurUser()))) {
				sendMessage("Hello! I am XT Bot. My commands are:\n```"+
						Bot.getPrefix(event.getGuild())+"hey\n"+
						Bot.getPrefix(event.getGuild())+"info\n"+
						Bot.getPrefix(event.getGuild())+"music\n"+
						Bot.getPrefix(event.getGuild())+"fortnite or "+Bot.getPrefix(event.getGuild())+"ftn\n"+
						Bot.getPrefix(event.getGuild())+"settings```", channel);
            } else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"info")) {
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("Information");
				embed.appendDesc("This bot is built with [Spring Boot](https://spring.io/projects/spring-boot). It is coded in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.");
				embed.appendField("Author", "SizableShrimp", true);
				embed.appendField("Discord4J Version", "2.10.2-SNAPSHOT", true);
				embed.appendField("Prefix", Bot.getPrefix(event.getGuild()), false);
				embed.appendField("Uptime", getUptime(), false);
				new MessageBuilder(Bot.client).appendContent("To find out my commands, use `"+Bot.getPrefix(event.getGuild())+"help`").withEmbed(embed.build()).withChannel(channel).build();
			} else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"fortnite") || message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"ftn")) {
				if (args.length >= 2) {
					String platform = args[0];
					if (platform.equalsIgnoreCase("pc") || platform.equalsIgnoreCase("ps4") || platform.equalsIgnoreCase("xbox")) {
						platform = platform.toLowerCase();
					} else {
						incorrectUsage("fortnite [pc|ps4|xbox] [username]```", channel);
						return;
					}
					StringBuilder username = new StringBuilder();
					username.append(args[1]);
					for (int i = 2; i < args.length; i++) username.append(" ").append(args[i]);
					try {
						HttpsURLConnection conn = (HttpsURLConnection) new URL("https://api.fortnitetracker.com/v1/profile/"+platform+"/"+username.toString()).openConnection();
						conn.setRequestMethod("GET");
						conn.setRequestProperty("User-Agent", "XT Bot (github.com/SizableShrimp/XT-Bot)");
						conn.setRequestProperty("TRN-Api-Key", System.getenv("FORTNITE_KEY"));
						conn.connect();
						if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
							String inputLine;
							StringBuilder response = new StringBuilder();
							while ((inputLine = reader.readLine()) != null) response.append(inputLine);
							reader.close();
							JSONObject json = new JSONObject(response.toString());
							try {
								if (json.getString("error").equals("Player Not Found")) {
									sendMessage("The user specified could not be found. Please try a different name or platform.", channel);
									return;
								}
							} catch (JSONException ignored) {}
							EmbedBuilder embed = new EmbedBuilder();
							embed.withAuthorName(json.getString("epicUserHandle")+" | "+json.getString("platformNameLong"));
							embed.appendField("Solos", getSolos(json), true);
							embed.appendField("Duos", getDuos(json), true);
							embed.appendField("Squads", getSquads(json), true);
							embed.appendField("Lifetime", getLifetime(json), false);
							embed.withFooterText("fortnitetracker.com");
							embed.withColor(74, 134, 232);
							sendEmbed(embed, channel);
							return;
						}
						sendMessage("A good connection was not established. Please try again later.", channel);
                    } catch (IOException e) {
						e.printStackTrace();
						sendMessage("An error occured when trying to fetch the stats. Please try again later.", channel);
                    } catch (JSONException e) {
						e.printStackTrace();
						sendMessage("An error occured when trying to parse the stats. Please try again later.", channel);
                    }
				} else {
					incorrectUsage("fortnite [pc|ps4|xbox] [username]```", channel);
                }
			} else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"hey")) {
				sendMessage("Hello! :smile:", channel);
            } else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"newname")) {
				if (event.getGuild().getOwner().equals(event.getAuthor()) 
						|| !PermissionUtils.hasPermissions(channel, Bot.client.getOurUser(), Permissions.MANAGE_NICKNAMES) 
						|| PermissionUtils.isUserHigher(event.getGuild(), event.getAuthor(), Bot.client.getOurUser())) {
					deleteLater(7, event.getMessage(), channel.sendMessage("\u200B:x: I do not have permission to change your nickname. (This command does not work for admins.)"));
				} else {
					if (args.length != 1) {
						deleteLater(7, event.getMessage(), incorrectUsage("newname (male|female)", channel));
						return;
					}
					boolean isMale;
                    switch (args[0]) {
                        case "male":
                            isMale = true;
                            break;
                        case "female":
                            isMale = false;
                            break;
                        default:
                            deleteLater(7, event.getMessage(), incorrectUsage("newname (male|female)", channel));
                            return;
                    }
					String name = new NameGenerator().generateName(isMale ? Gender.MALE : Gender.FEMALE).getFirstName();
					event.getGuild().setUserNickname(event.getAuthor(), name);
					deleteLater(7, event.getMessage(), channel.sendMessage("\u200B:white_check_mark: Your name has been changed to `"+name+"`."));
				}
            } else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"settings prefix")) {
				if (PermissionUtils.hasPermissions(channel, event.getAuthor(), Permissions.MANAGE_SERVER)) {
					if (args.length != 2) {
						incorrectUsage("settings prefix [new prefix]", channel);
                    } else {
						String newPrefix = args[1];
						if (newPrefix.length() != 1) {
							sendMessage(":x: A prefix can only be 1 character long.", channel);
							return;
						}
                        if (!newPrefix.toUpperCase().equals(newPrefix)) {
							sendMessage(":x: A prefix cannot be a letter.", channel);
							return;
						}
						Bot.setPrefix(event.getGuild(), newPrefix);
						sendMessage(":white_check_mark: Prefix successfully changed to `"+Bot.getPrefix(event.getGuild())+"`", channel);
                    }
				} else {
					noPermission("Manage Server", channel);
                }
			} else if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"settings") && args.length == 0) {
				if (PermissionUtils.hasPermissions(channel, event.getAuthor(), Permissions.MANAGE_SERVER)) {
					EmbedBuilder embed = new EmbedBuilder();
					embed.withAuthorName("XT Bot Settings");
					embed.appendField("**Prefix**", "`"+Bot.getPrefix(event.getGuild())+"settings prefix [new prefix]`", true);
					sendEmbed(embed, channel);
                } else {
					noPermission("Manage Server", channel);
				}
			}
		});
	}

//	@EventSubscriber
//	public void onReady(ReadyEvent event) {
//		RequestBuffer.request(() -> Bot.client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "a random thing"));
//		Bot.client.getDispatcher().registerListener(new PartyEvents());
//	}

	@EventSubscriber
	public void onGuildReceive(GuildCreateEvent event) {
//		String prefix = Bot.retrieveSQLPrefix(event.getGuild().getLongID());
//		if (prefix != null) {
//			Bot.setPrefix(event.getGuild(), prefix);
//		} else {
//			Bot.setPrefix(event.getGuild(), ",");
//			Bot.insertGuild(event.getGuild().getLongID(), ",");
//		}
		Bot.setPrefix(event.getGuild(), ",");
	}

	@EventSubscriber
	public void onGuildLeave(GuildLeaveEvent event) {
		Bot.removePrefix(event.getGuild());
	}

	public static IMessage sendMessage(String message, IChannel channel) {
		return channel.sendMessage("\u200B"+message);
	}

	public static IMessage sendEmbed(EmbedBuilder embed, IChannel channel) {
		return channel.sendMessage(embed.build());
	}
	
	private static IMessage incorrectUsage(String usage, IChannel channel) {
		return sendMessage("Incorrect usage. Please use: ```"+Bot.getPrefix(channel.getGuild())+usage+"```", channel);
	}
	
	private static IMessage noPermission(String permission, IChannel channel) {
		return sendMessage(":x: Insufficient permission. You can do this command if you have the **"+permission+"** permission.", channel);
	}

	private static void deleteLater(Integer seconds, IMessage... messages) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            for (IMessage m : messages) m.delete();
        }, seconds, TimeUnit.SECONDS);
	}
	
	private String getUptime() {
		long uptime = System.currentTimeMillis()-Bot.firstOnline;
		long days = TimeUnit.MILLISECONDS.toDays(uptime);
		long hours = TimeUnit.MILLISECONDS.toHours(uptime) - TimeUnit.DAYS.toHours(days);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.HOURS.toMinutes(hours) - TimeUnit.DAYS.toMinutes(days);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.DAYS.toSeconds(days);
		List<String> formats = new ArrayList<>();
		if (days > 0) formats.add(days == 1 ? Long.toString(days) +" day" : Long.toString(days) +" days");
		if (hours > 0) formats.add(hours == 1 ? Long.toString(hours) +" hour" : Long.toString(hours) +" hours");
		if (minutes > 0) formats.add(minutes == 1 ? Long.toString(minutes)+" minute" : Long.toString(minutes) +" minutes");
		if (seconds > 0) formats.add(seconds == 1 ? Long.toString(seconds) +" second" : Long.toString(seconds) +" seconds");
		if (formats.size() == 0) return "Less than a second";
		if (formats.size() == 2) return formats.get(0)+" and "+formats.get(1);
		if (formats.size() == 3) return formats.get(0)+", "+formats.get(1)+", and "+formats.get(2);
		if (formats.size() == 4) return formats.get(0)+", "+formats.get(1)+", "+formats.get(2)+", and "+formats.get(3);
		return formats.get(0);
	}

	private String getSolos(JSONObject json) {
		try {
			StringBuilder main = new StringBuilder();
			if (json.getJSONObject("stats").optJSONObject("p2") == null) return "No stats found for Solos.";
			main.append("**Matches:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Percentage:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("winRatio").getString("displayValue")).append("%");
			main.append("\n**Top 10:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top10").getString("displayValue"));
			main.append("\n**Top 25:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top25").getString("displayValue"));
			main.append("\n**Kills:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** ").append(json.getJSONObject("stats").getJSONObject("p2").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Error when retrieving Solos stats.";
		}
	}

	private String getDuos(JSONObject json) {
		try {
			StringBuilder main = new StringBuilder();
			if (json.getJSONObject("stats").optJSONObject("p10") == null) return "No stats found for Duos.";
			main.append("**Matches:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Percentage:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("winRatio").getString("displayValue")).append("%");
			main.append("\n**Top 5:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top5").getString("displayValue"));
			main.append("\n**Top 12:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top12").getString("displayValue"));
			main.append("\n**Kills:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** ").append(json.getJSONObject("stats").getJSONObject("p10").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Error when retrieving Duos stats.";
		}
	}

	private String getSquads(JSONObject json) {
		try {
			StringBuilder main = new StringBuilder();
			if (json.getJSONObject("stats").optJSONObject("p9") == null) return "No stats found for Squads.";
			main.append("**Matches:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Percentage:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("winRatio").getString("displayValue")).append("%");
			main.append("\n**Top 3:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top3").getString("displayValue"));
			main.append("\n**Top 6:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top6").getString("displayValue"));
			main.append("\n**Kills:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** ").append(json.getJSONObject("stats").getJSONObject("p9").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Error when retrieving Squads stats.";
		}
	}

	private String getLifetime(JSONObject json) {
		try {
			StringBuilder main = new StringBuilder();
			JSONArray stats = json.getJSONArray("lifeTimeStats");
			Double matches = (Double.valueOf(stats.getJSONObject(7).getString("value")));
			Double wins = (Double.valueOf(stats.getJSONObject(8).getString("value")));
			String percent = matches == 0 ? "" : new BigDecimal((Double.valueOf(stats.getJSONObject(8).getString("value")))/matches*100).setScale(1, RoundingMode.HALF_UP).toString();
			main.append("**Matches:** ").append(NumberFormat.getInstance().format(matches));
			main.append("\n**Wins:** ").append(NumberFormat.getInstance().format(wins));
			main.append("\n**Win Percentage:** ").append(percent).append("0%");
			main.append("\n**Top 10:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.getJSONObject(3).getString("value"))));
			main.append("\n**Top 25:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.getJSONObject(5).getString("value"))));
			main.append("\n**Kills:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.getJSONObject(10).getString("value"))));
			main.append("\n**K/D:** ").append(stats.getJSONObject(11).getString("value"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Error when retrieving Lifetime stats.";
		}
	}
}
