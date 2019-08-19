package me.sizableshrimp.discordbot.commands.fun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.commands.Command;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

public class FortniteCommand extends Command {

    @Override
    public CommandInfo getInfo() {
        return new CommandInfo("%cmdname% [pc|ps4|xbox] [username]",
                "Returns Fortnite stats about the user including wins, kills, top placements, and more. Includes Solos, Duos, Squads, and Lifetime.");
    }

    @Override
    public Set<String> getNames() {
        return Set.of("fortnite", "ftn");
    }

    @Override
    protected Mono<Message> run(MessageCreateEvent event, String[] args) {
        if (args.length < 2) {
            return incorrectUsage(event);
        }
        String platform = args[0].toLowerCase();
        if (!platform.equals("pc") && !platform.equals("ps4") && !platform.equals("xbox")) {
            return incorrectUsage(event);
        }
        String username = String.join(" ", Arrays.copyOfRange(args, 1, args.length)); //exclude platform

        return getJson(platform, username)
                .flatMap(json -> {
                    if (json.has("error")) {
                        if (json.path("error").asText().equals("Player Not Found")) {
                            return event.getMessage().getChannel().flatMap(c -> sendMessage("The user specified could not be found. Please try a different name or platform.", c));
                        }
                        try {
                            return Mono.error(new Exception(Bot.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json)));
                        } catch (JsonProcessingException e) {
                            return Mono.error(new Exception("Error while trying to throw error response from Fortnite API.", e));
                        }
                    }
                    return event.getMessage().getChannel().flatMap(c -> sendEmbed(createEmbed(json, platform,
                            username), c));
                })
                .switchIfEmpty(event.getMessage().getChannel().flatMap(c ->
                        sendMessage("A good connection was not established. Please try again later.", c)))
                .onErrorResume(throwable -> {
                    LoggerFactory.getLogger(getClass()).error("An error occurred when fetching Fortnite stats.", throwable);
                    return event.getMessage().getChannel().flatMap(c -> sendMessage("An error occurred when running this command. Please try again later.", c));
                });
    }

    private Consumer<EmbedCreateSpec> createEmbed(JsonNode json, String platform, String username) {
        String url = String.format("https://fortnitetracker.com/profile/%s/%s", platform, URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20"));
        String title = json.path("epicUserHandle").asText() + " | " + json.path("platformNameLong").asText();
        JsonNode stats = json.path("stats");

        return embed -> {
            embed.setAuthor(title, url, null);
            embed.addField("Solos", getSolos(stats), true);
            embed.addField("Duos", getDuos(stats), true);
            embed.addField("Squads", getSquads(stats), true);
            embed.addField("Lifetime", getLifetime(json), false);
            embed.setFooter("fortnitetracker.com | Click on the username to be taken to the Fortnite Tracker website.",
                    "https://pbs.twimg.com/profile_images/966414667596808193/dyH-Qrz8_400x400.jpg");
            embed.setColor(new Color(74, 134, 232));
        };
    }

    private Mono<JsonNode> getJson(String platform, String username) {
        return Mono.fromCallable(() -> {
            String url = "https://api.fortnitetracker.com/v1/profile/" + platform + "/" + username;
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "XT-Bot");
            conn.setRequestProperty("TRN-Api-Key", Bot.getConfig().getProperty("FORTNITE_KEY"));
            conn.connect();
            return conn;
        }).filter(conn -> {
            try {
                return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                return false;
            }
        }).flatMap(conn -> Mono.fromCallable(conn::getInputStream))
                .flatMap(stream -> Mono.fromCallable(() -> Bot.MAPPER.readTree(stream)));
    }

    private String getSolos(JsonNode stats) {
        if (!stats.has("p2")) {
            return "No stats found for Solos.";
        }

        StringBuilder main = new StringBuilder();
        JsonNode solos = stats.path("p2");

        main.append("**Matches:** ").append(solos.path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(solos.path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(solos.path("winRatio").path("displayValue").asText()).append("%");
        main.append("\n**Top 10:** ").append(solos.path("top10").path("displayValue").asText());
        main.append("\n**Top 25:** ").append(solos.path("top25").path("displayValue").asText());
        main.append("\n**Kills:** ").append(solos.path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(solos.path("kd").path("displayValue").asText());

        return main.toString();
    }

    private String getDuos(JsonNode stats) {
        if (!stats.has("p10")) {
            return "No stats found for Duos.";
        }

        StringBuilder main = new StringBuilder();
        JsonNode duos = stats.path("p10");

        main.append("**Matches:** ").append(duos.path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(duos.path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(duos.path("winRatio").path("displayValue").asText()).append("%");
        main.append("\n**Top 5:** ").append(duos.path("top5").path("displayValue").asText());
        main.append("\n**Top 12:** ").append(duos.path("top12").path("displayValue").asText());
        main.append("\n**Kills:** ").append(duos.path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(duos.path("kd").path("displayValue").asText());

        return main.toString();
    }

    private String getSquads(JsonNode stats) {
        if (!stats.has("p9")) {
            return "No stats found for Squads.";
        }

        StringBuilder main = new StringBuilder();
        JsonNode squads = stats.path("p9");

        main.append("**Matches:** ").append(squads.path("matches").path("displayValue").asText());
        main.append("\n**Wins:** ").append(squads.path("top1").path("displayValue").asText());
        main.append("\n**Win Percentage:** ").append(squads.path("winRatio").path(
                "displayValue").asText()).append("%");
        main.append("\n**Top 3:** ").append(squads.path("top3").path("displayValue").asText());
        main.append("\n**Top 6:** ").append(squads.path("top6").path("displayValue").asText());
        main.append("\n**Kills:** ").append(squads.path("kills").path("displayValue").asText());
        main.append("\n**K/D:** ").append(squads.path("kd").path("displayValue").asText());

        return main.toString();
    }

    private String getLifetime(JsonNode json) {
        StringBuilder main = new StringBuilder();
        NumberFormat formatter = NumberFormat.getInstance();
        JsonNode lifeTimeStats = json.path("lifeTimeStats");

        Double matches = (Double.valueOf(lifeTimeStats.path(7).path("value").asText()));
        Double wins = (Double.valueOf(lifeTimeStats.path(8).path("value").asText()));
        String percent = "0";
        if (matches != 0) {
            percent = BigDecimal.valueOf(wins / matches * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toString();
        }
        String top10 = formatter.format(Double.valueOf(lifeTimeStats.path(3).path("value").asText()));
        String top25 = formatter.format(Double.valueOf(lifeTimeStats.path(5).path("value").asText()));
        String kills = formatter.format(Double.valueOf(lifeTimeStats.path(10).path("value").asText()));
        String kd = lifeTimeStats.path(11).path("value").asText();

        main.append("**Matches:** ").append(formatter.format(matches));
        main.append("\n**Wins:** ").append(formatter.format(wins));
        main.append("\n**Win Percentage:** ").append(percent).append("%");
        main.append("\n**Top 10:** ").append(top10);
        main.append("\n**Top 25:** ").append(top25);
        main.append("\n**Kills:** ").append(kills);
        main.append("\n**K/D:** ").append(kd);
        return main.toString();
    }
}