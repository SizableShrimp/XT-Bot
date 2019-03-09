package me.sizableshrimp.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Permission;
import discord4j.core.shard.ShardingClientBuilder;
import org.slf4j.LoggerFactory;
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
     * Returns a {@link Flux} which emits a built {@link DiscordClient} for all shards from 0 to the recommended shard count.
     *
     * @param registerEvents If true, events will be registered and {@link Bot#schedule} will be called. Otherwise, nothing will be scheduled or registered.
     *                       Setting this to false is meant for debugging.
     * @return A Flux which emits a {@link DiscordClient} for all shards that are not yet logged in.
     */
    static Flux<DiscordClient> login(boolean registerEvents) {
        return new ShardingClientBuilder(System.getenv("TOKEN"))
                .build()
                .map(shard -> shard.setEventScheduler(Schedulers.immediate())
                        .setInitialPresence(Presence.online(Activity.playing("a random thing"))))
                .map(DiscordClientBuilder::build)
                .doOnNext(client -> {
                    if (registerEvents) registerEvents(client);
                });
    }

    private static void registerEvents(DiscordClient client) {
        EventDispatcher dispatcher = client.getEventDispatcher();
        Mono.when(
                dispatcher.on(MessageCreateEvent.class)
                        .filterWhen(e -> e.getMessage().getChannel().map(c -> c.getType() == Channel.Type.GUILD_TEXT))
                        .filterWhen(e -> e.getMessage().getChannel().cast(TextChannel.class)
                                .flatMap(c -> Mono.justOrEmpty(client.getSelfId()).flatMap(c::getEffectivePermissions).map(set -> set.asEnumSet().contains(Permission.SEND_MESSAGES))))
                        .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(false))
                        .flatMap(EventListener::onMessageCreate)
                        .onErrorContinue((error, event) -> LoggerFactory.getLogger(Bot.class).error("Event listener had an uncaught exception!", error)),
                dispatcher.on(ReadyEvent.class)
                        .take(1)
                        .filter(ignored -> client.getConfig().getShardIndex() == 0) //only want to schedule once
                        .doOnNext(ignored -> Bot.setFirstOnline(System.currentTimeMillis())) //set the first online time on ready event of shard 0
                        .doOnNext(ignored -> Bot.schedule(client)),
                dispatcher.on(VoiceStateUpdateEvent.class)
                        .filter(event -> event.getClient().getSelfId()
                                .map(id -> !id.equals(event.getCurrent().getUserId()))
                                .orElse(false)) //don't want bot user
                        .flatMap(EventListener::onVoiceChannelLeave)).subscribe();
    }
}
