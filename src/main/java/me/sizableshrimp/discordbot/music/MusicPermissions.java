package me.sizableshrimp.discordbot.music;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import me.sizableshrimp.discordbot.Util;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public enum MusicPermissions {
    DJ, ALONE, DEFAULT;

    static Mono<MusicPermissions> getPermission(Member member) {
        return member.getBasePermissions()
                .filterWhen(ps -> hasDJRole(member).map(hasDjRole -> hasDjRole || ps.contains(Permission.MANAGE_CHANNELS)))
                .map(ps -> MusicPermissions.DJ)
                .switchIfEmpty(Util.isMemberAlone(member)
                        .map(b -> b ? MusicPermissions.ALONE : MusicPermissions.DEFAULT)
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

    public static Mono<Boolean> requirePermissions(Member member, TextChannel channel, MusicPermissions... permissions) {
        List<MusicPermissions> perms = Arrays.asList(permissions);
        if (perms.contains(MusicPermissions.DEFAULT)) return Mono.just(true);
        Mono<Boolean> contains = getPermission(member).map(perms::contains);
        return contains
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        if (perms.contains(MusicPermissions.ALONE)) {
                            return Util.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or are a DJ.", channel);
                        } else if (perms.contains(MusicPermissions.DJ)) {
                            return Util.sendMessage(":x: Insufficient permission. You can do this command if you are a DJ.", channel);
                        }
                    }
                    return Mono.empty();
                })
                .then(contains);
    }
}
