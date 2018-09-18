package me.sizableshrimp.discordbot.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.sizableshrimp.discordbot.Bot;
import me.sizableshrimp.discordbot.EventListener;
import me.sizableshrimp.discordbot.party.PartyEvents.Card;
import me.sizableshrimp.discordbot.party.PartyEvents.Player;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;

public class UnoGame {
	private Player turn;
	final long person1;
	private List<Card> hand1;
	final long person2;
	private List<Card> hand2;
	private List<Card> deck;
	private List<Card> discard;
	private boolean skipping = false;
	private Card.Color wildColor = null;
	private boolean isWild = false;

	public UnoGame(long person1, List<Card> hand1, long person2, List<Card> hand2, List<Card> deck, List<Card> discard) {
		this.turn = null;
		this.person1 = person1;
		this.hand1 = hand1;
		this.person2 = person2;
		this.hand2 = hand2;
		this.deck = deck;
		this.discard = discard;
	}

	/**
	 * Gets the player of the current turn
	 */
	public Player getTurn() {
		return turn;
	}

	/**
	 * Gets the IUser of the specified player
	 */
	public IUser getUser(Player player) {
		switch (player) {
		case FIRST:
			return Bot.client.getUserByID(person1);
		case SECOND:
			return Bot.client.getUserByID(person2);
		default:
			return null;
		}
	}
	
	/**
	 * Gets the IUser of the OPPOSITE player that was specified
	 */
	public IUser getOtherUser(Player player) {
		switch (player) {
		case FIRST:
			return Bot.client.getUserByID(person2);
		case SECOND:
			return Bot.client.getUserByID(person1);
		default:
			return null;
		}
	}

	/**
	 * Gets the player of the IUser, or null if not any of the players
	 */
	public Player getPlayer(IUser user) {
		if (user.getLongID() == person1) return Player.FIRST;
		if (user.getLongID() == person2) return Player.SECOND;
		return null;
	}
	
	/**
	 * Gets the opposite player of the one specified
	 */
	public Player getOtherPlayer(Player player) {
		switch (player) {
		case FIRST:
			return Player.SECOND;
		case SECOND:
			return Player.FIRST;
		default:
			return null;
		}
	}

	/**
	 * Gets the hand of the specified player
	 */
	public List<Card> getHand(Player player) {
		switch (player) {
		case FIRST:
			return hand1;
		case SECOND:
			return hand2;
		default:
			return null;
		}
	}

	/**
	 * Gets the deck
	 */
	public List<Card> getDeck() {
		return deck;
	}

	/**
	 * Gets a certain amount of cards from the top of the deck
	 */
	public List<Card> getTopCard(int amount) {
		if (deck.size() == 0) reshuffle();
		List<Card> cards = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			cards.add(deck.get(i));
		}
		return cards;
	}

	/**
	 * Removes a certain amount of cards from the top of the deck
	 */
	public void removeTopCard(int amount) {
		if (deck.size() == 0) reshuffle();
		for (int i = 0; i < amount; i++) {
			deck.remove(0);
		}
	}

	/**
	 * Gets the top card in the discard pile
	 */
	public Card getTopDiscard() {
		if (discard.size() == 0) return null;
		return discard.get(discard.size()-1);
	}

	/**
	 * Draws a certain amount of cards from the deck and puts it into the specified player's hand
	 */
	public List<Card> draw(Player player, int amount) {
		List<Card> drawn = getTopCard(amount);
		getHand(player).addAll(drawn);
		removeTopCard(amount);
		return drawn;
	}

	/**
	 * Goes to next turn
	 */
	public void nextTurn() {
		if (skipping) {
			turn = turn.next().next();
			setSkipping(false);
			return;
		}
		turn = turn.next();
	}
	
	public boolean hasFinished() {
		return hand1.size() == 0 || hand2.size() == 0;
	}
	
	public Player winner() {
		if (hand1.size() == 0) return Player.FIRST;
		if (hand2.size() == 0) return Player.SECOND;
		return null;
	}
	
	public void setSkipping(boolean skipping) {
		this.skipping = skipping;
	}

	/***
	 * Plays card by putting it on top of discard
	 */
	public void playCard(Card card, Player player) {
			if (!getHand(player).contains(card)) return;
			getHand(player).remove(card);
			discard(card);
	}

	/**
	 * Plays the draw four wild card and sets the wild color and boolean
	 */
	public void playDrawFourWild(Card.Color color, Player player) {
		draw(getOtherPlayer(player), 4);
		playCard(Card.DRAW_FOUR_WILD, player);
		wildColor = color;
		isWild = true;
		skipping = true;
	}
	
	/**
	 * Plays the wild card and sets the wild color and boolean
	 */
	public void playWild(Card.Color color, Player player) {
		playCard(Card.WILD, player);
		wildColor = color;
		isWild = true;
		skipping = true;
	}
	
	/**
	 * Returns the color of the wild card on the top of discard pile, or null if none
	 */
	public Card.Color getWildColor() {
		return wildColor;
	}
	
	/**
	 * Returns if the top of the discard is a type of wild card
	 */
	public boolean isWild() {
		return isWild;
	}
	
	/**
	 * Adds a card to the discard
	 */
	public void discard(Card card) {
		discard.add(card);
		if (isWild()) {
			isWild = false;
			wildColor = null;
		}
	}

	/**
	 * Reshuffles the discard back into the draw pile.
	 */
	public void reshuffle() {
		List<Card> temp = new ArrayList<>(discard);
		Collections.shuffle(temp);
		deck.addAll(temp);
		discard = new ArrayList<>();
	}

	/**
	 * Starts the UNO game.
	 */
	public boolean start(IChannel original) {
		EmbedBuilder embed1 = PartyEvents.defaultUnoEmbed();
		EmbedBuilder embed2 = PartyEvents.defaultUnoEmbed();
		List<String> names = new ArrayList<>();
		IUser person1 = Bot.client.getUserByID(this.person1);
		IUser person2 = Bot.client.getUserByID(this.person2);
		names.add(person1.getName());
		names.add(person2.getName());
		Collections.shuffle(names);
		Player firstTurn = names.get(0).equals(person1.getName()) ? Player.FIRST : Player.SECOND;
		if (firstTurn == Player.FIRST) {
			embed1.withDesc("The game has started! **You** go first");
			embed2.withDesc("The game has started! **"+names.get(0)+"** goes first");
		} else {
			embed1.withDesc("The game has started! **"+names.get(0)+"** goes first");
			embed2.withDesc("The game has started! **You** go first");
		}
		embed1.appendField("Discard", PartyEvents.getUnoEmoji(getTopDiscard()).toString(), true);
		embed2.appendField("Discard", PartyEvents.getUnoEmoji(getTopDiscard()).toString(), true);
		embed1.appendField("Your Hand", PartyEvents.displayHand(hand1), false);
		embed2.appendField("Your Hand", PartyEvents.displayHand(hand2), false);
		IMessage message;
		try {
			message = person1.getOrCreatePMChannel().sendMessage(embed1.build());
		} catch (DiscordException e) {
			e.printStackTrace();
			PartyEvents.end(this);
			EventListener.sendMessage("**Game canceled.** "+person1.mention()+" Please enable your private messages to play the game.", original);
			return false;
		}
		if (firstTurn == Player.FIRST) PartyEvents.calculateOptions(Player.FIRST, message, this);
		try {
			message = person2.getOrCreatePMChannel().sendMessage(embed2.build());
		} catch (DiscordException e) {
			e.printStackTrace();
			PartyEvents.end(this);
			EventListener.sendMessage("**Game canceled.** "+person2.mention()+" Please enable your private messages to play the game.", original);
			return false;
		}
		if (firstTurn == Player.SECOND) PartyEvents.calculateOptions(Player.SECOND, message, this);
		return true;
	}
}
