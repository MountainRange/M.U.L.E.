package io.github.mountainrange.mule;

import io.github.mountainrange.mule.enums.*;
import io.github.mountainrange.mule.gameplay.Player;

/**
 * Simple config class
 */
public class Config {

	private static Config instance = null;

	public static final String DEFAULT_PACK = "plain";
	public static final int SELECTOR_SPEED = 1;

	public Difficulty difficulty;
	public GameType gameType;
	public MapSize mapSize;
	public MapType mapType;
	public Player[] playerList;
	public int currentPlayer;
	public int numOfPlayers;
	public int maxPlayers;
	public Race race;
	public boolean fadeEnabled;
	public boolean selectEnabled;

	protected Config() {
		difficulty = Difficulty.MESA;
		gameType = GameType.HOTSEAT;
		mapSize = MapSize.ALPS;
		mapType = MapType.CLASSIC;
		currentPlayer = 0;
		numOfPlayers = 2;
		maxPlayers = 4;
		race = Race.FOLD;
		fadeEnabled = false;
		selectEnabled = false;
	}

	public static Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}
}
