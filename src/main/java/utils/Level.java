package utils;

import java.util.ArrayList;

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
		
        for(int screenX = 0; screenX < renderPane.width; screenX++) {
        	// Calculate the angle of the ray being fired relative to the center of the screen (-FOV/2 to FOV/2)
        	float horizontalAngle = ((1.0f * screenX - (renderPane.width / 2)) / renderPane.width);
        	// Calculate the angle of the ray being fired (adjusted for the camera's angle offset)
        	float rayAngle = player.camera.angle + horizontalAngle;
        	
            // Draw the floor and ceiling for the current screen column
        	drawWallColumn(renderPane, screenX, cameraTileX, cameraTileZ, horizontalAngle, rayAngle);
        	drawFloorAndCeilingColumn(renderPane, screenX, cameraTileX, cameraTileZ, horizontalAngle, rayAngle);
        }
    }
	
	private void drawWallColumn(final RenderPane3D renderPane, final int screenX, final float cameraTileX, final float cameraTileZ, final float horizontalAngle, final float rayAngle) {
		// This constant controls how accurate the ray distance is by determining how much a ray moves before wall collision is checked
		final float rayPrecision = 0.001f;
		final int maximumRayIncrement = (int) (16 / rayPrecision);
		
		// Work out the current position of the ray being cast to find walls, and also how much it should travel in each direction based on it's angle
    	float currentRayTileX = cameraTileX;
    	float currentRayTileZ = cameraTileZ;
    	final float rayXDelta = (float) Math.sin(rayAngle) * rayPrecision;
    	final float rayZDelta = (float) Math.cos(rayAngle) * rayPrecision;
    	
    	// Search for a wall, moving one axis at a time to avoid clipped diagonally through walls
    	Wall collidedWall = null;
    	float wallSpriteHorizontalPercentage = 0;
    	for(int i = 0; i < maximumRayIncrement; i++) {
    		// Step forwards on the X axis and check for a collision on the left and right of a wall
    		currentRayTileX += rayXDelta;
    		collidedWall = getLevelWall((int) currentRayTileX, (int) currentRayTileZ);
    		if(collidedWall != null) {
    			wallSpriteHorizontalPercentage = currentRayTileZ % 1;
    			break;
    		}
    		
    		// Step forwards on the Z axis and check for a collision on the top and bottom of a wall
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
        	int wallSpriteX = (int) (wallSpriteHorizontalPercentage * collidedWall.sprite.width);

        	// Find the distance from the camera to the wall
        	final float rayTileXDistanceSquared = (currentRayTileX - cameraTileX) * (currentRayTileX - cameraTileX);
        	final float rayTileZDistanceSquared = (currentRayTileZ - cameraTileZ) * (currentRayTileZ - cameraTileZ);
        	float wallTileDistance = (float) (Math.sqrt(rayTileXDistanceSquared + rayTileZDistanceSquared) * Math.cos(horizontalAngle));
        	final float wallDistance = wallTileDistance * tileSize;

        	// Calculate the wall height based on the wall's distance from the camera
        	final float actualScreenWallHeight = (16 * tileSize) / wallTileDistance;
        	final int screenWallTop = (int) Math.floor((renderPane.height / 2) - (actualScreenWallHeight / 2));
        	final int screenWallBottom = (int) Math.ceil((renderPane.height / 2) + (actualScreenWallHeight / 2));
        	final int screenWallHeight = screenWallBottom - screenWallTop;
        	
        	// Iterate through each row of the wall and draw the sprite at each point
        	for(int screenY = Math.max(0, screenWallTop); screenY < Math.min(renderPane.height, screenWallBottom); screenY++) {
        		final float wallSpriteVerticalPercentage = (screenY - screenWallTop) / (1.0f * screenWallHeight);
        		final int wallSpriteY = (int) (wallSpriteVerticalPercentage * collidedWall.sprite.height);

        		final int colour = collidedWall.sprite.pixels[wallSpriteX + wallSpriteY * collidedWall.sprite.width];
        		renderPane.setPixel(screenX, screenY, wallDistance, colour);
        	}
        }
        
	}
	
	private void drawFloorAndCeilingColumn(final RenderPane3D renderPane, final int screenX, final float cameraTileX, final float cameraTileZ, final float horizontalAngle, final float rayAngle) {
		final float cameraAngleSin = (float) Math.sin(player.camera.angle);
		final float cameraAngleCos = (float) Math.cos(player.camera.angle);
		final float horizontalAngleSin = (float) Math.sin(horizontalAngle);
		final float horizontalAngleCos = (float) Math.cos(horizontalAngle);
		
		final int halfScreenHeight = (renderPane.height / 2);
		
		for(int screenY = 0; screenY < renderPane.height; screenY++) {
			// Calculate the vertical angle between the ray being projected to the floor/ceiling based on how far we are vertically through the column.
			// We know that: tan(verticalAngle) = relative_screen_y / min_render_distance
			// Using the small angle approximation that tan(angle) = angle: verticalAngle = relative_screen_y / min_render_distance
			final float relativeScreenY = (1.0f * screenY - halfScreenHeight) / renderPane.height;
			final float verticalAngle = relativeScreenY / player.camera.minRenderDistance;

			// Determine whether the current pixel is for a floor ceiling based on whether it's the top or bottom half of the screen
			final boolean isFloor = (relativeScreenY >= 0);
			// Calculate the Z distance to the floor/ceiling tile for the current pixel by using the wall height and the vertical angle of the ray.
			// We know: z_distance = wall_height / tan(verticalAngle)
			// Using the small angle approximation that tan(angle) = angle: z_distance = wall_height / verticalAngle
			float floorZDelta = isFloor ? (5 / verticalAngle) : -(5 / verticalAngle);
			// Calculate the X distance to the floor/ceiling tile for the current pixel based on the distance and the current ray angle relative to the center of the camera.
			// We know: x_distance = z_distance * sin(horizontalAngle)
			float floorXDelta = floorZDelta * horizontalAngleSin;
			// Correct for the fishbowl effect
			floorZDelta *= horizontalAngleCos;
			
			// Rotate the relative tile position based on the camera's current angle
			final float worldTileX = (float) (floorXDelta * cameraAngleCos + floorZDelta * cameraAngleSin) + cameraTileX;
			final float worldTileZ = (float) (floorZDelta * cameraAngleCos - floorXDelta * cameraAngleSin) + cameraTileZ;

			// Get the tile for the current world position
			final Tile currentTile = isFloor ? getLevelFloorTile((int) Math.floor(worldTileX), (int) Math.floor(worldTileZ)) : getLevelCeilingTile((int) Math.floor(worldTileX), (int) Math.floor(worldTileZ)); 
			if(currentTile != null) {
				// Based on how far through the tile we are calculate the sprite position for the floor/ceiling tile
				final int spriteX = (int) ((worldTileX % 1) * currentTile.sprite.width);
				final int spriteZ = (int) ((worldTileZ % 1) * currentTile.sprite.height);
				
				final int colour = currentTile.sprite.pixels[spriteX + spriteZ * currentTile.sprite.width];
				renderPane.setPixel(screenX, screenY, floorZDelta * tileSize, colour);
			}
		}
	}
	
}