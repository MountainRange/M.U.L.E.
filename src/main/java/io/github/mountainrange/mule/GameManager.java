package io.github.mountainrange.mule;

import io.github.mountainrange.mule.enums.GameType;
import io.github.mountainrange.mule.enums.MessageType;
import io.github.mountainrange.mule.enums.MuleType;
import io.github.mountainrange.mule.enums.ResourceType;
import io.github.mountainrange.mule.gameplay.*;
import io.github.mountainrange.mule.gameplay.javafx.VisualGrid;
import io.github.mountainrange.mule.managers.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Holds key information about the state of the game, and drives turns.
 */
public class GameManager implements Serializable {

	private List<Player> playerList;
	private List<Player> buyers;
	private List<Player> turnOrder;

	private transient Config config;
	private transient SceneLoader sceneLoader;
	private Shop shop;
	private transient WorldMap<? extends Tile> map;

	private transient KeyBindManager keyManager;
	private transient MouseHandler mouseHandler;
	private transient RandomEventManager randManager;
	private transient Timeline selectorTimeline;
	private transient Timeline timerTimeline;
	private transient Timeline messageTimeline;

	private transient Label resourceLabel;
	private transient Label turnLabel;

	private boolean freeLand;
	private boolean gambleFlag;
	private boolean inAuction;

	private int currentPlayerNum;
	private int foodRequired;
	private int passCounter;
	private int phaseCount;
	private int roundCount;
	private int timeLeft;

	private void writeObject(java.io.ObjectOutputStream stream)
			throws IOException {
		stream.writeObject(playerList);
		stream.writeObject(buyers);
		stream.writeObject(turnOrder);
		stream.writeObject(shop);
		map.getGrid().backup();
		stream.writeObject(map);
		stream.writeBoolean(freeLand);
		stream.writeBoolean(gambleFlag);
		stream.writeBoolean(inAuction);
		stream.writeInt(currentPlayerNum);
		stream.writeInt(foodRequired);
		stream.writeInt(passCounter);
		stream.writeInt(phaseCount);
		stream.writeInt(roundCount);
		stream.writeInt(timeLeft);
	}

	private void readObject(java.io.ObjectInputStream stream)
			throws IOException, ClassNotFoundException {
		playerList = (ArrayList<Player>) stream.readObject();
		buyers = (ArrayList<Player>) stream.readObject();
		turnOrder = (ArrayList<Player>) stream.readObject();
		shop = (Shop) stream.readObject();
		map = (WorldMap) stream.readObject();
		map.getGrid().restore();
		freeLand = stream.readBoolean();
		gambleFlag = stream.readBoolean();
		inAuction = stream.readBoolean();
		currentPlayerNum = stream.readInt();
		foodRequired = stream.readInt();
		passCounter = stream.readInt();
		phaseCount = stream.readInt();
		roundCount = stream.readInt();
		timeLeft = stream.readInt();
	}

	@SuppressWarnings("deprecated")
	public void initialize(Label turnLabel, Label resourceLabel, SceneLoader sceneLoader, Pane mapPane) {
		map.refreshTiles();

		((VisualGrid) map.getGrid()).setUpperPane(mapPane);
		this.resourceLabel = resourceLabel;
		this.sceneLoader = sceneLoader;
		this.turnLabel = turnLabel;

		config = Config.getInstance();

		keyManager = new KeyBindManager();
		mouseHandler = new MouseHandler();
		randManager = new RandomEventManager();

		selectorTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.SELECTOR_SPEED),
						this::selectorAction
				)
		);
		selectorTimeline.setCycleCount(Timeline.INDEFINITE);

		timerTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.SELECTOR_SPEED),
						this::turnTimerAction
				)
		);
		timerTimeline.setCycleCount(Timeline.INDEFINITE);

		messageTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.MESSAGE_DURATION),
						this::messageAction
				)
		);
		messageTimeline.setCycleCount(1);

		if (phaseCount == 0) {
			map.select(0, 0);
			if (config.gameType == GameType.HOTSEAT) {
				if (config.selectEnabled) {
					runSelector();
				}
			} else if (config.gameType == GameType.SIMULTANEOUS) {
				turnLabel.setText("Land grab Phase");
				runSelector();
			}
		} else {
			map.select(4, 2);
			setLabels();
			timerTimeline.play();
		}
	}

	public GameManager(WorldMap map, Label turnLabel, Label resourceLabel, SceneLoader sceneLoader) {
		this.map = map;
		this.resourceLabel = resourceLabel;
		this.sceneLoader = sceneLoader;
		this.turnLabel = turnLabel;

		config = Config.getInstance();
		playerList = new ArrayList<>(Arrays.asList(config.playerList).subList(0, config.numOfPlayers));
		buyers = new ArrayList<>();
		turnOrder = new ArrayList<>(playerList);
		shop = new Shop(config.difficulty);

		currentPlayerNum = 0;
		passCounter = 0;
		phaseCount = 0;
		roundCount = -2;
		timeLeft = 0;

		freeLand = true;

		keyManager = new KeyBindManager();
		mouseHandler = new MouseHandler();
		randManager = new RandomEventManager();

		selectorTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.SELECTOR_SPEED),
						this::selectorAction
				)
		);
		selectorTimeline.setCycleCount(Timeline.INDEFINITE);

		timerTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.SELECTOR_SPEED),
						this::turnTimerAction
				)
		);
		timerTimeline.setCycleCount(Timeline.INDEFINITE);

		messageTimeline = new Timeline(
				new KeyFrame(
						Duration.seconds(Config.MESSAGE_DURATION),
						this::messageAction
				)
		);
		messageTimeline.setCycleCount(1);

		nextRound();
	}

	/**
	 * Passes player turn during land-grab phase for HOTSEAT only.
	 */
	public void pass() {
		if (!freeLand) {
			passCounter++;
			currentPlayerNum = (currentPlayerNum + 1) % config.numOfPlayers;
			setLabels();
			if (config.numOfPlayers == passCounter) {
				nextRound();
			}
		}

	}

	/**
	 * Increments the turn.
	 */
	public void incrementTurn() {
		passCounter++;
		currentPlayerNum = (currentPlayerNum + 1) % config.numOfPlayers;
		setLabels();
		if (config.numOfPlayers == passCounter) {
			nextRound();
		}
		if (currentPlayerNum == 0) {
			passCounter = 0;
		}
	}

	/**
	 * Attempt to buy the currently selected tile for the given player, or place a MULE.
	 * @param player player to buy tile for
	 */
	public void buyTile(Player player) {
		if (player.hasMule()) {
			map.placeMule(player);
		} else {
			if (map.getOwner() == null) {
				int cost = (int) (300 + (roundCount * Math.random() * 100));
				if (cost > player.getMoney()) {
					System.out.println("Not enough money");
					return;
				}
				if (phaseCount == 0) {
					if (config.gameType == GameType.HOTSEAT) {
						cost = (int) (300 + (Math.random() * 100));
						map.sellTile(player);
						currentPlayerNum = (currentPlayerNum + 1) % config.numOfPlayers;
						setLabels();
						if (!freeLand) {
							player.setMoney(player.getMoney() - cost);
							if (currentPlayerNum == 0) {
								passCounter = 0;
							}
							if (config.numOfPlayers == passCounter) {
								nextRound();
							}
						} else {
							if (currentPlayerNum == 0) {
								passCounter = 0;
								nextRound();
							}
						}
					} else if (config.gameType == GameType.SIMULTANEOUS) {
						if (map.countLandOwnedBy(player) < roundCount + 3) {
							buyers.add(player);
						}
					}
				} else if (phaseCount == 1) {
					player.setMoney(player.getMoney() - cost);
					map.sellTile(player);
					setLabels();
				}
			}
		}
	}

	private void delayedBuy() {
		if (buyers.size() > 0) {
			Player player = buyers.get(0);
			if (!freeLand) {
				int cost = (int)(300 + (roundCount * Math.random() * 100));
				if (cost > player.getMoney()) {
					System.out.println("Not enough money");
					return;
				}
				map.sellTile(player);
				setLabels();
				player.setMoney(player.getMoney() - cost);
			} else  {
				map.sellTile(player);
				setLabels();
			}
		}
		buyers.clear();
	}

	/**
	 * Test if all players have bought land this round.
	 * @return whether all players bought land
	 */
	private boolean allBoughtLand() {
		for (Player p : playerList) {
			if (map.countLandOwnedBy(p) < roundCount) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Update the HUD with the current player's statistics.
	 */
	public void setLabels() {
		Player currentPlayer = turnOrder.get(currentPlayerNum);
		turnLabel.setText(turnOrder.get(currentPlayerNum).getName() + "'s Turn " + timeLeft);
		String s = String.format("%1$s's Money: %2$s F: %3$s E: %4$s S: %5$s C: %6$s",
				currentPlayer.getName(), currentPlayer.getMoney(),
				currentPlayer.stockOf(ResourceType.FOOD),
				currentPlayer.stockOf(ResourceType.ENERGY),
				currentPlayer.stockOf(ResourceType.SMITHORE),
				currentPlayer.stockOf(ResourceType.CRYSTITE));
		resourceLabel.setText(s);
	}

	/**
	 * Advance the game to the next round, and perform any associated actions. Specifically, calculate and apply
	 * production, increment the round counter, and reorder players by increasing score.
	 */
	private void nextRound() {
		// Reorder players based on score
		calculateTurnOrder();
		// Increment roundCount to the next round
		roundCount++;

		passCounter = 0;
		setLabels();
		if (roundCount == 0) {
			freeLand = false;
			foodRequired = 3;
		} else if (roundCount == 1) {
			phaseCount = 1;
		} else if (roundCount == 5) {
			foodRequired++;
		} else if (roundCount == 9) {
			foodRequired++;
		}
		if (phaseCount == 0) {
			landGrabPhase();
		} else if (phaseCount == 1) {
			normalPhase();
		}
	}

	/**
	 * End the current player's turn, begin the next player's turn, and perform any other associated actions.
	 */
	public void endTurn() {
		if (!sceneLoader.getCurrentScene().equals(MULE.PLAY_SCENE)) {
			sceneLoader.setScene(MULE.PLAY_SCENE);
		}
		// Stop the ticking turn timer
		timerTimeline.stop();
		// Clear the player's MULE, if the player is carrying one
		turnOrder.get(currentPlayerNum).setMule(null);
		currentPlayerNum = (currentPlayerNum + 1) % config.numOfPlayers;
		// Display message announcing the start of the new player's turn
		//showCustomText(MessageType.TURN.getPlayerTurnMessage(currentPlayerNum));
		// Get Random Event that occurred
		randManager.runRandomEvent(new GameState(this, map), currentPlayerNum == 0);


		turnTimer();
		setLabels();

		if (currentPlayerNum == 0) {
			nextRound();
		}
	}

	/**
	 * Calls WorldMap to display a normal message.
	 * @param msg message to display
	 */
	public void showText(MessageType msg) {
		map.showText(msg);
	}

	/**
	 * Calls WorldMap to display a custom message.
	 * @param msg message to display
	 */
	public void showCustomText(String msg) {
		map.showCustomText(msg);
	}

	/**
	 * Displays a temporary message.
	 * @param msg message to display
	 */
	public void showTempText(MessageType msg) {
		showText(msg);
		messageTimeline.playFromStart();
	}

	/**
	 * Clear the display after a messageTimeline expires.
	 * @param e event to react to
	 */
	private void messageAction(ActionEvent e) {
		showText(MessageType.NONE);
	}

	/**
	 * Apply a food-related event to the current player.
	 * @param msg message to apply
	 */
	public void changeFood(MessageType msg) {
		if (msg == MessageType.LOSEFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, -1);
		} else if (msg == MessageType.LOSESOMEFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, -(player.stockOf(ResourceType.FOOD) / 4));
		} else if (msg == MessageType.LOSEHALFFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, -(player.stockOf(ResourceType.FOOD) / 2));
		} else if (msg == MessageType.GAINFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, 1);
		} else if (msg == MessageType.GAINSOMEFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, (player.stockOf(ResourceType.FOOD) / 2));
		} else if (msg == MessageType.GAINDOUBLEFOOD) {
			Player player = playerList.get(currentPlayerNum);
			player.changeStockOf(ResourceType.FOOD, player.stockOf(ResourceType.FOOD));
		}
	}

	private void landGrabPhase() {
		showTempText(MessageType.LANDGRAB);
		map.select(0, 0);
		if (config.gameType == GameType.HOTSEAT) {
			if (config.selectEnabled) {
				runSelector();
			}
		} else if (config.gameType == GameType.SIMULTANEOUS) {
			turnLabel.setText("Land grab Phase");
			runSelector();
		}
	}

	private void normalPhase() {
		if (roundCount == 1) {
			randManager.runRandomEvent(new GameState(this, map), currentPlayerNum == 0);
		}
		if (roundCount > 1) {
			calculateProduction();
		}
		map.select(4, 2);
		setLabels();
		turnTimer();
	}

	private void calculateProduction() {
		// Calculate production for all players on the given round
		Map<Player, EnumMap<ResourceType, ProductionResult>> production = ProductionManager.calculateProduction(map,
				playerList, roundCount);

		// Apply production results
		for (Map.Entry<Player, EnumMap<ResourceType, ProductionResult>> playerEntry : production.entrySet()) {
			Player player = playerEntry.getKey();
			EnumMap<ResourceType, ProductionResult> resourceResults = playerEntry.getValue();

			for (Map.Entry<ResourceType, ProductionResult> resourceEntry : resourceResults.entrySet()) {
				ResourceType resource = resourceEntry.getKey();
				ProductionResult productionResult = resourceEntry.getValue();

				// For each resource, change the player's stock by the appropriate amount
				player.changeStockOf(resource, productionResult.delta());
			}
		}
	}

	private void enterAuction(List<Player> buyers) {
		setInAuction(true);
		config.buyers = new ArrayList<>(buyers);
		sceneLoader.setScene(MULE.AUCTION_SCENE);
	}

	public void setInAuction(boolean inAuction) {
		this.inAuction = inAuction;
	}

	/**
	 * In simultaneous mode, set up a {@code Timeline} to move the currently selected tile across the map, allowing
	 * players to buy the currently selected tile.
	 */
	private void runSelector() {
		selectorTimeline.play();
	}

	/**
	 * Perform all the actions associated with moving the map cursor in simultaneous mode.
	 * @param event event to react to
	 */
	private void selectorAction(ActionEvent event) {
		if (!inAuction && phaseCount == 0) {
			delayedBuy();
			map.selectRightWrap();
		}
		if (allBoughtLand() && freeLand) {
			selectorTimeline.stop();
			nextRound();
		}
		if (config.numOfPlayers == passCounter) {
			selectorTimeline.stop();
			nextRound();
		}
	}

	/**
	 * Set {@code timeLeft} based on the amount of food the current player has.
	 */
	private void resetTimer() {
		if (turnOrder.get(currentPlayerNum).stockOf(ResourceType.FOOD) < foodRequired) {
			timeLeft = 30;
		} else if (turnOrder.get(currentPlayerNum).stockOf(ResourceType.FOOD) <= 0) {
			timeLeft = 5;
		} else {
			timeLeft = 50;
		}
	}

	/**
	 * Set up a {@code Timeline} that keeps track of the amount of time the current player has left, and ends their
	 * turn when they are out of time.
	 */
	private void turnTimer() {
		resetTimer();
		timerTimeline.play();
	}

	/**
	 * Perform all the actions associated with a single tick of the turn timer.
	 * @param event event to react to
	 */
	private void turnTimerAction(ActionEvent event) {
		timeLeft--;
		setLabels();
		if (gambleFlag) {
			gambleFlag = false;
			turnOrder.get(currentPlayerNum).changeMoney(Shop.gamblingProfit(roundCount, timeLeft));
			endTurn();
		}
		if (timeLeft <= 0) {
			endTurn();
		}
	}

	/**
	 * Compute the score of the given player. The score of a player is computed as follows:
	 * {@code money + (plot * 500 + price of outfitting) + mules in store * 35 + (resource * price of resource)}.
	 * @return map of players to their scores
	 */
	public Map<Player, Integer> scoreGame() {
		Map<Player, Integer> scores = new HashMap<>();

		// Compute score from total number of mules in store
		int muleScore = shop.muleStock() * 35;
		for (Player player : playerList) {
			// Add score from money and mules
			int score = player.getMoney() + muleScore;

			// Add score from player resources
			for (ResourceType resource : ResourceType.values()) {
				score += player.stockOf(resource) * shop.priceOf(resource);
			}

			// Add score from tiles owned and MULEs installed
			score += map.countLandOwnedBy(player) * 500;
			for (Tile tile : map.landOwnedBy(player)) {
				if (tile.getMule() != MuleType.EMPTY) {
					score += Shop.outfitPriceOf(tile.getMule());
				}
			}

			scores.put(player, score);
		}

		return scores;
	}

	/**
	 * Sort players in ascending order by score (so players with the lowest score go first).
	 *
	 * {@code turnOrder} holds the sorted version of {@code playerList}.
	 */
	public void calculateTurnOrder() {
		Map<Player, Integer> scores = scoreGame();
		turnOrder.sort((p1, p2) -> scores.get(p1) - scores.get(p2));
	}

	/**
	 * Sets a global variable indicating the player is going to gamble. This is read by the timer in {@code turnTimer},
	 * and causes the turn to end and gives the player the money earned gambling.
	 */
	public void setGambleFlag() {
		gambleFlag = true;
	}

	public void handleKey(KeyEvent e) {
		this.keyManager.handleKey(new GameView(config.gameType, sceneLoader.getCurrentScene(), phaseCount),
				e.getCode(), new GameState(this, map));
	}

	public List<Player> getPlayerList() {
		return playerList;
	}

	public int getCurrentPlayerNum() {
		return currentPlayerNum;
	}

	public Player getCurrentPlayer() {
		return turnOrder.get(currentPlayerNum);
	}

	/**
	 * Returns the shop.
	 * @return {@code Shop} associated with this {@code GameManager}
	 */
	public Shop getShop() {
		return shop;
	}

	/**
	 * Attempt to buy the currently selected tile for the current player.
	 */
	public void buyTile() {
		buyTile(turnOrder.get(currentPlayerNum));
	}

	public void handleMouse(MouseEvent e) {
		this.mouseHandler.handleEvent(e);
	}

	/**
	 * A small class for handling mouse events
	 */
	private class MouseHandler {
		public void handleEvent(MouseEvent e) {
			if (e.getEventType().getName().equals("MOUSE_PRESSED")) {
				if (map.isInside(new Point2D(e.getX(), e.getY()), (map.getColumns() / 2), (map.getRows() / 2))) {
					if (phaseCount == 1) {
						sceneLoader.setScene(MULE.TOWN_SCENE);
					}
				}
			}
		}
	}

	public void endTimers() {
		selectorTimeline.stop();
		timerTimeline.stop();
		messageTimeline.stop();
	}
}
