package me.sizableshrimp.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.shard.ShardingClientBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class DiscordConfiguration {
    private DiscordConfiguration() {}

    /**
     * Returns a {@link Mono} which logs into all shards from 0 to the recommended shard count and registers events.
     *
     * @return A {@code Mono<Void>} which logs into all shards when subscribed to.
     */
    static Mono<Void> login() {
        return login(true)
                .flatMap(DiscordClient::login)
                .then();
    }

    /**
     * Returns a {@link Flux} which emits a built {@link DiscordClient} for all shards from 0 to the recommended
     * shard count.
     *
     * @param registerEvents If true, events will be registered. Otherwise, no events will be registered. Setting
     *                       this to false is meant for debugging.
     * @return A Flux which emits a {@link DiscordClient} for all shards that are not yet logged in.
     */
    static Flux<DiscordClient> login(boolean registerEvents) {
        return login(registerEvents, Bot.getConfig().getProperty("BOT_TOKEN"));
    }

    /**
     * Returns a {@link Flux} which emits a built {@link DiscordClient} for all shards from 0 to the recommended
     * shard count.
     *
     * @param registerEvents If true, events will be registered. Otherwise, no events will be registered. Setting
     *                       this to false is meant for debugging.
     * @param token          The token of the discord bot.
     * @return A Flux which emits a {@link DiscordClient} for all shards that are not yet logged in.
     */
    static Flux<DiscordClient> login(boolean registerEvents, String token) {
        return new ShardingClientBuilder(token)
                .build()
                .map(shard -> shard.setEventScheduler(Schedulers.immediate()))
                .map(DiscordClientBuilder::build)
                .doOnNext(client -> {
                    if (registerEvents) {
                        registerEvents(client);
                    }
                }).doOnNext(client -> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    client.logout().block();
                    System.out.println("oof");
                })));
    }

    private static void registerEvents(DiscordClient client) {
        EventHandler.register(client.getEventDispatcher());
    }
}
