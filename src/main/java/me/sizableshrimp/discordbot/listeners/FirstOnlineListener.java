package me.sizableshrimp.discordbot.listeners;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.sizableshrimp.discordbot.Bot;
import reactor.core.publisher.Mono;

public class FirstOnlineListener extends EventListener<ReadyEvent> {
    private static boolean alreadyRegistered = false; //accounts for multiple shards

    public FirstOnlineListener() {
        super(ReadyEvent.class);
    }

    @Override
    protected Mono<Void> execute(ReadyEvent event) {
        return Mono.empty();
    }

    @Override
    public void register(EventDispatcher dispatcher) {
        if (alreadyRegistered) {
            return;
        }
        dispatcher.on(type)
                .next()
                //sets firstOnline to the time when the first ready event is received
                .doOnNext(ignored -> Bot.setFirstOnline(System.currentTimeMillis()))
                .doOnNext(event -> Bot.schedule(event.getClient()))
                .subscribe();
        alreadyRegistered = true;
    }
}
