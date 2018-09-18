package me.sizableshrimp.discordbot.party;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.EventListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IReaction;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

public class PartyEvents {
	final static Color UNO_NEUTRAL = new Color(255, 255, 0);
	final static Color UNO_BAD = new Color(255, 0, 0);
	final static Color UNO_GOOD = new Color(0, 255, 0);
	private final static long UNO_GUILD = 471178200580489236L;
	private final static long UNO_GUILD2 = 490759923240796160L;
	static HashMap<Long, UnoGame> unos = new HashMap<>();
	static HashMap<Integer, Long> waitingUno = new HashMap<>();
	static List<Long> waitingReactions = new ArrayList<>();
	static List<Long> drawFourReactions = new ArrayList<>();
	static List<Long> wildReactions = new ArrayList<>();
	final static List<IEmoji> WILD_COLORS = Arrays.asList(Bot.client.getGuildByID(UNO_GUILD).getEmojiByName("red_square"), 
			Bot.client.getGuildByID(UNO_GUILD).getEmojiByName("green_square"), 
			Bot.client.getGuildByID(UNO_GUILD).getEmojiByName("blue_square"), 
			Bot.client.getGuildByID(UNO_GUILD).getEmojiByName("yellow_square"));
	final static List<Card> DEFAULT_DECK = Arrays.asList(Card.WILD, Card.WILD, Card.WILD, Card.WILD, Card.DRAW_FOUR_WILD, Card.DRAW_FOUR_WILD, Card.DRAW_FOUR_WILD, Card.DRAW_FOUR_WILD, Card.YELLOW_0, Card.YELLOW_1, Card.YELLOW_2, 
			Card.YELLOW_3, Card.YELLOW_4, Card.YELLOW_5, Card.YELLOW_6, Card.YELLOW_7, Card.
			YELLOW_8, Card.YELLOW_9, Card.YELLOW_DRAW_TWO, Card.YELLOW_SKIP, Card.
			YELLOW_REVERSE, Card.YELLOW_1, Card.YELLOW_2, Card.
			YELLOW_3, Card.YELLOW_4, Card.YELLOW_5, Card.YELLOW_6, Card.YELLOW_7, Card.
			YELLOW_8, Card.YELLOW_9, Card.YELLOW_DRAW_TWO, Card.YELLOW_SKIP, Card.
			YELLOW_REVERSE, Card.RED_0, Card.RED_1, Card.RED_2, Card.RED_3, Card.RED_4, Card.RED_5, Card.RED_6, Card.RED_7, Card.
			RED_8, Card.RED_9, Card.RED_DRAW_TWO, Card.RED_SKIP, Card.RED_REVERSE, Card.RED_1, Card.RED_2, Card.RED_3, Card.RED_4, Card.RED_5, Card.RED_6, Card.RED_7, Card.
			RED_8, Card.RED_9, Card.RED_DRAW_TWO, Card.RED_SKIP, Card.RED_REVERSE, Card.BLUE_0, Card.BLUE_1, Card.BLUE_2, Card.
			BLUE_3, Card.BLUE_4, Card.BLUE_5, Card.BLUE_6, Card.BLUE_7, Card.
			BLUE_8, Card.BLUE_9, Card.BLUE_DRAW_TWO, Card.BLUE_SKIP, Card.
			BLUE_REVERSE, Card.BLUE_1, Card.BLUE_2, Card.
			BLUE_3, Card.BLUE_4, Card.BLUE_5, Card.BLUE_6, Card.BLUE_7, Card.
			BLUE_8, Card.BLUE_9, Card.BLUE_DRAW_TWO, Card.BLUE_SKIP, Card.
			BLUE_REVERSE, Card.GREEN_0, Card.GREEN_1, Card.GREEN_2, Card.GREEN_3, Card.GREEN_4, Card.GREEN_5, Card.GREEN_6, Card.GREEN_7, Card.
			GREEN_8, Card.GREEN_9, Card.GREEN_DRAW_TWO, Card.GREEN_SKIP, Card.GREEN_REVERSE, Card.GREEN_1, Card.GREEN_2, Card.GREEN_3, Card.GREEN_4, Card.GREEN_5, Card.GREEN_6, Card.GREEN_7, Card.
			GREEN_8, Card.GREEN_9, Card.GREEN_DRAW_TWO, Card.GREEN_SKIP, Card.GREEN_REVERSE);

	enum Card {
		UNO_BACK, WILD, DRAW_FOUR_WILD, YELLOW_1, YELLOW_2, 
		YELLOW_3, YELLOW_4, YELLOW_5, YELLOW_6, YELLOW_7, 
		YELLOW_8, YELLOW_9, YELLOW_0, YELLOW_DRAW_TWO, YELLOW_SKIP, 
		YELLOW_REVERSE, RED_1, RED_2, RED_3, RED_4, RED_5, RED_6, RED_7, 
		RED_8, RED_9, RED_0, RED_DRAW_TWO, RED_SKIP, RED_REVERSE, BLUE_1, BLUE_2, 
		BLUE_3, BLUE_4, BLUE_5, BLUE_6, BLUE_7, 
		BLUE_8, BLUE_9, BLUE_0, BLUE_DRAW_TWO, BLUE_SKIP, 
		BLUE_REVERSE, GREEN_1, GREEN_2, GREEN_3, GREEN_4, GREEN_5, GREEN_6, GREEN_7, 
		GREEN_8, GREEN_9, GREEN_0, GREEN_DRAW_TWO, GREEN_SKIP, GREEN_REVERSE;
		enum Color {
			YELLOW, RED, BLUE, GREEN;
		}
		enum Type {
			NUMBER, DRAW_TWO, SKIP, REVERSE, WILD, DRAW_FOUR_WILD;
		}
		public Color getColor() {
			if (this.toString().contains("YELLOW")) {
				return Color.YELLOW;
			} else if (this.toString().contains("RED")) {
				return Color.RED;
			} else if (this.toString().contains("BLUE")) {
				return Color.BLUE;
			} else if (this.toString().contains("GREEN")) {
				return Color.GREEN;
			} else {
				return null;
			}
		}
		public Type getType() {
			if (Pattern.compile("[0-9]").matcher(this.toString()).find()) {
				return Type.NUMBER;
			} else if (this.toString().contains("DRAW_TWO")) {
				return Type.DRAW_TWO;
			} else if (this.toString().contains("SKIP")) {
				return Type.SKIP;
			} else if (this.toString().contains("REVERSE")) {
				return Type.REVERSE;
			} else if (this.toString().contains("DRAW_FOUR_WILD")) {
				return Type.DRAW_FOUR_WILD;
			} else if (this.toString().equals("WILD")) {
				return Type.WILD;
			} else {
				return null;
			}
		}
	}

	enum Player {
		FIRST, SECOND;
		public Player next() {
			return values()[(ordinal()+1) % values().length];
		}
		public Player previous() {
			return values()[(ordinal()-1) % values().length];
		}
	}

	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {//TODO add PMS ONLY help command and add to message of every embed (with new embed send method)
		RequestBuffer.request(() -> {
			if (event.getAuthor().isBot()) return;
			String message = event.getMessage().getContent();
			String[] args = Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length);
			IUser user = event.getAuthor();
			//			if (event.getChannel().isPrivate() && unos.containsKey(user)) {
			//				//TODO add stuff here
			//			}
			if (message.toLowerCase().startsWith(Bot.getPrefix(event.getGuild())+"uno")) {
				if (args.length == 1 && args[0].equals("play")) {
					if (waitingUno.containsValue(user.getLongID())) {
						EventListener.sendMessage(":x: You are already waiting to join a game.", event.getChannel());
						return;
					}
					if (unos.containsKey(user.getLongID())) {
						EventListener.sendMessage(":x: You are already in a game.", event.getChannel());
						return;
					}
					final int id = waitingUno.size()+1;
					waitingUno.put(id, user.getLongID());
					EventListener.sendMessage("Created a lobby. Someone else can join this UNO game by typing `"+Bot.getPrefix(event.getGuild())+"uno join "+String.valueOf(id)+"`\n**Please make sure your private messaging is enabled or you will not be able to play**", event.getChannel());
				} else if (args.length == 2 && args[0].equals("join")) {
					if (waitingUno.containsValue(user.getLongID())) {
						EventListener.sendMessage(":x: You are already waiting to join a game.", event.getChannel());
						return;
					}
					try {
						Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						EventListener.sendMessage("Please enter a number.", event.getChannel());
						return;
					}
					int num = Integer.valueOf(args[1]);
					if (num > waitingUno.size() || num <= 0 || waitingUno.get(num) == null) {
						EventListener.sendMessage("Please enter a valid lobby id.", event.getChannel());
						return;
					}
					IUser person1 = Bot.client.getUserByID(waitingUno.get(num));
					waitingUno.remove(num);
					generateNewUno(person1, user, event.getChannel());
				}
			}
		});
	}

	@EventSubscriber
	public void onReact(ReactionAddEvent event) {
		if (!event.getChannel().isPrivate()) return;
		IUser user = event.getUser();
		if (unos.containsKey(user.getLongID()) && waitingReactions.contains(event.getMessageID()) && event.getReaction().getUserReacted(Bot.client.getOurUser()) && event.getReaction().getUserReacted(user)) {
			UnoGame game = unos.get(user.getLongID());
			if (wildReactions.contains(event.getMessageID())) {
				Card.Color color = getWildColor(event.getReaction());
				if (color == null) return;
				sendWild(game, user, color);
				waitingReactions.remove(event.getMessageID());
				wildReactions.remove(event.getMessageID());
				return;
			} else if (drawFourReactions.contains(event.getMessageID())) {
				Card.Color color = getWildColor(event.getReaction());
				if (color == null) return;
				drawFourWild(game, user, color);
				waitingReactions.remove(event.getMessageID());
				drawFourReactions.remove(event.getMessageID());
				return;
			}
			doAction(event, game, game.getPlayer(user));
			waitingReactions.remove(event.getMessageID());
			if (game.hasFinished()) displayWinner(game);
			doNextTurn(game);
		}
	}


	static Card.Color getWildColor(IReaction reaction) {
		if (reaction.getEmoji().getName().toLowerCase().equals("red_square")) {
			return Card.Color.RED;
		} else if (reaction.getEmoji().getName().toLowerCase().equals("green_square")) {
			return Card.Color.GREEN;
		} else if (reaction.getEmoji().getName().toLowerCase().equals("blue_square")) {
			return Card.Color.BLUE;
		} else if (reaction.getEmoji().getName().toLowerCase().equals("yellow_square")) {
			return Card.Color.YELLOW;
		} else {
			return null;
		}
	}

	static void doNextTurn(UnoGame game) {
		game.nextTurn();
		Player player = game.getTurn();
		EmbedBuilder embed = defaultUnoEmbed();
		embed.appendDesc("**Your turn**");
		embed.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		embed.appendField("Your Hand", displayHand(game.getHand(player)), false);
		if (game.isWild()) embed.appendDesc("\nThe color of the wild card is **"+game.getWildColor().toString()+"**");
		calculateOptions(player, game.getUser(player).getOrCreatePMChannel().sendMessage(embed.build()), game);
	}

	static IMessage doAction(ReactionAddEvent event, UnoGame game, Player player) {
		Card selected = getCard(event.getReaction().getEmoji());
		if (selected == Card.UNO_BACK) {
			return draw(game, event.getUser(), player);
		} else if (selected.getType() != Card.Type.NUMBER) {
			return special(selected, game, player);
		} else {
			return playCard(selected, game, event.getUser());
		}
	}

	static IMessage special(Card card, UnoGame game, Player player) {
		Card.Type type = card.getType();
		if (type == Card.Type.DRAW_FOUR_WILD) {
			EmbedBuilder embed = defaultUnoEmbed();
			embed.appendDesc("You selected a draw four wild card! What color do you choose?");
			embed.withImage(getUnoEmoji(Card.DRAW_FOUR_WILD).getImageUrl());
			IMessage message = game.getUser(player).getOrCreatePMChannel().sendMessage(embed.build());
			waitingReactions.add(message.getLongID());
			drawFourReactions.add(message.getLongID());
			react(message, WILD_COLORS);
			return message;
		} else if (type == Card.Type.WILD) {
			EmbedBuilder embed = defaultUnoEmbed();
			embed.appendDesc("You selected a wild card! What color do you choose?");
			embed.withImage(getUnoEmoji(Card.WILD).getImageUrl());
			IMessage message = game.getUser(player).getOrCreatePMChannel().sendMessage(embed.build());
			waitingReactions.add(message.getLongID());
			wildReactions.add(message.getLongID());
			react(message, WILD_COLORS);
			return message;
		} else if (type == Card.Type.DRAW_TWO) {
			game.draw(game.getOtherPlayer(player), 2);
			IUser other = game.getOtherUser(player);
			EmbedBuilder receiver = defaultUnoEmbed();
			receiver.withColor(UNO_BAD);
			receiver.appendDesc("You were forced to take two cards because **"+other.getName()+"** played a draw two card.");
			receiver.withImage(getUnoEmoji(card).getImageUrl());
			receiver.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			receiver.appendField("Your Hand", displayHand(game.getHand(game.getPlayer(other))), false);
			other.getOrCreatePMChannel().sendMessage(receiver.build());
			EmbedBuilder sender = defaultUnoEmbed();
			sender.withColor(UNO_GOOD);
			sender.appendDesc("You made **"+game.getUser(player).getName()+"** take two cards!");
			receiver.withImage(getUnoEmoji(card).getImageUrl());
			sender.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			sender.appendField("Your Hand", displayHand(game.getHand(player)), false);
			game.setSkipping(true);
			return game.getUser(player).getOrCreatePMChannel().sendMessage(sender.build());
		} else if (type == Card.Type.REVERSE) {
			IUser other = game.getOtherUser(player);
			EmbedBuilder receiver = defaultUnoEmbed();
			receiver.withColor(UNO_BAD);
			receiver.appendDesc("Your turn was skipped because **"+other.getName()+"** used a reverse card (when in 2 players a reverse skips the other's turn)");
			receiver.withImage(getUnoEmoji(card).getImageUrl());
			receiver.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			receiver.appendField("Your Hand", displayHand(game.getHand(game.getPlayer(other))), false);
			other.getOrCreatePMChannel().sendMessage(receiver.build());
			EmbedBuilder sender = defaultUnoEmbed();
			sender.withColor(UNO_GOOD);
			sender.appendDesc("You made **"+game.getUser(player).getName()+"** skip a turn!");
			sender.withImage(getUnoEmoji(card).getImageUrl());
			sender.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			sender.appendField("Your Hand", displayHand(game.getHand(player)), false);
			game.setSkipping(true);
			return game.getUser(player).getOrCreatePMChannel().sendMessage(sender.build());
		} else if (type == Card.Type.SKIP) {
			IUser other = game.getOtherUser(player);
			EmbedBuilder receiver = defaultUnoEmbed();
			receiver.withColor(UNO_BAD);
			receiver.appendDesc("Your turn was skipped because **"+other.getName()+"** used a skip card");
			receiver.withImage(getUnoEmoji(card).getImageUrl());
			receiver.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			receiver.appendField("Your Hand", displayHand(game.getHand(game.getPlayer(other))), false);
			other.getOrCreatePMChannel().sendMessage(receiver.build());
			EmbedBuilder sender = defaultUnoEmbed();
			sender.withColor(UNO_GOOD);
			sender.appendDesc("You made **"+game.getUser(player).getName()+"** skip a turn!");
			sender.withImage(getUnoEmoji(card).getImageUrl());
			sender.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
			sender.appendField("Your Hand", displayHand(game.getHand(player)), false);
			game.setSkipping(true);
			return game.getUser(player).getOrCreatePMChannel().sendMessage(sender.build());
		}
		return null;
	}

	static IMessage playCard(Card card, UnoGame game, IUser user) {
		Player player = game.getPlayer(user);
		game.playCard(card, player);
		IEmoji emoji = getUnoEmoji(card);
		EmbedBuilder embed = defaultUnoEmbed();
		embed.appendDesc("You played a card!");
		embed.withImage(emoji.getImageUrl());
		embed.appendField("Discard", emoji.toString(), true);
		embed.appendField("Your Hand", displayHand(game.getHand(player)), false);
		return user.getOrCreatePMChannel().sendMessage(embed.build());
	}

	static IMessage drawFourWild(UnoGame game, IUser user, Card.Color color) {
		Player player = game.getPlayer(user);
		game.playDrawFourWild(color, player);
		IUser other = game.getOtherUser(player);
		EmbedBuilder receiver = defaultUnoEmbed();
		receiver.withColor(UNO_BAD);
		receiver.appendDesc("You were forced to take four cards because **"+other.getName()+"** played a draw four wild card. The color is now **"+color.toString()+"**");
		receiver.withImage(getUnoEmoji(Card.DRAW_FOUR_WILD).getImageUrl());
		receiver.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		receiver.appendField("Your Hand", displayHand(game.getHand(game.getPlayer(other))), false);
		other.getOrCreatePMChannel().sendMessage(receiver.build());
		EmbedBuilder sender = defaultUnoEmbed();
		sender.withColor(UNO_GOOD);
		sender.appendDesc("You made **"+game.getUser(player).getName()+"** take four cards! The color is now **"+color.toString()+"**");
		receiver.withImage(getUnoEmoji(Card.DRAW_FOUR_WILD).getImageUrl());
		sender.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		sender.appendField("Your Hand", displayHand(game.getHand(player)), false);
		return game.getUser(player).getOrCreatePMChannel().sendMessage(sender.build());
	}

	static IMessage sendWild(UnoGame game, IUser user, Card.Color color) {
		Player player = game.getPlayer(user);
		game.playWild(color, player);
		IUser other = game.getOtherUser(player);
		EmbedBuilder receiver = defaultUnoEmbed();
		receiver.appendDesc("**"+other.getName()+"** used a wild card! The color is now **"+color.toString()+"**");
		receiver.withImage(getUnoEmoji(Card.WILD).getImageUrl());
		receiver.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		receiver.appendField("Your Hand", displayHand(game.getHand(game.getPlayer(other))), false);
		other.getOrCreatePMChannel().sendMessage(receiver.build());
		EmbedBuilder sender = defaultUnoEmbed();
		sender.appendDesc("You used a wild card and made the color **"+color.toString()+"**");
		receiver.withImage(getUnoEmoji(Card.WILD).getImageUrl());
		sender.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		sender.appendField("Your Hand", displayHand(game.getHand(player)), false);
		return game.getUser(player).getOrCreatePMChannel().sendMessage(sender.build());
	}

	static IMessage draw(UnoGame game, IUser user, Player player) {
		Card card = game.draw(player, 1).get(0);
		EmbedBuilder embed = defaultUnoEmbed();
		embed.appendDescription("You drew a card");
		embed.withImage(getUnoEmoji(card).getImageUrl());
		embed.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		embed.appendField("Your Hand", displayHand(game.getHand(player)), false);
		return user.getOrCreatePMChannel().sendMessage(embed.build());
	}

	static Card getCard(ReactionEmoji reaction) {
		return Card.valueOf(reaction.getName().toUpperCase());
	}

	static void calculateOptions(Player player, IMessage message, UnoGame game) {
		List<IEmoji> viableActions = new ArrayList<>();
		Card current = game.getTopDiscard();
		if (game.isWild()) {
			for (Card inHand : game.getHand(player)) {
				if (inHand.getColor() == game.getWildColor() || inHand == Card.DRAW_FOUR_WILD || inHand == Card.WILD) viableActions.add(getUnoEmoji(inHand));
			}
		} else {
			for (Card inHand : game.getHand(player)) {
				if (inHand == Card.DRAW_FOUR_WILD || inHand == Card.WILD || inHand.getType() == current.getType() || inHand.getColor() == current.getColor()) viableActions.add(getUnoEmoji(inHand));
			}
		}
		viableActions.add(getUnoEmoji(Card.UNO_BACK));
		react(message, viableActions);
		waitingReactions.add(message.getLongID());
	}

	static void react(IMessage message, List<IEmoji> emojis) {
		for (IEmoji e : emojis) {
			message.addReaction(e);
		}
	}

	static IEmoji getUnoEmoji(Card card) {
		if (card == null) return null;
		IEmoji emoji1 = Bot.client.getGuildByID(UNO_GUILD).getEmojiByName(card.toString().toLowerCase());
		IEmoji emoji2 = Bot.client.getGuildByID(UNO_GUILD2).getEmojiByName(card.toString().toLowerCase());
		return emoji1 == null ? emoji2 : emoji1;
	}

	private void generateNewUno(IUser person1, IUser person2, IChannel channel) {
		List<Card> deck = new ArrayList<>(DEFAULT_DECK);
		List<Card> hand1 = new ArrayList<>();
		List<Card> hand2 = new ArrayList<>();
		List<Card> discard = new ArrayList<>();
		Collections.shuffle(deck);
		boolean firstPerson = true;
		for (int i = 0; i < 14; i++) {
			if (firstPerson) {
				hand1.add(deck.get(i));
				deck.remove(i);
			} else {
				hand2.add(deck.get(i));
				deck.remove(i);
			}
			firstPerson = !firstPerson;
		}
		discard.add(deck.get(0));
		deck.remove(0);
		UnoGame game = new UnoGame(person1.getLongID(), hand1, person2.getLongID(), hand2, deck, discard);
		unos.put(person1.getLongID(), game);
		unos.put(person2.getLongID(), game);
		if (game.start(channel)) EventListener.sendMessage("Started game. "+person1.mention()+" "+person2.mention()+" Please check your private messages to start playing.", channel);
	}

	static EmbedBuilder defaultUnoEmbed() {
		EmbedBuilder embed  = new EmbedBuilder();
		embed.withAuthorName("UNO");
		embed.withColor(UNO_NEUTRAL);
		embed.withAuthorIcon("upload.wikimedia.org/wikipedia/commons/thumb/f/f9/UNO_Logo.svg/1280px-UNO_Logo.svg.png");
		return embed;
	}

	static String displayHand(List<Card> hand) {
		StringBuilder result = new StringBuilder();
		for (Card c : hand) {
			if (result.length() > 0) result.append(" ");
			result.append(getUnoEmoji(c).toString());
		}
		if (result.length() == 0) return "None";
		return result.toString();
	}

	static void displayWinner(UnoGame game) {
		EmbedBuilder won = defaultUnoEmbed();
		EmbedBuilder lost = defaultUnoEmbed();
		Player winner = game.winner();
		Player loser = game.getOtherPlayer(winner);
		won.withColor(UNO_GOOD);
		won.appendDesc("You won!");
		won.withImage("upload.wikimedia.org/wikipedia/commons/thumb/f/f9/UNO_Logo.svg/1280px-UNO_Logo.svg.png");
		won.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		won.appendField("Your Hand", displayHand(game.getHand(winner)), false);
		won.appendField("Opponent's Hand", displayHand(game.getHand(loser)), false);
		lost.withColor(UNO_BAD);
		lost.appendDesc("You lost. Better luck next time!");
		lost.withImage("upload.wikimedia.org/wikipedia/commons/thumb/f/f9/UNO_Logo.svg/1280px-UNO_Logo.svg.png");
		lost.appendField("Discard", getUnoEmoji(game.getTopDiscard()).toString(), true);
		lost.appendField("Your Hand", displayHand(game.getHand(loser)), false);
		lost.appendField("Opponent's Hand", displayHand(game.getHand(winner)), false);
		game.getUser(winner).getOrCreatePMChannel().sendMessage("Thanks for playing!", won.build());
		game.getUser(loser).getOrCreatePMChannel().sendMessage("Thanks for playing!", lost.build());
		end(game);
	}

	static void end(UnoGame game) {
		unos.remove(game.person1);
		unos.remove(game.person2);
	}
}