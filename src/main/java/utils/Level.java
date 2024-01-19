package utils;

import java.util.ArrayList;

import graphics.Camera;
import graphics.Entity;
import graphics.RenderPane3D;
import graphics.Sprite;
import mobs.Ghost;
import mobs.Mob;
import mobs.Player;
import mobs.Wolf;
import ui.InputHandler;

public class Level {

	public int tileSize = 16;
	
	public final Sprite tileMap;
	public final Sprite entityMap;

	public final Player player;
	public final ArrayList<Entity> entities = new ArrayList<Entity>();
	
	public Level(String levelName) {
		tileMap = new Sprite("assets/levels/" + levelName + "/tile_map.png");
		entityMap = new Sprite("assets/levels/" + levelName + "/entity_map.png");

		player = new Player(this, 0, 0, 0);
		resetEntities();
		resetPlayer();
	}

	public void update(InputHandler inputHandler, float delta) {
		// Update the player
		player.update(inputHandler, delta);
		
		// Update any mobs in the level
		for(Entity currentEntity : entities) {
			if(currentEntity instanceof Mob) ((Mob) currentEntity).update(inputHandler, delta);
		}
	}
	
	public void drawLevel(RenderPane3D renderPane) {
		drawWallsFloorAndCeiling(renderPane);
	}

	public void resetPlayer() {
		// Iterate through the entity map to find the player's original location
		for(int i = 0; i < entityMap.pixels.length; i++) {
			final int currentColour = entityMap.pixels[i];
			
			// Try and find the pixel that identifies the camera starting position
			if(currentColour == 0xfffff600) {
				final int pixelX = i % entityMap.width;
				final int pixelZ = i / entityMap.height;
				
				player.camera.x = pixelX * tileSize + 16;
				player.camera.y = 0;
				player.camera.z = pixelZ * tileSize - 16;
				player.camera.angle = 0 * (float) Math.PI;
				
				break;
			}
		}
	}
	public void resetEntities() {
		// Remove any existing entities
		entities.clear();
		
		// Iterate through the entity map and add all entities to their expected locations
		for(int y = 0; y < entityMap.height; y++) {
			for(int x = 0; x < entityMap.width; x++) {
				int colour = entityMap.pixels[x + y * entityMap.width];
				float tileX = x * tileSize + 16;
				float tileZ = y * tileSize - 16;
				
				if(colour == 0xffff0000) entities.add(new Wolf(this, tileX, 11, tileZ));
				if(colour == 0xff0026ff) entities.add(new Ghost(this, tileX, 0, tileZ));
			}
		}
	}
	
	public Tile getLevelFloorTile(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Tile.getFloorTile(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	public Tile getLevelCeilingTile(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Tile.getFloorTile(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	public Wall getLevelWall(int levelX, int levelZ) {
		if((levelX < 0) || (levelX >= tileMap.width)) return null;
		if((levelZ < 0) || (levelZ >= tileMap.height)) return null;
		return Wall.getWall(tileMap.pixels[levelX + levelZ * tileMap.width]);
	}
	
	private void drawWallsFloorAndCeiling(final RenderPane3D renderPane) {
		final float cameraTileX = player.camera.x / tileSize;
		final float cameraTileZ = player.camera.z / tileSize;
		
		final float horizontalRayAngleIncrement = (player.camera.fovRadians / renderPane.width);
		final float startingHorizontalRayAngle = -(player.camera.fovRadians / 2);
		
        for(int screenX = 0; screenX < renderPane.width; screenX++) {
        	// Calculate the angle of the ray being fired relative to the center of the screen (-FOV/2 to FOV/2)
        	float horizontalAngle = startingHorizontalRayAngle + horizontalRayAngleIncrement * screenX;
        	// Calculate the angle of the ray being fired (adjusted for the camera's angle offset)
        	float rayAngle = player.camera.angle + horizontalAngle;
        	
            // Draw the floor and ceiling for the current screen column
        	drawWallColumn(renderPane, screenX, cameraTileX, cameraTileZ, horizontalAngle, rayAngle);
        	drawFloorAndCeilingColumn(renderPane, screenX, cameraTileX, cameraTileZ, horizontalAngle, rayAngle);
        }
    }
	
	private void drawWallColumn(final RenderPane3D renderPane, final int screenX, final float cameraTileX, final float cameraTileZ, final float horizontalAngle, final float rayAngle) {
		// This constant controls how accurate the ray distance is by determining how much a ray moves before wall collision is checked
		final float rayPrecision = 0.01f;
		final int maximumRayIncrement = (int) (16 / rayPrecision);
		
		// Work out the current position of the ray being cast to find walls, and also how much it should travel in each direction based on it's angle
    	float currentRayTileX = cameraTileX;
    	float currentRayTileZ = cameraTileZ;
    	final float rayXDelta = (float) Math.sin(rayAngle) * rayPrecision;
    	final float rayZDelta = (float) Math.cos(rayAngle) * rayPrecision;
    	
    	// Search for a wall
    	Wall collidedWall = null;
    	float wallSpriteHorizontalPercentage = 0;
    	for(int i = 0; i < maximumRayIncrement; i++) {
    		// Move the ray based on it's angle, and if the ray is out of the map then stop trying to find walls
    		// Move one axis at a time to avoid clipping diagonally through walls
    		currentRayTileX += rayXDelta;
    		collidedWall = getLevelWall((int) currentRayTileX, (int) currentRayTileZ);
    		if(collidedWall != null) {
    			wallSpriteHorizontalPercentage = currentRayTileZ % 1;
    			break;
    		}
    		currentRayTileZ += rayZDelta;
    		collidedWall = getLevelWall((int) currentRayTileX, (int) currentRayTileZ);
    		if(collidedWall != null) {
    			wallSpriteHorizontalPercentage = currentRayTileX % 1;
    			break;
    		}

    		// Check whether the ray has left the tile map, and if so stop trying to find further walls
    		if((currentRayTileX < 0) || (currentRayTileZ < 0) || (currentRayTileX >= tileMap.width) || (currentRayTileZ >= tileMap.height)) break;
    	}
        
        // If a wall was found, calculate how far away it is and draw it to the screen
        if(collidedWall != null) {
        	final float rayTileXDistanceSquared = (currentRayTileX - cameraTileX) * (currentRayTileX - cameraTileX);
        	final float rayTileZDistanceSquared = (currentRayTileZ - cameraTileZ) * (currentRayTileZ - cameraTileZ);
        	final float wallTileDistance = (float) Math.sqrt(rayTileXDistanceSquared + rayTileZDistanceSquared) * (float) Math.cos(horizontalAngle);
        	
        	final int screenWallHeight = (int) ((16 * tileSize) / wallTileDistance);
        	
        	int wallSpriteX = (int) (wallSpriteHorizontalPercentage * collidedWall.sprite.width);
        	
        	final float wallDistance = wallTileDistance * tileSize;
        	final int screenWallTop = (renderPane.height / 2) - (screenWallHeight / 2);
        	final int screenWallBottom = (renderPane.height / 2) + (screenWallHeight / 2);
        	
        	for(int screenY = Math.max(0, screenWallTop); screenY < Math.min(renderPane.height, screenWallBottom); screenY++) {
        		final float wallSpriteVerticalPercentage = (screenY - screenWallTop) / (1.0f * screenWallHeight);
        		final int wallSpriteY = (int) (wallSpriteVerticalPercentage * collidedWall.sprite.height);
        		
        		final int colour = collidedWall.sprite.pixels[wallSpriteX + wallSpriteY * collidedWall.sprite.width];
        		renderPane.setPixel(screenX, screenY, wallDistance, colour);
        	}
        }
		
	}
	
	private void drawFloorAndCeilingColumn(final RenderPane3D renderPane, final int screenX, final float cameraTileX, final float cameraTileZ, final float horizontalAngle, final float rayAngle) {
		final int halfScreenHeight = (renderPane.height / 2);
		
		for(int screenY = 0; screenY < renderPane.height; screenY++) {
			final float relativeScreenY = (1.0f * screenY - halfScreenHeight) / renderPane.height;
			final float verticalAngle = relativeScreenY / player.camera.minRenderDistance;
			
			final boolean isFloor = (relativeScreenY >= 0);
			float floorZDistance = isFloor ? (5 / verticalAngle) : -(5 / verticalAngle);
			floorZDistance /= Math.cos(horizontalAngle);
			
			final float worldTileX = floorZDistance * (float) Math.sin(rayAngle) + cameraTileX;
			final float worldTileZ = floorZDistance * (float) Math.cos(rayAngle) + cameraTileZ;
			
			final Tile currentTile = isFloor ? getLevelFloorTile((int) Math.floor(worldTileX), (int) Math.floor(worldTileZ)) : getLevelCeilingTile((int) Math.floor(worldTileX), (int) Math.floor(worldTileZ)); 
			if(currentTile != null) {
				final int spriteX = (int) ((worldTileX % 1) * currentTile.sprite.width);
				final int spriteZ = (int) ((worldTileZ % 1) * currentTile.sprite.height);
				
				int colour = currentTile.sprite.pixels[spriteX + spriteZ * currentTile.sprite.width];
				if(spriteX == 0) colour = 0xffff0000;
				if(spriteZ == 0) colour = 0xff00ff00;
				
				renderPane.setPixel(screenX, screenY, floorZDistance * tileSize, colour);
			}
		}
	}
	
}