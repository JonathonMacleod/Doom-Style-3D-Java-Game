package utils;

import java.util.ArrayList;

import graphics.Entity;
import graphics.Sprite;
import mobs.Ghost;
import mobs.Player;
import mobs.Wolf;

public class Level {

	public final Sprite tileMap;
	public final Sprite entityMap;

	public final Player player;
	
	public final ArrayList<Entity> entities = new ArrayList<Entity>();
	
	public Level(String levelName) {
		tileMap = new Sprite("assets/levels/" + levelName + "/tile_map.png");
		entityMap = new Sprite("assets/levels/" + levelName + "/entity_map.png");

		addEntities();
		player = new Player(this, null, 0, 0, 0);
		resetPlayer();
	}
	
	public Tile getLevelTile(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Tile.getFloorTile(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	
	public Wall getLevelWall(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Wall.getWall(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}

	public void resetPlayer() {
		for(int i = 0; i < entityMap.pixels.length; i++) {
			final int currentColour = entityMap.pixels[i];
			
			// Try and find the pixel that identifies the camera starting position
			if(currentColour == 0xfffff600) {
				final int pixelX = i % entityMap.width;
				final int pixelZ = i / entityMap.height;
				
				player.camera.x = pixelX * 32 + 16;
				player.camera.y = 0;
				player.camera.z = pixelZ * 32 - 16;
				player.camera.angle = (float) Math.PI;
				
				break;
			}
		}
	}
	
	private void addEntities() {
		for(int y = 0; y < entityMap.height; y++) {
			for(int x = 0; x < entityMap.width; x++) {
				int colour = entityMap.pixels[x + y * entityMap.width];
				int tileX = x * 32 + 16;
				int tileZ = y * 32 - 16;
				
				if(colour == 0xffff0000) entities.add(new Wolf(this, tileX, 11, tileZ));
				if(colour == 0xff0026ff) entities.add(new Ghost(this, tileX, 0, tileZ));
			}
		}
	}
}
