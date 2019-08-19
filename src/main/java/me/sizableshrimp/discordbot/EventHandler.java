package me.sizableshrimp.discordbot;

import discord4j.core.event.EventDispatcher;
import me.sizableshrimp.discordbot.listeners.EventListener;
import me.sizableshrimp.discordbot.loading.CommandLoader;

import java.util.Set;

class EventHandler {
    private static Set<EventListener> listeners;

    private EventHandler() {}

    static {
        listeners = new CommandLoader<>(EventListener.class).loadClasses();
    }

    static void register(EventDispatcher dispatcher) {
        listeners.forEach(listener -> listener.register(dispatcher));
    }
}
