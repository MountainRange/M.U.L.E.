package io.github.mountainrange.mule.gameplay;

import io.github.mountainrange.mule.enums.MuleType;
import io.github.mountainrange.mule.enums.ResourceType;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * A class to represent the map and facilitates interactions
 * between the actual data store and the rest of the program
 */
public class WorldMap implements Iterable<Tile> {

	private Grid map;
	private Map<Player, Set<Tile>> ownedTiles;

	public WorldMap(Grid g) {
		this.map = g;

		ownedTiles = new HashMap<>();
	}

	public WorldMap(Grid g, List<Player> playerList) {
		this.map = g;

		ownedTiles = new HashMap<>();
		for (Player p : playerList) {
			ownedTiles.put(p, new HashSet<>());
		}
	}

	// ----------------------------Logical methods-------------------------------

	/**
	 * Count the number of tiles owned by the given player, or zero if the given player owns no tiles.
	 * @param player player to count owned tiles
	 * @return number of tiles owned by the given player
	 */
	public int countLandOwnedBy(Player player) {
		if (!ownedTiles.containsKey(player)) {
			return 0;
		}
		return ownedTiles.get(player).size();
	}

	/**
	 * Return an unmodifiable set with the tiles owned by the given player, or an empty set if the given player owns no
	 * tiles.
	 * @param player player to get owned tiles
	 * @return unmodifiable set with tiles owned by the given player
	 */
	public Set<Tile> landOwnedBy(Player player) {
		return Collections.unmodifiableSet(ownedTiles.getOrDefault(player, new HashSet<>()));
	}

	/**
	 * Sell the given tile to the given player. Selling will fail if the tile is already owned.
	 * @param player player to sell the tile to
	 * @param tile tile to sell
	 * @return whether the tile was actually sold
	 */
	public boolean sellTile(Player player, Tile tile) {
		if (tile.hasOwner()) {
			return false;
		}

		tile.setOwner(player);
		if (!ownedTiles.containsKey(player)) {
			// Adding a new player
			ownedTiles.put(player, new HashSet<>());
		}
		ownedTiles.get(player).add(tile);
		return true;
	}

	/**
	 * Set the player's mule at a given tile
	 * @param player player to get mule from
	 * @param tile tile to set mule
	 * @return whether the mule was placed
	 */
	public boolean setMule(Player player, Tile tile) {
		if (tile.getOwner() != player || tile.getMule() != MuleType.EMPTY) {
			return false;
		}

		tile.setMuleDraw(player.getCurrentMuleType());
		return true;
	}

	@Override
	public Iterator<Tile> iterator() {
		return map.iterator();
	}

	/**
	 * Gets the number of columns in this map
	 * @return cols the number of columns in this map
	 */
	public int getColumns() {
		return map.getCols();
	}

	/**
	 * Gets the number of rows in this map
	 * @return rows the number of rows in this map
	 */
	public int getRows() {
		return map.getRows();
	}

	// ----------------------------Graphical methods-----------------------------

	/**
	 * Get the tile the cursor is currently on.
	 * @return the tile the cursor is on
	 */
	public Tile cursorTile() {
		int x = map.getCursorX();
		int y = map.getCursorY();
		return map.get(x, y);
	}

	/**
	 * Sell the tile at the cursor location to the given player. Selling will fail if the tile is already owned.
	 * @param player player to sell the tile to
	 * @return whether the tile was actually sold
	 */
	public boolean sellTile(Player player) {
		Tile tile = cursorTile();
		return sellTile(player, tile);
	}

	/**
	 * Set the mule at the current location
	 * @param player player to get mule from
	 * @return whether the mule was set
	 */
	public boolean setMule(Player player) {
		Tile tile = cursorTile();
		return setMule(player, tile);
	}

	public boolean select(int x, int y) {
		if (x < 0 || y < 0 || x >= map.getCols() || y >= map.getRows()) {
			System.out.println("Cannot select off map");
			return false;
		}
		map.select(x, y);
		return true;
	}

	public boolean selectRel(int xmove, int ymove) {
		int newposx = map.getCursorX() + xmove;
		int newposy = map.getCursorY() + ymove;
		if (newposx < 0 || newposy < 0 || newposx >= map.getCols()
				|| newposy >= map.getRows()) {
			System.out.println("Cannot select off map");
			return false;
		}
		map.select(newposx, newposy);
		return true;
	}

	public boolean selectUp() {
		return selectRel(0, -1);
	}

	public boolean selectDown() {
		return selectRel(0, 1);
	}

	public boolean selectRight() {
		return selectRel(1, 0);
	}

	public boolean selectLeftWrap() {
		int newposx = map.getCursorX() - 1;
		int newposy = map.getCursorY() - 1;
		if (newposx < 0 && newposy < 0) {
			return select(map.getCols() - 1, newposy);
		}
		return selectRel(1, 0);
	}

	public boolean selectRightWrap() {
		int newposx = map.getCursorX() + 1;
		int newposy = map.getCursorY() + 1;
		if (newposx >= map.getCols()) {
			return select(0, newposy % map.getRows());
		}
		return selectRel(1, 0);
	}

	public boolean selectLeft() {
		return selectRel(-1, 0);
	}

	public Player getOwner() {
		return cursorTile().getOwner();
	}

	public boolean isInside(Point2D toCheck, int column, int row) {
		return map.isInside(toCheck, column, row);
	}

}
