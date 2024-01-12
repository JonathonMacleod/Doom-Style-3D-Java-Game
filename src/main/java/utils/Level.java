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

	public float tileSize = 16.0f;
	public float floorTileDepth = 4.0f;
	public float ceilingTileHeight = 4.0f;
	
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
		ceilingTileHeight = 4;
		floorTileDepth = 8;

		int x = 40;
		int z = 37;
		
		// Draw the floor and ceiling tiles of the level to the render pane based on the player's camera location.
		drawLevelFloorAndCeiling(renderPane, x, z);
		
		final int wallRenderRadius = 25;
		final int wallCameraX = (int) (player.camera.x / tileSize);
		final int wallCameraZ = (int) (player.camera.z / tileSize);
		final int startWallX = Math.max(0, wallCameraX - wallRenderRadius);
		final int endWallX = Math.min(tileMap.width, wallCameraX + wallRenderRadius);
		final int startWallZ = Math.max(0, wallCameraZ - wallRenderRadius);
		final int endWallZ = Math.min(tileMap.height, wallCameraZ + wallRenderRadius);
		for(int levelTileY = startWallZ; levelTileY < endWallZ; levelTileY++) {
			for(int levelTileX = startWallX; levelTileX < endWallX; levelTileX++) {
				final Wall currentWall = getLevelWall(levelTileX, levelTileY);
				if(currentWall != null) {
					drawWall(renderPane, currentWall, levelTileX, levelTileY);
				}
			}
		}
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
				player.camera.angle = (float) Math.PI;
				
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
	
	private void drawLevelFloorAndCeiling(final RenderPane3D renderPane, int targetX, int targetZ) {
		final float screenSpaceCenterX = (renderPane.width / 2.0f);
		final float screenSpaceCenterY = (renderPane.height / 2.0f);
		
		// Calculate the sin and cosine of the negative camera angle (as the math used for rotation depends on counter clockwise, but we rotate clockwise).
		final float cameraAngleCos = (float) Math.cos(-player.camera.angle);
		final float cameraAngleSin = (float) Math.sin(-player.camera.angle);

		// Iterate through every row in the screen to calculate the floor or ceiling colour and distance at that point.
		for(int screenSpaceY = 0; screenSpaceY < renderPane.height; screenSpaceY++) {
			// Calculate the position of the current screen space Y position (0 to height - 1) in clip-space (-0.5 to 0.5).
			final float clipSpaceY = (screenSpaceY - screenSpaceCenterY) / renderPane.height;
			boolean isFloor = (clipSpaceY >= 0);
			
			// Calculate the distance of the current floor/ceiling tiles at this row in the screen space.
			// This is done by finding the angle between the camera and the current position in the clip space. Since the render pane is the camera's minimum
			// render distance away from the camera position, we can use the right-angle triangle formula: 
			// tan(angle) = opposite / adjacent = clipSpaceY / min_clipping_distance
			//
			// Since the angle should be small, we can get away with using small angle approximations to say:
			// tan(angle) = angle
			//
			// Now we know the angle, and we know the distance between the camera and the floor/ceiling using the depth and height constants, we can calculate
			// the distance until a ray fired at that angle would intercept the floor or ceiling using: 
			// clipSpaceZ = floor_or_ceiling_height / sin(angle)
			//
			// Or using another small angle approximation:
			// sin(angle) = angle
			// clipSpaceZ = floor_or_ceiling_height / angle
			final float verticalAngle = (float) clipSpaceY / player.camera.minRenderDistance;
			final float floorCeilingHeight = isFloor ? (floorTileDepth * tileSize + player.camera.y) : (-ceilingTileHeight * tileSize + player.camera.y);
			float clipSpaceZ = floorCeilingHeight / verticalAngle;

			// At clipSpaceY values of 0, clipSpaceZ values will be infinity, so ensure that the distance of the floor and ceiling is within the maximum render
			// distance to save computation.
			if(clipSpaceZ > player.camera.maxRenderDistance) continue;
			
			// Iterate through every column in the screen row to calculate the floor or ceiling colour at that point.
			for(int screenSpaceX = 0; screenSpaceX < renderPane.width; screenSpaceX++) {
				// Calculate the position of the current screen space X position (0 to width - 1) in clip-space (-0.5 to 0.5).
				final float clipSpaceX = (screenSpaceX - screenSpaceCenterX) / renderPane.width;

				// Approximate the angle of the current pixel by assuming that the angle at the right-hand side is the camera FOV. Since we want the player to
				// be looking forward, we must calculate this angle in a range between -FOV/2 and FOV/2, so at width / 2 the angle is 0.
				final float horizontalAngle = player.camera.fovRadians * clipSpaceX;
				
				// Calculate the relative X offset between the camera and the floor/ceiling at a distance of clipSpaceZ from the camera using 
				// the right-angle triangle formula: xOffset = tan(horizontalAngle) * clipSpaceZ.
				// Using small angle approximations we can also use sin(angle) = angle to say: xOffset = horizontalAngle * clipSpaceZ
				final float worldSpaceXOffset = horizontalAngle * clipSpaceZ;
				
				// Rotate the projection plane x offset and the clip space z distance around the camera position.
				final float worldSpaceX = (cameraAngleCos * worldSpaceXOffset - cameraAngleSin * clipSpaceZ) + player.camera.x;
				final float worldSpaceZ  = (cameraAngleSin * worldSpaceXOffset + cameraAngleCos * clipSpaceZ) + player.camera.z;

				// Calculate the current tile coordinate at the world space position for the current pixel.
				final int worldSpaceTileX = (int) (worldSpaceX / tileSize);
				final int worldSpaceTileZ = (int) (worldSpaceZ / tileSize);
				
				// Make sure the tile being drawn is within the level.
				if((worldSpaceTileX >= 0) && (worldSpaceTileX < tileMap.width) && (worldSpaceTileZ >= 0) && (worldSpaceTileZ < tileMap.height)) {
					// Find the tile corresponding to the tile location in the tile map.
					final int tileMapIndex = worldSpaceTileX + worldSpaceTileZ * tileMap.width;
					final int tileMapColour = tileMap.pixels[tileMapIndex];
					final Tile currentTile = isFloor ? Tile.getFloorTile(tileMapColour) : Tile.getCeilingTile(tileMapColour);
					
					// If we have a tile at the location in the tile map then draw it.
					if(currentTile != null) {
						// Calculate how far through the tile sprite we are based on the world position and the tile size, then find the colour of the pixel at that sprite position.
						int xTilePixel = (int) ((Math.abs(worldSpaceX * 1.0f) % tileSize) / tileSize * currentTile.sprite.width);
						int zTilePixel = (int) ((Math.abs(worldSpaceZ * 1.0f) % tileSize) / tileSize * currentTile.sprite.height);
						int colour = currentTile.sprite.pixels[xTilePixel + (currentTile.sprite.height - zTilePixel - 1) * currentTile.sprite.width];
						
						if(worldSpaceTileX == 45 && worldSpaceTileZ == 39) colour = 0xffff00ff;
						
						if(xTilePixel == 0) colour = 0xffff00ff;
						if(zTilePixel == 0) colour = 0xff7f007f;
						
						if(worldSpaceTileX == targetX && worldSpaceTileZ == targetZ) colour = 0xff77ff;
						
						renderPane.setPixel(screenSpaceX, screenSpaceY, clipSpaceZ, colour);
					}
				}
			}
		}
	}

	private void drawWall(final RenderPane3D renderPane, final Wall wall, final int wallWorldSpaceTileX, final int wallWorldSpaceTileZ) {
		// Draw the four surfaces of the wall
		//TODO: Cull these surfaces so only relevant ones are drawn
		drawWallSurface(renderPane, wall.sprite, wallWorldSpaceTileX, wallWorldSpaceTileZ, 1, 0);
		drawWallSurface(renderPane, wall.sprite, wallWorldSpaceTileX + 1, wallWorldSpaceTileZ, 0, 1);
		drawWallSurface(renderPane, wall.sprite, wallWorldSpaceTileX + 1, wallWorldSpaceTileZ + 1, -1, 0);
		drawWallSurface(renderPane, wall.sprite, wallWorldSpaceTileX, wallWorldSpaceTileZ + 1, 0, -1);
	}
	
	private void drawWallSurface2(final RenderPane3D renderPane, final Sprite sprite, final int wallWorldSpaceTileX, final int wallWorldSpaceTileZ, final int wallWorldSpaceTileXLength, final int wallWorldSpaceTileZDepth) {
		final float cameraAngleSin = (float) Math.sin(player.camera.angle);
		final float cameraAngleCos = (float) Math.cos(player.camera.angle);
		final float worldSpaceCameraTileX = (player.camera.x / tileSize);
		final float worldSpaceCameraTileY = (player.camera.y / tileSize);
		final float worldSpaceCameraTileZ = (player.camera.z / tileSize);

		// Calculate the X and Z positions of the left and right sides of the wall relative to the camera
		final float relativeWallLeftWorldSpaceTileX = (wallWorldSpaceTileX - worldSpaceCameraTileX) * 2;
		final float relativeWallLeftWorldSpaceTileZ = (wallWorldSpaceTileZ - worldSpaceCameraTileZ) * 2;
		final float relativeWallRightWorldSpaceTileX = (wallWorldSpaceTileX + wallWorldSpaceTileXLength - worldSpaceCameraTileX) * 2;
		final float relativeWallRightWorldSpaceTileZ = (wallWorldSpaceTileZ + wallWorldSpaceTileZDepth - worldSpaceCameraTileZ) * 2;
		
		// Rotate the wall positions around the camera based on where the camera is currently looking
		final float rotatedRelativeWallLeftWorldSpaceTileX = relativeWallLeftWorldSpaceTileX * cameraAngleCos - relativeWallLeftWorldSpaceTileZ * cameraAngleSin + 0.3f * cameraAngleCos;
		final float rotatedRelativeWallLeftWorldSpaceTileZ = relativeWallLeftWorldSpaceTileZ * cameraAngleCos + relativeWallLeftWorldSpaceTileX * cameraAngleSin + 0.3f * cameraAngleSin;
		final float rotatedRelativeWallRightWorldSpaceTileX = relativeWallRightWorldSpaceTileX * cameraAngleCos - relativeWallRightWorldSpaceTileZ * cameraAngleSin + 0.3f * cameraAngleCos;
		final float rotatedRelativeWallRightWorldSpaceTileZ = relativeWallRightWorldSpaceTileZ * cameraAngleCos + relativeWallRightWorldSpaceTileX * cameraAngleSin + 0.3f * cameraAngleSin;
		
		// Calculate the vertical wall positions using the known ceiling and floor positions
		final float wallTop = (-0.5f + worldSpaceCameraTileY) * (ceilingTileHeight / 2);
		final float wallBottom = (0.5f + worldSpaceCameraTileY) * (floorTileDepth / 2);
		
		// If both the left and right sides of the wall are behind the minimum render distance then stop rendering
		if((rotatedRelativeWallLeftWorldSpaceTileZ < player.camera.minRenderDistance) && (rotatedRelativeWallRightWorldSpaceTileZ < player.camera.minRenderDistance)) {
			return;
		}
		
		// Assume that both sides of the wall are visible initially until otherwise determined
		float visibleWallLeftWorldSpaceTileX = rotatedRelativeWallLeftWorldSpaceTileX;
		float visibleWallLeftWorldSpaceTileZ = rotatedRelativeWallLeftWorldSpaceTileZ;
		float visibleWallRightWorldSpaceTileX = rotatedRelativeWallRightWorldSpaceTileX;
		float visibleWallRightWorldSpaceTileZ = rotatedRelativeWallRightWorldSpaceTileZ;
		
		// If the left side of the wall is behind the minimum render distance then work out the earliest point that can be seen infront of the minimum render distance
		if(rotatedRelativeWallLeftWorldSpaceTileZ < player.camera.minRenderDistance) {
			// Work out how much distance of the wall is not visible behind the minimum render distance
			final float clippedZDepth = player.camera.minRenderDistance - rotatedRelativeWallLeftWorldSpaceTileZ;
			// Work out the difference in Z values between the left and right sides of the wall
			final float actualZDepth = rotatedRelativeWallRightWorldSpaceTileZ - rotatedRelativeWallLeftWorldSpaceTileZ;
			// Calculate the percentage of the wall not visible behind the minimum render distance
			final float clippedWallPercentage = clippedZDepth / actualZDepth;

			// Calculate the first positions of the left hand side of the wall that is visible
			visibleWallLeftWorldSpaceTileX = rotatedRelativeWallLeftWorldSpaceTileX + (rotatedRelativeWallRightWorldSpaceTileX - rotatedRelativeWallLeftWorldSpaceTileX) * clippedWallPercentage;
			visibleWallLeftWorldSpaceTileZ = rotatedRelativeWallLeftWorldSpaceTileZ + (rotatedRelativeWallRightWorldSpaceTileZ - rotatedRelativeWallLeftWorldSpaceTileZ) * clippedWallPercentage;
		} 
		
		// If the right side of the wall is behind the minimum render distance then work out the earliest point that can be seen infront of the minimum render distance
		if(rotatedRelativeWallRightWorldSpaceTileZ < player.camera.minRenderDistance) {
			// Work out how much distance of the wall is not visible behind the minimum render distance
			final float clippedZDepth = player.camera.minRenderDistance - rotatedRelativeWallLeftWorldSpaceTileZ;
			// Work out the difference in Z values between the left and right sides of the wall
			final float actualZDepth = rotatedRelativeWallRightWorldSpaceTileZ - rotatedRelativeWallLeftWorldSpaceTileZ;
			// Calculate the percentage of the wall not visible behind the minimum render distance
			final float clippedWallPercentage = clippedZDepth / actualZDepth;

			// Calculate the first positions of the right hand side of the wall that is visible
			visibleWallRightWorldSpaceTileX = rotatedRelativeWallLeftWorldSpaceTileX + (rotatedRelativeWallRightWorldSpaceTileX - rotatedRelativeWallLeftWorldSpaceTileX) * clippedWallPercentage;
			visibleWallRightWorldSpaceTileZ = rotatedRelativeWallLeftWorldSpaceTileZ + (rotatedRelativeWallRightWorldSpaceTileZ - rotatedRelativeWallLeftWorldSpaceTileZ) * clippedWallPercentage;
		}

		// Calculate the screen positions of the wall
		final int actualScreenSpaceLeft = (int) ((visibleWallLeftWorldSpaceTileX / visibleWallLeftWorldSpaceTileZ) * renderPane.width + (renderPane.width / 2.0f));
		final int actualScreenSpaceRight = (int) ((visibleWallRightWorldSpaceTileX / visibleWallRightWorldSpaceTileZ) * renderPane.width + (renderPane.width / 2.0f));
		final float actualScreenWidth = (float) (actualScreenSpaceRight - actualScreenSpaceLeft);

		// If the right hand side of the wall is being drawn to the left of the left hand side of the wall we are seeing the wall from behind, so don't render it
		if(actualScreenSpaceLeft >= actualScreenSpaceRight) {
			return;
		}
		
		// Clip the screen space coordinates to the size of the render pane
		final int screenSpaceLeft = actualScreenSpaceLeft < 0 ? 0 : actualScreenSpaceLeft;
		final int screenSpaceRight = actualScreenSpaceRight > renderPane.width ? renderPane.width : actualScreenSpaceRight;
		
		// Calculate the screen position of the bottom and top of the wall at both the left and right sides
		final int screenSpaceLeftTop = (int) ((wallTop / visibleWallLeftWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f));
		final int screenSpaceLeftBottom = (int) ((wallBottom / visibleWallLeftWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f));
		final int screenSpaceRightTop = (int) ((wallTop / visibleWallRightWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f));
		final int screenSpaceRightBottom = (int) ((wallBottom / visibleWallRightWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f));
		
		// Now iterate through the visible columns of the wall to calculate the height of the wall at that X position, and draw the textured column
		for(int screenSpaceX = screenSpaceLeft; screenSpaceX < screenSpaceRight; screenSpaceX++) {
			// So we can interpolate the Z distance and also the sprite texture position, determine how far through the wall we are 
			final float xPercentage = (float) (screenSpaceX - actualScreenSpaceLeft) / (actualScreenSpaceRight - actualScreenSpaceLeft);
			final float currentZ = visibleWallLeftWorldSpaceTileZ + (visibleWallRightWorldSpaceTileZ - visibleWallLeftWorldSpaceTileZ) * xPercentage;
			final int spritePixelX = (int) ((screenSpaceX - actualScreenSpaceLeft) / actualScreenWidth * sprite.width);

			// Interpolate the known wall vertical positions to find the current wall height
			final int currentActualWallTop = (int) (screenSpaceLeftTop + (screenSpaceRightTop - screenSpaceLeftTop) * xPercentage - 0.5);
			final int currentActualWallBottom = (int) (screenSpaceLeftBottom + (screenSpaceRightBottom - screenSpaceLeftBottom) * xPercentage);
			final float currentActualWallHeight = (float) (currentActualWallBottom - currentActualWallTop);
			final int currentWallTop = currentActualWallTop < 0 ? 0 : currentActualWallTop;
			final int currentWallBottom = currentActualWallBottom > renderPane.height ? renderPane.height : currentActualWallBottom;
			
			// Now iterate through each row of the current wall column to draw the sprite texture at that point
			for(int screenSpaceY = currentWallTop; screenSpaceY < currentWallBottom; screenSpaceY++) {
				final int spritePixelY = (int) (((screenSpaceY - currentWallTop) + (currentWallTop - currentActualWallTop)) / currentActualWallHeight * sprite.height);
				
				final int colour = sprite.pixels[spritePixelX + spritePixelY * sprite.width];
				renderPane.setPixel(screenSpaceX, screenSpaceY, currentZ, colour);
			}
		}
	}
	
	private void drawWallSurface(final RenderPane3D renderPane, final Sprite sprite, final int wallWorldSpaceTileX, final int wallWorldSpaceTileZ, final int wallWorldSpaceTileXLength, final int wallWorldSpaceTileZDepth) {
		final float cameraAngleCos = (float) Math.cos(player.camera.angle);
		final float cameraAngleSin = (float) Math.sin(player.camera.angle);
		
		// Calculate the left and right boundaries of the wall in world space
		final float worldSpaceWallLeftX = wallWorldSpaceTileX * tileSize;
		final float worldSpaceWallLeftZ = wallWorldSpaceTileZ * tileSize;
		final float worldSpaceWallRightX = (wallWorldSpaceTileX + wallWorldSpaceTileXLength) * tileSize;
		final float worldSpaceWallRightZ = (wallWorldSpaceTileZ + wallWorldSpaceTileZDepth) * tileSize;

		// Calculate the position of the wall boundaries relative to the player in world space
		final float relativeWorldSpaceWallLeftX = worldSpaceWallLeftX - player.camera.x;
		final float relativeWorldSpaceWallLeftZ = worldSpaceWallLeftZ - player.camera.z;
		final float relativeWorldSpaceWallRightX = worldSpaceWallRightX - player.camera.x;
		final float relativeWorldSpaceWallRightZ = worldSpaceWallRightZ - player.camera.z;
		
		// Calculate the position of the wall boundaries relative to the player with the player's rotation
		final float rotatedWorldSpaceWallLeftX = (cameraAngleCos * relativeWorldSpaceWallLeftX - cameraAngleSin * relativeWorldSpaceWallLeftZ);
		final float rotatedWorldSpaceWallLeftZ = (cameraAngleSin * relativeWorldSpaceWallLeftX + cameraAngleCos * relativeWorldSpaceWallLeftZ);
		final float rotatedWorldSpaceWallRightX = (cameraAngleCos * relativeWorldSpaceWallRightX - cameraAngleSin * relativeWorldSpaceWallRightZ);
		final float rotatedWorldSpaceWallRightZ = (cameraAngleSin * relativeWorldSpaceWallRightX + cameraAngleCos * relativeWorldSpaceWallRightZ);

		// If both the left and right boundaries of the wall are behind the camera then stop trying to render it
		if((rotatedWorldSpaceWallLeftZ < player.camera.minRenderDistance) && (rotatedWorldSpaceWallRightZ < player.camera.minRenderDistance)) {
			return;
		}
		
		// Calculate the width and depth of the wall in world space (ignoring clipping)
		final float actualWorldWallWidth = rotatedWorldSpaceWallRightX - rotatedWorldSpaceWallLeftX;
		final float actualWorldWallDepth = rotatedWorldSpaceWallRightZ - rotatedWorldSpaceWallLeftZ;
		
		// Calculate the clipped position of the wall in world space
		float clippedWorldSpaceLeftX = rotatedWorldSpaceWallLeftX;
		float clippedWorldSpaceLeftZ = rotatedWorldSpaceWallLeftZ;
		float clippedWorldSpaceRightX = rotatedWorldSpaceWallRightX;
		float clippedWorldSpaceRightZ = rotatedWorldSpaceWallRightZ;
		if(clippedWorldSpaceLeftZ < player.camera.minRenderDistance) {
			// Calculate how much of the wall depth is behind the camera and use that to work how the relative position of the clipped point on the minimum render distance
			final float clippedDepth = player.camera.minRenderDistance - clippedWorldSpaceLeftZ;
			final float percentageClipped = clippedDepth / actualWorldWallDepth;

			// Calculate the position of the clipped point
			clippedWorldSpaceLeftX = clippedWorldSpaceLeftX + (percentageClipped * actualWorldWallWidth);
			clippedWorldSpaceLeftZ = clippedWorldSpaceLeftZ + (percentageClipped * actualWorldWallDepth);
		}
		if(clippedWorldSpaceRightZ < player.camera.minRenderDistance) {
			// Calculate how much of the wall depth is infront of the camera and use that to work how the relative position of the clipped point on the minimum render distance
			final float clippedDepth = player.camera.minRenderDistance - clippedWorldSpaceLeftZ;
			final float percentageClipped = clippedDepth / actualWorldWallDepth;

			// Calculate the position of the clipped point
			clippedWorldSpaceRightX = clippedWorldSpaceLeftX + (percentageClipped * actualWorldWallWidth);
			clippedWorldSpaceRightZ = clippedWorldSpaceLeftZ + (percentageClipped * actualWorldWallDepth);
		}
		
		// Calculate the screen position of the left and right boundaries of the wall
		final float clipSpaceLeftX = player.camera.minRenderDistance * (clippedWorldSpaceLeftX * (tileSize / 2) / clippedWorldSpaceLeftZ);
		final int screenSpaceLeftX = (int) (clipSpaceLeftX * renderPane.width + (renderPane.width / 2.0f));
		final float clipSpaceRightX = player.camera.minRenderDistance * (clippedWorldSpaceRightX * (tileSize / 2) / clippedWorldSpaceRightZ);
		final int screenSpaceRightX = (int) (clipSpaceRightX * renderPane.width + (renderPane.width / 2.0f));
		final int screenSpaceWidth = screenSpaceRightX - screenSpaceLeftX;
	
		// If the right side of the wall is before the left side then we are on the wrong side to render it
		if(screenSpaceLeftX >= screenSpaceRightX) {
			return;
		}
		
		// Calculate the left and right positions of the wall on the screen (clipped)
		final int clippedScreenSpaceLeftX = screenSpaceLeftX < 0 ? 0 : screenSpaceLeftX;
		final int clippedScreenSpaceRightX = screenSpaceRightX > renderPane.width ? renderPane.width : screenSpaceRightX;
	
		final float clipSpaceLeftTop = (ceilingTileHeight * tileSize / clippedWorldSpaceLeftZ) * player.camera.minRenderDistance;
		final float clipSpaceLeftBottom = (floorTileDepth * tileSize / clippedWorldSpaceLeftZ) * player.camera.minRenderDistance;
		final float clipSpaceRightTop = (ceilingTileHeight * tileSize / clippedWorldSpaceRightZ) * player.camera.minRenderDistance;
		final float clipSpaceRightBottom = (floorTileDepth * tileSize / clippedWorldSpaceRightZ) * player.camera.minRenderDistance;
		
		final int screenSpaceLeftTop = (int) ((renderPane.height / 2.0f) - (renderPane.height * clipSpaceLeftTop));
		final int screenSpaceLeftBottom = (int) ((renderPane.height / 2.0f) + (renderPane.height * clipSpaceLeftBottom));
		final int screenSpaceRightTop = (int) ((renderPane.height / 2.0f) - (renderPane.height * clipSpaceRightTop));
		final int screenSpaceRightBottom = (int) ((renderPane.height / 2.0f) + (renderPane.height * clipSpaceRightBottom));

		// Iterate through each column of the wall on the screen
		for(int screenSpaceX = clippedScreenSpaceLeftX; screenSpaceX < clippedScreenSpaceRightX; screenSpaceX++) {
			final float percentAcrossWidth = (screenSpaceX - screenSpaceLeftX * 1.0f) / screenSpaceWidth;
			final int spriteX = (int) (percentAcrossWidth * sprite.width);
			
			final float horizontalAngle = (screenSpaceX * 1.0f / renderPane.width) * player.camera.fovRadians - (player.camera.fovRadians / 2.0f);
			final float currentClipSpaceZ = (rotatedWorldSpaceWallLeftZ + (percentAcrossWidth * actualWorldWallDepth)) / (float) Math.cos(horizontalAngle);
			
			final int currentScreenSpaceTop = (int) (percentAcrossWidth * (screenSpaceRightTop - screenSpaceLeftTop) + screenSpaceLeftTop);
			final int currentScreenSpaceBottom = (int) (percentAcrossWidth * (screenSpaceRightBottom - screenSpaceLeftBottom) + screenSpaceLeftBottom);
			final int currentScreenSpaceHeight = currentScreenSpaceBottom - currentScreenSpaceTop;
			
			final int currentClippedScreenSpaceTop = currentScreenSpaceTop < 0 ? 0 : currentScreenSpaceTop;
			final int currentClippedScreenSpaceBottom = currentScreenSpaceBottom >= renderPane.height ? renderPane.height : currentScreenSpaceBottom;
			
			for(int screenSpaceY = currentClippedScreenSpaceTop; screenSpaceY < currentClippedScreenSpaceBottom; screenSpaceY++) {
				final float percentAcrossHeight = (screenSpaceY - currentScreenSpaceTop * 1.0f) / currentScreenSpaceHeight;
				final int spriteY = (int) (percentAcrossHeight * sprite.height);
				
				int colour = sprite.pixels[spriteX + spriteY * sprite.width];
				if(spriteX == 0) colour = 0xffff00ff;
				if(spriteY == 0) colour = 0xff00ff00;
				
				renderPane.setPixel(screenSpaceX, screenSpaceY, currentClipSpaceZ, colour);

				if(screenSpaceX == 40 && screenSpaceY == 130) {
					renderPane.setPixel(40, 130, 0xffffffff);
				}
			}
		}
	}
	
}