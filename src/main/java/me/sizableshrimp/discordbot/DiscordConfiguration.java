package me.sizableshrimp.discordbot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import discord4j.common.jackson.PossibleModule;
import discord4j.common.jackson.UnknownPropertyHandler;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.RestClient;
import discord4j.rest.http.ExchangeStrategies;
import discord4j.rest.http.client.DiscordWebClient;
import discord4j.rest.json.response.GatewayResponse;
import discord4j.rest.request.DefaultRouter;
import discord4j.rest.route.Routes;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

class DiscordConfiguration {
    private DiscordConfiguration() {}

    /**
     * Logs into all shards from 0 to the recommended shard count and registers events.
     * @see <a href="https://discordapp.com/developers/docs/topics/gateway#get-gateway-bot">Recommended Shard Count</a>
     */
    static void login() {
        DiscordClientBuilder builder = new DiscordClientBuilder(System.getenv("TOKEN"))
                .setInitialPresence(Presence.online(Activity.playing("a random thing")));
        getShardCount(builder.getToken())
                .map(shardCount -> IntStream.range(0, shardCount)
                        .mapToObj(i -> builder.setShardIndex(i).build())
                        .peek(DiscordConfiguration::registerEvents)
                        .map(DiscordClient::login)
                        .collect(Collectors.toList())
                ).flatMap(Mono::when)
                .block();
    }

    private static void registerEvents(DiscordClient client) {
        EventDispatcher dispatcher = client.getEventDispatcher();
        Mono.when(
                dispatcher.on(MessageCreateEvent.class)
                        .filterWhen(e -> e.getMessage().getChannel().map(c -> c.getType() == Channel.Type.GUILD_TEXT))
                        .filterWhen(e -> e.getMessage().getAuthor().map(u -> !u.isBot()))
                        .flatMap(EventListener::onMessageCreate)
                        .onErrorContinue((error, event) -> LoggerFactory.getLogger(Bot.class).error("Event listener had an uncaught exception!", error)),
                dispatcher.on(ReadyEvent.class)
                        .take(1)
                        .filter(ignored -> client.getConfig().getShardIndex() == 0) //only want to schedule once
                        .doOnNext(ignored -> Bot.setFirstOnline(System.currentTimeMillis())) //set the first online time on ready event of shard 0
                        .doOnNext(ignored -> Bot.schedule(client)),
                dispatcher.on(VoiceStateUpdateEvent.class)
                        .flatMap(EventListener::onVoiceStateUpdate)).subscribe();
    }

    private static Mono<Integer> getShardCount(String token) {
        final ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .addHandler(new UnknownPropertyHandler(false))
                .registerModules(new PossibleModule(), new Jdk8Module());

        HttpHeaders defaultHeaders = new DefaultHttpHeaders();
        defaultHeaders.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
        defaultHeaders.add(HttpHeaderNames.AUTHORIZATION, "Bot " + token);
        defaultHeaders.add(HttpHeaderNames.USER_AGENT, "DiscordBot(https://discord4j.com, v3)");
        HttpClient httpClient = HttpClient.create().baseUrl(Routes.BASE_URL).compress(true);

        DiscordWebClient webClient = new DiscordWebClient(httpClient, defaultHeaders,
                ExchangeStrategies.withJacksonDefaults(mapper));

        final RestClient restClient = new RestClient(new DefaultRouter(webClient));

        return restClient.getGatewayService().getGatewayBot().map(GatewayResponse::getShards);
    }
}
