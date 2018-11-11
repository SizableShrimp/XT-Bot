package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ajbrown.namemachine.Gender;
import org.ajbrown.namemachine.NameGenerator;
import org.json.JSONException;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.PermissionUtils;
import sx.blah.discord.util.RequestBuffer;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventListener {
    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor() == null /*webhook*/ || event.getAuthor().isBot() || event.getChannel().isPrivate())
            return;
        String prefix = Bot.getPrefix(event.getGuild());
        String message = event.getMessage().getContent();
        IChannel channel = event.getChannel();
        String[] args = Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length);
        if (message.toLowerCase().startsWith(prefix + "help") || (!message.contains("@everyone") && !message.contains("@here") && event.getMessage().getMentions().contains(Bot.client.getOurUser()))) {
            sendMessage("Hello! I am XT Bot. My commands are:\n```" +
                    prefix + "hey\n" +
                    prefix + "info\n" +
                    prefix + "music\n" +
                    prefix + "fortnite or " + prefix + "ftn\n" +
                    prefix + "settings```", channel);
        } else if (message.toLowerCase().startsWith(prefix + "info")) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.withAuthorName("Information");
            embed.appendDesc("This bot is built with [Spring Boot](https://spring.io/projects/spring-boot). It is coded in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.");
            embed.appendField("Author", "SizableShrimp", true);
            embed.appendField("Discord4J Version", "2.10.2-SNAPSHOT", true);
            embed.appendField("Prefix", prefix, false);
            embed.appendField("Uptime", getUptime(), false);
            new MessageBuilder(Bot.client).appendContent("To find out my commands, use `" + prefix + "help`").withEmbed(embed.build()).withChannel(channel).build();
        } else if (message.toLowerCase().startsWith(prefix + "fortnite") || message.toLowerCase().startsWith(prefix + "ftn")) {
            if (args.length < 2) {
                incorrectUsage("fortnite [pc|ps4|xbox] [username]```", channel);
                return;
            }
            String platform = args[0].toLowerCase();
            if (!platform.equals("pc") && !platform.equals("ps4") && !platform.equals("xbox")) {
                incorrectUsage("fortnite [pc|ps4|xbox] [username]```", channel);
                return;
            }
            String username = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            try {
                HttpsURLConnection conn = (HttpsURLConnection) new URL("https://api.fortnitetracker.com/v1/profile/" + platform + "/" + username).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "XT Bot (github.com/SizableShrimp/XT-Bot)");
                conn.setRequestProperty("TRN-Api-Key", System.getenv("FORTNITE_KEY"));
                conn.connect();
                if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    sendMessage("A good connection was not established. Please try again later.", channel);
                    return;
                }
                JsonNode json = new ObjectMapper().readTree(conn.getInputStream());
                if (json.has("error") && json.path("error").asText().equals("Player Not Found")) {
                    sendMessage("The user specified could not be found. Please try a different name or platform.", channel);
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder();
                embed.withAuthorName(json.path("epicUserHandle").asText() + " | " + json.path("platformNameLong").asText());
                embed.appendField("Solos", getSolos(json), true);
                embed.appendField("Duos", getDuos(json), true);
                embed.appendField("Squads", getSquads(json), true);
                embed.appendField("Lifetime", getLifetime(json), false);
                embed.withFooterText("fortnitetracker.com");
                embed.withColor(new Color(74, 134, 232));
                sendEmbed(embed, channel);
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("An error occurred when trying to fetch the stats. Please try again later.", channel);
            } catch (JSONException e) {
                e.printStackTrace();
                sendMessage("An error occurred when trying to parse the stats. Please try again later.", channel);
            }
        } else if (message.toLowerCase().startsWith(prefix + "hey")) {
            sendMessage("Hello! :smile:", channel);
        } else if (message.toLowerCase().startsWith(prefix + "newname")) {
            if (!PermissionUtils.hasPermissions(channel, Bot.client.getOurUser(), Permissions.MANAGE_NICKNAMES)) {
                deleteLater(7, event.getMessage(), channel.sendMessage("\u200B:x: I need the **Manage Nicknames** permission to change nicknames."));
                return;
            }
            if (PermissionUtils.isUserHigher(event.getGuild(), event.getAuthor(), Bot.client.getOurUser())) {
                deleteLater(7, event.getMessage(), channel.sendMessage("\u200B:x: I do not have permission to change your nickname. (This command does not work for admins.)"));
                return;
            }
            if (args.length != 1) {
                deleteLater(7, event.getMessage(), incorrectUsage("newname [male|female]", channel));
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
                    deleteLater(7, event.getMessage(), incorrectUsage("newname [male|female]", channel));
                    return;
            }
            String name = new NameGenerator().generateName(isMale ? Gender.MALE : Gender.FEMALE).getFirstName();
            event.getGuild().setUserNickname(event.getAuthor(), name);
            deleteLater(7, event.getMessage(), channel.sendMessage("\u200B:white_check_mark: Your name has been changed to `" + name + "`."));
        } else if (message.toLowerCase().startsWith(prefix + "settings prefix")) {
            if (PermissionUtils.hasPermissions(event.getGuild(), event.getAuthor(), Permissions.MANAGE_SERVER)) {
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
                    sendMessage(":white_check_mark: Prefix successfully changed to `" + prefix + "`", channel);
                }
            } else {
                noPermission("Manage Server", channel);
            }
        } else if (message.toLowerCase().startsWith(prefix + "settings") && args.length == 0) {
            if (PermissionUtils.hasPermissions(event.getGuild(), event.getAuthor(), Permissions.MANAGE_SERVER)) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.withAuthorName("XT Bot Settings");
                embed.appendField("**Prefix**", "`" + prefix + "settings prefix [new prefix]`", true);
                sendEmbed(embed, channel);
            } else {
                noPermission("Manage Server", channel);
            }
        }
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        RequestBuffer.request(() -> Bot.client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "a random thing"));
    }

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
        return channel.sendMessage("\u200B" + message);
    }

    public static IMessage sendEmbed(EmbedBuilder embed, IChannel channel) {
        return channel.sendMessage(embed.build());
    }

    public static IMessage incorrectUsage(String usage, IChannel channel) {
        return sendMessage("Incorrect usage. Please use: ```" + Bot.getPrefix(channel.getGuild()) + usage + "```", channel);
    }

    private static IMessage noPermission(String permission, IChannel channel) {
        return sendMessage(":x: Insufficient permission. You can do this command if you have the **" + permission + "** permission.", channel);
    }

    private static void deleteLater(Integer seconds, IMessage... messages) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            for (IMessage m : messages) RequestBuffer.request(m::delete);
        }, seconds, TimeUnit.SECONDS);
    }

    private String getUptime() {
        long uptime = System.currentTimeMillis() - Bot.firstOnline;
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.HOURS.toMinutes(hours) - TimeUnit.DAYS.toMinutes(days);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.DAYS.toSeconds(days);
        List<String> formats = new ArrayList<>();
        if (days > 0) formats.add(days == 1 ? Long.toString(days) + " day" : Long.toString(days) + " days");
        if (hours > 0) formats.add(hours == 1 ? Long.toString(hours) + " hour" : Long.toString(hours) + " hours");
        if (minutes > 0)
            formats.add(minutes == 1 ? Long.toString(minutes) + " minute" : Long.toString(minutes) + " minutes");
        if (seconds > 0)
            formats.add(seconds == 1 ? Long.toString(seconds) + " second" : Long.toString(seconds) + " seconds");
        if (formats.size() == 0) return "Less than a second";
        if (formats.size() == 2) return formats.get(0) + " and " + formats.get(1);
        if (formats.size() == 3) return formats.get(0) + ", " + formats.get(1) + ", and " + formats.get(2);
        if (formats.size() == 4)
            return formats.get(0) + ", " + formats.get(1) + ", " + formats.get(2) + ", and " + formats.get(3);
        return formats.get(0);
    }

    private String getSolos(JsonNode json) {
        StringBuilder main = new StringBuilder();
        if (!json.path("stats").has("p2")) return "No stats found for Solos.";
        main.append("**Matches:** ").append(json.path("stats").path("p2").path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(json.path("stats").path("p2").path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(json.path("stats").path("p2").path("winRatio").path("displayValue").asText()).append("%");
        main.append("\n**Top 10:** ").append(json.path("stats").path("p2").path("top10").path("displayValue").asText());
        main.append("\n**Top 25:** ").append(json.path("stats").path("p2").path("top25").path("displayValue").asText());
        main.append("\n**Kills:** ").append(json.path("stats").path("p2").path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(json.path("stats").path("p2").path("kd").path("displayValue").asText());
        return main.toString();
    }

    private String getDuos(JsonNode json) {
        StringBuilder main = new StringBuilder();
        if (!json.path("stats").has("p10")) return "No stats found for Duos.";
        main.append("**Matches:** ").append(json.path("stats").path("p10").path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(json.path("stats").path("p10").path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(json.path("stats").path("p10").path("winRatio").path("displayValue").asText()).append("%");
        main.append("\n**Top 5:** ").append(json.path("stats").path("p10").path("top5").path("displayValue").asText());
        main.append("\n**Top 12:** ").append(json.path("stats").path("p10").path("top12").path("displayValue").asText());
        main.append("\n**Kills:** ").append(json.path("stats").path("p10").path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(json.path("stats").path("p10").path("kd").path("displayValue").asText());
        return main.toString();
    }

    private String getSquads(JsonNode json) {
        StringBuilder main = new StringBuilder();
        if (!json.path("stats").has("p9")) return "No stats found for Squads.";
        main.append("**Matches:** ").append(json.path("stats").path("p9").path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(json.path("stats").path("p9").path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(json.path("stats").path("p9").path("winRatio").path("displayValue").asText()).append("%");
        main.append("\n**Top 3:** ").append(json.path("stats").path("p9").path("top3").path("displayValue").asText());
        main.append("\n**Top 6:** ").append(json.path("stats").path("p9").path("top6").path("displayValue").asText());
        main.append("\n**Kills:** ").append(json.path("stats").path("p9").path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(json.path("stats").path("p9").path("kd").path("displayValue").asText());
        return main.toString();
    }

    private String getLifetime(JsonNode json) {
        StringBuilder main = new StringBuilder();
        JsonNode stats = json.path("lifeTimeStats");
        Double matches = (Double.valueOf(stats.path(7).path("value").asText()));
        Double wins = (Double.valueOf(stats.path(8).path("value").asText()));
        String percent = matches == 0 ? "" : new BigDecimal((Double.valueOf(stats.path(8).path("value").asText())) / matches * 100).setScale(1, RoundingMode.HALF_UP).toString();
        main.append("**Matches:** ").append(NumberFormat.getInstance().format(matches));
        main.append("\n**Wins:** ").append(NumberFormat.getInstance().format(wins));
        main.append("\n**Win Percentage:** ").append(percent).append("0%");
        main.append("\n**Top 10:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.path(3).path("value").asText())));
        main.append("\n**Top 25:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.path(5).path("value").asText())));
        main.append("\n**Kills:** ").append(NumberFormat.getInstance().format(Double.valueOf(stats.path(10).path("value").asText())));
        main.append("\n**K/D:** ").append(stats.path(11).path("value").asText());
        return main.toString();
    }
}