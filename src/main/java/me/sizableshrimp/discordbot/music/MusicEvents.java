package me.sizableshrimp.discordbot.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceDisconnectedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.PermissionUtils;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MusicEvents {
    private Music music;
    private List<Long> lockedGuilds = new ArrayList<>();

    public MusicEvents() {
        music = new Music();
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor() == null /*webhook*/ || event.getAuthor().isBot() || event.getChannel().isPrivate())
            return;
        String prefix = Bot.getPrefix(event.getGuild());
        String message = event.getMessage().getContent();
        GuildMusicManager manager = music.getGuildMusicManager(event.getGuild());
        String[] args = Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length);
        if (message.toLowerCase().startsWith(prefix + "music")) {
            EventListener.sendMessage("I can play music! My music commands are:```[] - required   () - optional  * - admins or people alone with bot only  ** - admins only\n" +
                    prefix + "play [song] - Plays the song that you request.\n" +
                    "* " + prefix + "volume (new volume) or " + prefix + "vol (new volume) - Changes the volume or tells the current volume.\n" +
                    "* " + prefix + "pause or " + prefix + "p - Pauses/resumes the song.\n" +
                    prefix + "queue or " + prefix + "q - Shows what is currently playing and what is queued up to go next.\n" +
                    "* " + prefix + "clear - Clears all the queued music.\n" +
                    prefix + "nowplaying or " + prefix + "np - Shows what is currently playing.\n" +
                    "* " + prefix + "remove [number in queue to remove] - Removes the song in the queue at the number given.\n" +
                    "* " + prefix + "rewind - Rewinds the song by 10 seconds.\n" +
                    "* " + prefix + "fastforward or " + prefix + "ff - Fast forwards the song by 10 seconds.\n" +
                    "* " + prefix + "goto [time in song] - Starts playing from a certain point in the song.\n" +
                    prefix + "skip - Requests to skip the song. If enough people have voted to skip, the next song will be played.\n" +
                    "* " + prefix + "forceskip - Forcefully skips to the next song.\n" +
                    "* " + prefix + "disconnect or " + prefix + "leave - Disconnects from the voice channel and stops playing music.\n" +
                    "* " + prefix + "loop - Puts the song currently playing on/off repeat.\n" +
                    "** " + prefix + "lock - Locks the bot to admins only.```" +
                    "If you are the only person in a voice channel with XT Bot, you may use normal commands and commands that have a * at the beginning. If you are an admin (have the **Manage Channels** permission), you may use all commands on this list.", event.getChannel());
        } else if (message.toLowerCase().startsWith(prefix + "play")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (args.length == 0) {
                EventListener.incorrectUsage("play [song]", event.getChannel());
                return;
            }
            IVoiceChannel voiceChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();
            if (voiceChannel != null && !PermissionUtils.hasPermissions(voiceChannel, Bot.client.getOurUser(), Permissions.VOICE_CONNECT)) {
                EventListener.sendMessage(":x: I do not have permission to join `" + voiceChannel.getName() + "`. I need the **Connect** permission in the voice channel's settings.", event.getChannel());
                return;
            }
            String query = String.join(" ", args);
            boolean isValid = true;
            try {
                new URL(query).toURI();
            } catch (MalformedURLException | URISyntaxException exception) {
                isValid = false;
            }
            if (!isValid) query = "ytsearch:" + query;
            if (event.getGuild().getConnectedVoiceChannel() != null && voiceChannel == event.getGuild().getConnectedVoiceChannel()) {
                music.loadAndPlay(event.getChannel(), query, isValid);
            } else if (event.getGuild().getConnectedVoiceChannel() == null && voiceChannel == null) {
                EventListener.sendMessage("Join a voice channel if you want me to play a song!", event.getChannel());
            } else if (event.getGuild().getConnectedVoiceChannel() != null && voiceChannel != event.getGuild().getConnectedVoiceChannel()) {
                EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to add a song to the queue.", event.getChannel());
            } else if (event.getGuild().getConnectedVoiceChannel() == null && voiceChannel != null) {
                voiceChannel.join();
                music.loadAndPlay(event.getChannel(), query, isValid);
            }
        } else if (message.toLowerCase().startsWith(prefix + "volume") || message.startsWith(prefix + "vol")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to change/look at the volume.", event.getChannel());
                    return;
                }
                if (args.length == 1) {
                    try {
                        Integer.valueOf(args[0]);
                    } catch (NumberFormatException exception) {
                        EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
                        return;
                    }
                    int volume = Integer.valueOf(args[0]);
                    if (volume < 0 || volume > 100) {
                        EventListener.sendMessage("Please enter a number between 0 and 100.", event.getChannel());
                        return;
                    }
                    int oldVolume = manager.player.getVolume();
                    manager.player.setVolume(volume);
                    EventListener.sendMessage("Volume set to **" + volume + "%** from **" + oldVolume + "**%", event.getChannel());
                } else if (args.length == 0) {
                    EventListener.sendMessage("Volume is at **" + manager.player.getVolume() + "%**", event.getChannel());
                } else {
                    EventListener.incorrectUsage("volume [new volume] or " + prefix + "volume", event.getChannel());
                }
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "pause") || message.toLowerCase().startsWith(prefix + "p")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to pause/resume the music.", event.getChannel());
                    return;
                }
                if (manager.player.getPlayingTrack() != null) {
                    if (manager.player.isPaused()) {
                        manager.player.setPaused(false);
                        EventListener.sendMessage(":arrow_forward: Music resumed.", event.getChannel());
                    } else {
                        manager.player.setPaused(true);
                        EventListener.sendMessage(":pause_button: Music paused.", event.getChannel());
                    }
                } else {
                    EventListener.sendMessage("There is no music to pause or resume.", event.getChannel());
                }
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "clear")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to clear the queue.", event.getChannel());
                    return;
                }
                manager.scheduler.queue.clear();
                EventListener.sendMessage("Queue cleared.", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "remove")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to remove a song from the queue.", event.getChannel());
                    return;
                }
                if (manager.scheduler.queue.isEmpty()) {
                    EventListener.sendMessage("There is nothing in the queue to remove.", event.getChannel());
                    return;
                }
                if (args.length == 1) {
                    try {
                        Integer.valueOf(args[0]);
                    } catch (NumberFormatException exception) {
                        EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
                        return;
                    }
                    int queueNum = Integer.valueOf(args[0]);
                    if (manager.scheduler.queue.size() < queueNum || queueNum <= 0) {
                        EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
                        return;
                    }
                    AudioTrack selected = null;
                    int num = 0;
                    for (AudioTrack track : manager.scheduler.queue) {
                        num++;
                        if (queueNum == num) selected = track;
                    }
                    if (selected == null) {
                        EventListener.sendMessage("Please enter a number from the queue.", event.getChannel());
                        return;
                    }
                    manager.scheduler.queue.remove(selected);
                    EventListener.sendMessage("Removed `" + selected.getInfo().title + "` from the queue.", event.getChannel());
                } else {
                    EventListener.incorrectUsage("remove [number from queue]", event.getChannel());
                }
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "goto")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to change the time of the current song.", event.getChannel());
                    return;
                }
                if (manager.player.getPlayingTrack() == null) {
                    EventListener.sendMessage("There is nothing to change the time of.", event.getChannel());
                    return;
                }
                if (args.length != 1 || !message.contains(":")) {
                    EventListener.sendMessage("Incorrect usage. Please use: ```" + prefix + "goto [time in song]```Example: `" + prefix + "goto 5:35`", event.getChannel());
                    return;
                }
                String time = args[0];
                int colons = 0;
                for (char c : time.toCharArray()) if (c == ':') colons++;
                if (colons == 0 || colons > 2) {
                    EventListener.sendMessage("Incorrect usage. Please use: ```" + prefix + "goto [time in song]```Example: `" + prefix + "goto 5:35`", event.getChannel());
                    return;
                }
                List<Integer> numbers = new ArrayList<>();
                for (String s : time.split(":")) {
                    try {
                        Integer.valueOf(s);
                    } catch (NumberFormatException e) {
                        EventListener.sendMessage("Incorrect usage. Please use: ```" + prefix + "goto [time in song]```Example: `" + prefix + "goto 5:35`", event.getChannel());
                        return;
                    }
                    numbers.add(Integer.valueOf(s));
                }
                long millis = colons == 1 ? TimeUnit.MINUTES.toMillis(numbers.get(0)) + TimeUnit.SECONDS.toMillis(numbers.get(1)) : TimeUnit.HOURS.toMillis(numbers.get(0)) + TimeUnit.MINUTES.toMillis(numbers.get(1)) + TimeUnit.SECONDS.toMillis(numbers.get(2));
                AudioTrack track = manager.player.getPlayingTrack();
                if (millis < 0L || millis > track.getDuration()) {
                    EventListener.sendMessage("Specified time is out of range. Please choose a different time.", event.getChannel());
                    return;
                }
                track.setPosition(millis);
                EventListener.sendMessage("Now playing at `" + time + "`", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "skip")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (event.getGuild().getConnectedVoiceChannel() == null) {
                EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", event.getChannel());
                return;
            }
            if (connectedToDifferent(event)) {
                EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to request to skip the current song.", event.getChannel());
                return;
            }
            if (manager.player.getPlayingTrack() == null) {
                EventListener.sendMessage("There is nothing to skip.", event.getChannel());
                return;
            }
            if (manager.usersSkipping.contains(event.getAuthor().getLongID())) {
                EventListener.sendMessage(":x: You have already requested to skip this song.", event.getChannel());
                return;
            }
            if (manager.usersSkipping.size() + 1 == manager.neededToSkip) {
                manager.scheduler.nextTrack();
                EventListener.sendMessage("Skipped song.", event.getChannel());
                return;
            }
            manager.usersSkipping.add(event.getAuthor().getLongID());
            EventListener.sendMessage(Integer.toString(manager.usersSkipping.size()) + "/" + Integer.toString(manager.neededToSkip) + " people have requested to skip this song.", event.getChannel());
        } else if (message.toLowerCase().startsWith(prefix + "forceskip")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to forceskip the current song.", event.getChannel());
                    return;
                }
                if (manager.player.getPlayingTrack() == null) {
                    EventListener.sendMessage(":x: There is nothing to skip.", event.getChannel());
                    return;
                }
                manager.scheduler.nextTrack();
                EventListener.sendMessage("Skipped song.", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "queue") || message.startsWith(prefix + "q")) {
            AudioTrack playing = manager.player.getPlayingTrack();
            BlockingQueue<AudioTrack> queue = manager.scheduler.queue;
            EmbedBuilder embed = new EmbedBuilder();
            embed.withAuthorName("Queue");
            embed.withColor(new Color(242, 242, 242));
            if (playing == null) {
                embed.appendDesc("There is currently nothing playing.");
                EventListener.sendEmbed(embed, event.getChannel());
                return;
            }
            embed.appendDesc("__**Now Playing:**__\n" + "[" + playing.getInfo().title + "](" + playing.getInfo().uri + ") | `" + getLength(playing.getInfo().length) + "`");
            embed.appendDesc("\n\n__**Up Next:**__\n");
            if (queue.isEmpty()) {
                embed.appendDesc("\nThere is currently nothing up next.");
                EventListener.sendEmbed(embed, event.getChannel());
                return;
            }
            int number = 1;
            for (AudioTrack track : queue)
                embed.appendDesc("\n" + Integer.toString(number++) + ". " + "[" + track.getInfo().title + "](" + track.getInfo().uri + ") | `" + getLength(track.getInfo().length) + "`");
            EventListener.sendEmbed(embed, event.getChannel());
        } else if (message.toLowerCase().startsWith(prefix + "nowplaying") || message.startsWith(prefix + "np")) {
            AudioTrack playing = manager.player.getPlayingTrack();
            EmbedBuilder embed = new EmbedBuilder();
            embed.withAuthorName("Now Playing");
            embed.withColor(new Color(242, 242, 242));
            if (playing == null) {
                embed.appendDesc("There is currently nothing playing.");
                EventListener.sendEmbed(embed, event.getChannel());
                return;
            }
            AudioTrackInfo info = playing.getInfo();
            embed.appendDesc("[" + info.title + "](" + info.uri + ")");
            embed.appendDesc("\n" + getLength(playing.getPosition()) + " / " + getLength(info.length));
            EventListener.sendEmbed(embed, event.getChannel());
        } else if (message.toLowerCase().startsWith(prefix + "rewind")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to rewind.", event.getChannel());
                    return;
                }
                if (manager.player.getPlayingTrack() == null) {
                    EventListener.sendMessage("There is nothing to rewind.", event.getChannel());
                    return;
                }
                manager.player.getPlayingTrack().setPosition(manager.player.getPlayingTrack().getPosition() - TimeUnit.SECONDS.toMillis(10));
                EventListener.sendMessage(":rewind: Skipped 10 seconds backwards.", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "fastforward") || message.toLowerCase().startsWith(prefix + "ff")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to fast forward.", event.getChannel());
                    return;
                }
                if (manager.player.getPlayingTrack() == null) {
                    EventListener.sendMessage("There is nothing to fast forward.", event.getChannel());
                    return;
                }
                manager.player.getPlayingTrack().setPosition(manager.player.getPlayingTrack().getPosition() + TimeUnit.SECONDS.toMillis(10));
                EventListener.sendMessage(":fast_forward: Skipped 10 seconds forwards.", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "disconnect") || message.startsWith(prefix + "leave")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to make me disconnect.", event.getChannel());
                    return;
                }
                if (event.getGuild().getConnectedVoiceChannel() == null) {
                    EventListener.sendMessage("I am not connected to a voice channel.", event.getChannel());
                    return;
                }
                if (event.getGuild().getConnectedVoiceChannel() != event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel()) {
                    EventListener.sendMessage(":x: You must be connected to `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to make me leave.", event.getChannel());
                    return;
                }
                event.getGuild().getConnectedVoiceChannel().leave();
                EventListener.sendMessage("Left `" + event.getGuild().getConnectedVoiceChannel().getName() + "`", event.getChannel());
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "loop")) {
            if (locked(event.getAuthor(), event.getChannel())) return;
            if (hasManageChannels(event.getAuthor(), event.getGuild()) || isAlone(event)) {
                if (notConnected(event.getChannel())) return;
                if (connectedToDifferent(event)) {
                    EventListener.sendMessage("Join `" + (PermissionUtils.hasPermissions(event.getGuild().getConnectedVoiceChannel(), event.getAuthor(), Permissions.READ_MESSAGES/*READ_MESSAGES includes being able to view a voice channel*/) ? event.getGuild().getConnectedVoiceChannel().getName() : "HIDDEN CHANNEL") + "` to loop the song.", event.getChannel());
                    return;
                }
                if (manager.scheduler.isRepeating()) {
                    manager.scheduler.setRepeating(false);
                    EventListener.sendMessage("Loop disabled.", event.getChannel());
                } else {
                    manager.scheduler.setRepeating(true);
                    EventListener.sendMessage(":repeat: Loop enabled.", event.getChannel());
                }
            } else {
                notDJ(event.getChannel());
            }
        } else if (message.toLowerCase().startsWith(prefix + "lock")) {
            if (hasManageChannels(event.getAuthor(), event.getGuild())) {
                if (notConnected(event.getChannel())) return;
                if (lockedGuilds.contains(event.getGuild().getLongID())) {
                    lockedGuilds.remove(event.getGuild().getLongID());
                    EventListener.sendMessage(":unlock: Music unlocked.", event.getChannel());
                } else {
                    lockedGuilds.add(event.getGuild().getLongID());
                    EventListener.sendMessage(":lock: Music locked.", event.getChannel());
                }
            } else {
                EventListener.sendMessage(":x: Insufficient permission. You can do this command if you have the **Manage Channels** permission.", event.getChannel());
            }
        }
    }

    @EventSubscriber
    public void onBotVoiceDisconnect(VoiceDisconnectedEvent event) {
        GuildMusicManager manager = music.getGuildMusicManager(event.getGuild());
        manager.scheduler.queue.clear();
        manager.player.startTrack(null, false);
        manager.player.setVolume(music.DEFAULT_VOLUME);
        manager.player.setPaused(false);
        manager.scheduler.setRepeating(false);
        lockedGuilds.remove(event.getGuild().getLongID());
    }

    @EventSubscriber
    public void onUserVoiceLeave(UserVoiceChannelLeaveEvent event) {
        IVoiceChannel voiceChannel = event.getVoiceChannel();
        if (event.getGuild().getConnectedVoiceChannel() == voiceChannel) {
            if (voiceChannel.getConnectedUsers().size() == 1) {
                voiceChannel.leave();
                return;
            }
            GuildMusicManager manager = music.getGuildMusicManager(event.getGuild());
            manager.neededToSkip--;
            manager.usersSkipping.remove(event.getUser().getLongID());
            if (manager.usersSkipping.size() == manager.neededToSkip) manager.scheduler.nextTrack();
        }
    }

    private boolean isAlone(MessageReceivedEvent event) {
        IVoiceState state = event.getAuthor().getVoiceStateForGuild(event.getGuild());
        if (state == null) return false;
        if (state.getChannel() != null && state.getChannel().getConnectedUsers().size() == 2) {
            List<IUser> users = state.getChannel().getConnectedUsers();
            return users.contains(Bot.client.getOurUser()) && users.contains(event.getAuthor());
        }
        return false;
    }

    private boolean hasManageChannels(IUser user, IGuild guild) {
        return PermissionUtils.hasPermissions(guild, user, Permissions.MANAGE_CHANNELS);
    }

    private String getLength(Long length) {
        long hours = 0L;
        if (TimeUnit.MILLISECONDS.toHours(length) > 0) hours = TimeUnit.MILLISECONDS.toHours(length);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(length) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
        String convertedSeconds = seconds < 10L ? "0" + Long.toString(seconds) : Long.toString(seconds);
        String convertedMinutes = minutes < 10L ? "0" + Long.toString(minutes) : Long.toString(minutes);
        return hours != 0L ? Long.toString(hours) + ":" + convertedMinutes + ":" + convertedSeconds : Long.toString(minutes) + ":" + convertedSeconds;
    }

    private boolean connectedToDifferent(MessageReceivedEvent event) {
        return (event.getGuild().getConnectedVoiceChannel() == null || event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel() != event.getGuild().getConnectedVoiceChannel());
    }

    private boolean locked(IUser user, IChannel channel) {
        if (lockedGuilds.contains(channel.getGuild().getLongID()) && !hasManageChannels(user, channel.getGuild())) {
            EventListener.sendMessage(":lock: Music is currently locked for normal members. Please try again later.", channel);
            return true;
        }
        return false;
    }

    private boolean notConnected(IChannel channel) {
        if (channel.getGuild().getConnectedVoiceChannel() == null) {
            EventListener.sendMessage(":x: I am not connected to a voice channel. Make me play music to use this command.", channel);
            return true;
        }
        return false;
    }

    private void notDJ(IChannel channel) {
        EventListener.sendMessage(":x: Insufficient permission. You can do this command if you are alone with the bot or have the **Manage Channels** permission.", channel);
    }
}
