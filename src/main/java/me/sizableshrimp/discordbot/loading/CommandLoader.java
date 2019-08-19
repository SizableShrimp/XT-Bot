package me.sizableshrimp.discordbot.loading;

import me.sizableshrimp.discordbot.Bot;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Auto-detects and loads commands that implement a generic type T. Can be used to dynamically load up all commands
 * without explicitly instantiating them. Injects needed dependencies into the instances individually.
 *
 * @param <T> The superclass whose subclasses should be loaded.
 */
public class CommandLoader<T> {
    private Class<T> token;

    /**
     * Creates a {@link CommandLoader} that takes in the surrounding dependencies and a token that represents the
     * classes to load.
     *
     * @param token A reflection {@link Class} that represents the superclass to load all subclasses from.
     */
    public CommandLoader(Class<T> token) {
        this.token = token;
    }

    /**
     * Dynamically instantiates all classes that inherit from type T,
     * ignoring classes that are annotated with {@link IgnoreCommand}.
     *
     * @return An unmodifiable set of instantiated objects derived from the superclass {@code T}.
     */
    public Set<T> loadClasses() {
        String mainPackage = Bot.class.getPackageName();
        Reflections reflections = new Reflections(mainPackage);

        Set<Class<? extends T>> cmds = reflections.getSubTypesOf(token).stream()
                .filter(clazz -> !clazz.isAnnotationPresent(IgnoreCommand.class) && clazz.getPackageName().startsWith(mainPackage))
                .collect(Collectors.toSet());

        Set<T> instances = new HashSet<>();

        for (var cmd : cmds) {
            try {
                if (Modifier.isAbstract(cmd.getModifiers())) {
                    continue;
                }
                T instance = cmd.getDeclaredConstructor().newInstance();
                instances.add(instance);
            } catch (Exception e) {
                LoggerFactory.getLogger(CommandLoader.class).error("Could not load {}", cmd.getName(), e);
            }
        }

        return Collections.unmodifiableSet(instances);
    }
}