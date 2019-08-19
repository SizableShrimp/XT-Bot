package me.sizableshrimp.discordbot.listeners;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.util.Snowflake;
import me.sizableshrimp.discordbot.Util;
import me.sizableshrimp.discordbot.music.Music;
import reactor.core.publisher.Mono;

public class VCLeaveListener extends EventListener<VoiceStateUpdateEvent> {

    public VCLeaveListener() {
        super(VoiceStateUpdateEvent.class);
    }

    //reset the music player if user leaves channel and bot is the only one in it
    @Override
    protected Mono<Void> execute(VoiceStateUpdateEvent event) {
        Snowflake guildId = event.getCurrent().getGuildId();

        if (Music.connections.get(guildId) == null) {
            return Mono.empty();
        }

        return Mono.justOrEmpty(event.getOld())
                .flatMap(VoiceState::getChannel)
                .filterWhen(old -> event.getCurrent().getChannel().hasElement().map(connected -> !connected)) //user left channel
                .doOnNext(old -> Music.getGuildManager(event.getClient(), guildId).usersSkipping.remove(event.getCurrent().getUserId()))
                .filterWhen(Util::isBotInVoiceChannel)
                .filterWhen(old -> Util.isBotAlone(event.getClient(), guildId))
                .doOnNext(ignored -> Music.disconnectBotFromChannel(guildId))
                .then();
    }

    @Override
    public void register(EventDispatcher dispatcher) {
        dispatcher.on(VoiceStateUpdateEvent.class)
                .filter(event -> event.getClient().getSelfId()
                        .map(id -> !id.equals(event.getCurrent().getUserId()))
                        .orElse(false)) //don't want bot user
                .flatMap(this::execute)
                .subscribe();
    }
}
