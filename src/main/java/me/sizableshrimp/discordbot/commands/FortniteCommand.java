package me.sizableshrimp.discordbot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FortniteCommand extends Command {
    @Override
    public String getUsage() {
        return "fortnite [pc|ps4|xbox] [username]";
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("fortnite", "ftn").collect(Collectors.toSet());
    }

    @Override
    protected Mono run(MessageCreateEvent event, String[] args) {
        if (args.length < 2) {
            return incorrectUsage(event);
        }
        String platform;
        if (args[0].equalsIgnoreCase("pc") || args[0].equalsIgnoreCase("ps4") || args[0].equalsIgnoreCase("xbox")) {
            platform = args[0].toLowerCase();
        } else {
            return incorrectUsage(event);
        }
        StringBuilder username = new StringBuilder();
        username.append(args[1]);
        for (int i = 2; i < args.length; i++) {
            username.append(" ").append(args[i]);
        }

        return Mono.fromCallable(() -> {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://api.fortnitetracker.com/v1/profile/" + platform + "/" + username.toString()).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "XT-Bot");
            conn.setRequestProperty("TRN-Api-Key", System.getenv("FORTNITE_KEY"));
            conn.connect();
            return conn;
        })
                .filter(conn -> {
                    try {
                        return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .flatMap(conn -> Mono.fromCallable(conn::getInputStream))
                .flatMap(stream -> Mono.fromCallable(() -> new ObjectMapper().readTree(stream)))
                .flatMap(json -> {
                    if (json.has("error") && json.path("error").asText().equals("Player Not Found")) {
                        return event.getMessage().getChannel().flatMap(c -> sendMessage("The user specified could not be found. Please try a different name or platform.", c));
                    }
                    URL url;
                    try {
                        url = new URL("https://fortnitetracker.com/profile/" + platform + "/" + URLEncoder.encode(username.toString(), "UTF-8").replace("+", "%20"));
                    } catch (MalformedURLException | UnsupportedEncodingException e) {
                        url = null;
                    }
                    EmbedCreateSpec embed = new EmbedCreateSpec();
                    embed.setAuthor(json.path("epicUserHandle").asText() + " | " + json.path("platformNameLong").asText(), url == null ? null : url.toString(), null);
                    embed.addField("Solos", getSolos(json), true);
                    embed.addField("Duos", getDuos(json), true);
                    embed.addField("Squads", getSquads(json), true);
                    embed.addField("Lifetime", getLifetime(json), false);
                    embed.setFooter("fortnitetracker.com", "https://pbs.twimg.com/profile_images/966414667596808193/dyH-Qrz8_400x400.jpg");
                    embed.setColor(new Color(74, 134, 232));
                    return event.getMessage().getChannel().flatMap(c -> sendEmbed(embed, c));
                })
                .switchIfEmpty(event.getMessage().getChannel().flatMap(c -> sendMessage("A good connection was not established. Please try again later.", c)))
                .onErrorResume(throwable -> event.getMessage().getChannel().flatMap(c -> sendMessage("An error occurred when running this command. Please try again later.", c)));
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
        String percent = matches == 0 ? "" : BigDecimal.valueOf((Double.valueOf(stats.path(8).path("value").asText())) / matches * 100).setScale(1, RoundingMode.HALF_UP).toString();
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
