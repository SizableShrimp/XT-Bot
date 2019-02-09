package me.sizableshrimp.discordbot.music;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import me.sizableshrimp.discordbot.Util;
import reactor.core.publisher.Mono;

import java.util.Set;

public enum MusicPermission {
    DJ, ALONE, NONE;

    public static Mono<MusicPermission> getPermission(Member member) {
        return isDJ(member)
                .filter(b -> b)
                .map(b -> MusicPermission.DJ)
                .switchIfEmpty(Util.isMemberAloneWithBot(member)
                        .map(b -> b ? MusicPermission.ALONE : MusicPermission.NONE)
                );
    }

    static Mono<Boolean> isDJ(Member member) {
        return Mono.zip(hasDJRole(member), hasManageChannels(member))
                .map(tuple -> tuple.getT1() || tuple.getT2());
    }

    private static Mono<Boolean> hasDJRole(Member member) {
        return member.getRoles()
                .any(role -> role.getName().equalsIgnoreCase("dj"));
    }

    private static Mono<Boolean> hasManageChannels(Member member) {
        return member.getBasePermissions().map(ps -> ps.contains(Permission.MANAGE_CHANNELS));
    }

    public static Mono<Boolean> requirePermissions(Member member, TextChannel channel,
                                                   Set<MusicPermission> permissions, boolean displayNoPermission) {
        if (permissions.contains(MusicPermission.NONE)) return Mono.just(true);
        if (permissions.contains(MusicPermission.ALONE))
            permissions.add(MusicPermission.DJ); //DJs can do all of Alone commands + more
        Mono<MusicPermission> permission = getPermission(member);
        Mono<Boolean> contains = permission.map(permissions::contains);
        if (!displayNoPermission) return contains;
        return contains
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        if (permissions.contains(MusicPermission.ALONE)) {
                            return Util.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or are a DJ.", channel);
                        } else if (permissions.contains(MusicPermission.DJ)) {
                            return Util.sendMessage(":x: Insufficient permission. You can do this command if you are a DJ.", channel);
                        }
                    }
                    return Mono.empty();
                })
                .then(contains);
    }

    public static Mono<Boolean> requirePermissions(Member member, TextChannel channel, Set<MusicPermission> permissions) {
        return requirePermissions(member, channel, permissions, true);
    }
}
