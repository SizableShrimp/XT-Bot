package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
public class Bot {
    private static final String CONFIG_FILENAME = "bot.config";
    private static final Config CONFIG = loadConfig(CONFIG_FILENAME);
    public static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static MongoDatabase database;
    private static String prefix;
    private static long firstOnline;

    public static void main(String[] args) {
        SpringApplication.run(Bot.class, args);
        load();
        DiscordConfiguration.login().subscribe();
    }

    private static void load() {
        if (getConfig() == null) {
            System.exit(-1);
        }

        LOGGER.info("{} config loaded.", CONFIG_FILENAME.substring(0, CONFIG_FILENAME.length() - 7));

        MongoClientURI uri = new MongoClientURI(CONFIG.getProperty("MONGO_URI"));
        try (MongoClient mongoClient = new MongoClient(uri)) {
            database = mongoClient.getDatabase("Bot");
        }
    }

    /**
     * Schedules the main events that run on a timer
     *
     * @param client A client from any shard used to get channels by ID
     */
    public static void schedule(DiscordClient client) {
        //YoutubeListener.schedule(client);

        MongoCollection<Document> collection = database.getCollection("Main Server");
        Document doc = collection.find(Filters.eq("_id", new ObjectId("5d0ab9211c9d440000f22de8"))).first();
        if (doc == null) doc = new Document();

        if (doc.getBoolean("enabled", false)) {
            Mono<Message> message420 = client.getChannelById(Snowflake.of(doc.getString("greeting_channel")))
                    .ofType(GuildMessageChannel.class)
                    .flatMap(Bot::create420Message);
            Flux.interval(Util.happy420(), Duration.ofDays(1))
                    .flatMap(l -> message420)
                    .subscribe();
        }

        boolean heroku = System.getenv().containsKey("HEROKU");

        if (heroku && System.getenv("HEROKU").equalsIgnoreCase("true")) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(System.getenv("URL")).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    connection.getResponseCode();
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.MINUTES);
        }
    }

    private static Mono<Message> create420Message(MessageChannel channel) {
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        boolean ultimate420 = time.getMonth() == Month.APRIL && time.getDayOfMonth() == 20;
        String message = ultimate420 ? "@everyone Happy ULTIMATE 420!!!" : "Happy 420!";
        return Util.sendMessage(message, channel);
    }

    public static long getFirstOnline() {
        return firstOnline;
    }

    public static void setFirstOnline(long millis) {
        firstOnline = millis;
    }

    public static String getPrefix() {
        if (prefix == null) {
            prefix = CONFIG.getProperty("PREFIX");
        }
        return prefix;
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    private static Config loadConfig(String filename) {
        // CONFIG
        Properties properties = new Properties();
        try {
            InputStream is = new FileInputStream(filename);
            properties.load(is);
        } catch (FileNotFoundException ex) {
            //logger doesn't work until after config is loaded
            System.out.println("Could not find config file");
        } catch (IOException ex) {
            System.out.println("Could not load config file");
        }
        return new Config(properties);
    }
}
