package me.sizableshrimp.discordbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.ajbrown.namemachine.Gender;
import org.ajbrown.namemachine.NameGenerator;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NameCommand extends Command {
    @Override
    public String getUsage() {
        return "newname (male|female)";
    }

    @Override
    public Set<String> getNames() {
        return Stream.of("newname").collect(Collectors.toSet());
    }

    @Override
    protected Mono<Void> run(MessageCreateEvent event, String[] args) {
        if (args.length != 1) {
            return incorrectUsage(event).flatMap(m -> deleteLater(7, m, event.getMessage()));
        }
        Gender gender;
        switch (args[0].toLowerCase()) {
            case "male":
                gender = Gender.MALE;
                break;
            case "female":
                gender = Gender.FEMALE;
                break;
            default:
                return incorrectUsage(event).flatMap(m -> deleteLater(7, m, event.getMessage()));
        }
        String name = new NameGenerator().generateName(gender).getFirstName();
        return event.getMessage().getAuthorAsMember()
                .flatMap(member -> member.edit(edit -> edit.setNickname(name)))
                .then(event.getMessage().getChannel().flatMap(c -> sendMessage("✅ Your name has been changed to `" + name + "`.", c)))
                .onErrorResume(throwable -> event.getMessage().getChannel().flatMap(c -> sendMessage("❌ I do not have permission to change your nickname. (This command does not work for admins.)", c)))
                .flatMap(message -> deleteLater(7, message, event.getMessage()));
    }
}
