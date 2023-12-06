package utils;

import graphics.Sprite;

public class Level {

	public final Sprite tileMap;
	public final Sprite entityMap;
	
	public Level(String levelName) {
		tileMap = new Sprite("assets/" + levelName + "/tile_map.png");
		entityMap = null; //new Sprite("assets/" + levelName + "/entity_map.png");
	}
	
	public Tile getLevelTile(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Tile.getTile(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	
	public Wall getLevelWall(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Wall.getWall(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	
}
