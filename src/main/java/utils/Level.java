package utils;

import java.util.ArrayList;
import java.util.Random;

import graphics.Art;
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
		tileSize = 16;

		int x = 40;
		int z = 37;
		
		// Draw the floor and ceiling tiles of the level to the render pane based on the player's camera location.
		drawLevelFloorAndCeiling(renderPane, x, z);

		drawWall(renderPane, x, z);
		
		for(int i = x; i < x + 2; i++) {
			for(int j = z; j < z + 2; j++) {
				drawWall(renderPane, i, j);
			}
		}
		
		// Apply fog to each pixel in the render pane based on the Z buffer distance to that pixel.
		renderPane.applyFog(player.camera.maxRenderDistance, 0xff010401, 0.3f);
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
				
				// Calculate the relative X offset between the camera and the point on the projection plane at a distance of clipSpaceZ from the camera using 
				// the right-angle triangle formula:
				// xOffset = sin(horizontalAngle) * clipSpaceZ
				//
				// Using the small angle approximation that: 
				// sin(angle) = angle
				// xOffset = horizontalAngle * clipSpaceZ
				//
				// However, this produces a fish-bowl effect, where tiles to the left or right of the screen appear further away, since the distance between 
				// the camera and the projection plane is larger at larger magnitude horizontal angles then at a horizontal angle of 0. To compensate we can 
				// calculate the difference in distance from the camera to the clip pane at the current angle then at an angle of 0 using the right-angle 
				// triangle formula to the center of the plane. To compensate, we calculate the distance to the projection plane position using:
				// distanceToPlane = opposite / cos(angle) = min_render_distance / cos(horizontalAngle)
				//
				// Using the small angle approximation that:
				// cos(angle) = 1 - (angle * angle) / 2.0f
				// distanceToPlane = min_render_distance / (1 - (horizontalAngle * horizontalAngle) / 2.0f)
				//
				// Therefore, we can calculate the fishbowl effect at that point using:
				// fishbowlDelta = distanceToPlane - min_render_distance 
				// fishbowlDelta = min_render_distance / (1 - (horizontalAngle * horizontalAngle) / 2.0f) - min_render_distance
				final float worldSpaceXOffsetWithFishbowl = horizontalAngle * clipSpaceZ;
				final float fishbowlDelta = (player.camera.minRenderDistance / (1 - (horizontalAngle * horizontalAngle) / 2.0f)) - player.camera.minRenderDistance;
				final float worldSpaceXOffset = worldSpaceXOffsetWithFishbowl - fishbowlDelta;
				
				// Rotate the projection plane x offset and the clip space z distance around the camera position.
				final float worldSpaceX = (cameraAngleCos * worldSpaceXOffset - cameraAngleSin * clipSpaceZ) + player.camera.x;
				final float worldSpaceZ = (cameraAngleSin * worldSpaceXOffset + cameraAngleCos * clipSpaceZ) + player.camera.z;

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

	private void drawWall(final RenderPane3D renderPane, final int wallWorldSpaceTileX, final int wallWorldSpaceTileZ) {
		drawWallSurface(renderPane, Art.WALL_BRICK, wallWorldSpaceTileX, wallWorldSpaceTileZ, 1, 0);
		drawWallSurface(renderPane, Art.WALL_BRICK, wallWorldSpaceTileX + 1, wallWorldSpaceTileZ, 0, 1);
		drawWallSurface(renderPane, Art.WALL_BRICK, wallWorldSpaceTileX + 1, wallWorldSpaceTileZ + 1, -1, 0);
		drawWallSurface(renderPane, Art.WALL_BRICK, wallWorldSpaceTileX, wallWorldSpaceTileZ + 1, 0, -1);
	}
	
	private void drawWallSurface(final RenderPane3D renderPane, final Sprite sprite, final int wallWorldSpaceTileX, final int wallWorldSpaceTileZ, final int wallWorldSpaceTileXLength, final int wallWorldSpaceTileZDepth) {
		final float cameraAngleSin = (float) Math.sin(player.camera.angle);
		final float cameraAngleCos = (float) Math.cos(player.camera.angle);
		final float worldSpaceCameraTileX = (player.camera.x / tileSize) - (float) Math.sin(-player.camera.angle) * 0.3f;
		final float worldSpaceCameraTileY = (player.camera.y / tileSize);
		final float worldSpaceCameraTileZ = (player.camera.z / tileSize) - (float) Math.cos(-player.camera.angle) * 0.3f;

		// Calculate the X and Z positions of the left and right sides of the wall relative to the camera
		final float relativeWallLeftWorldSpaceTileX = (wallWorldSpaceTileX - worldSpaceCameraTileX) * 2;
		final float relativeWallLeftWorldSpaceTileZ = (wallWorldSpaceTileZ - worldSpaceCameraTileZ) * 2;
		final float relativeWallRightWorldSpaceTileX = (wallWorldSpaceTileX + wallWorldSpaceTileXLength - worldSpaceCameraTileX) * 2;
		final float relativeWallRightWorldSpaceTileZ = (wallWorldSpaceTileZ + wallWorldSpaceTileZDepth - worldSpaceCameraTileZ) * 2;
		
		// Rotate the wall positions around the camera based on where the camera is currently looking
		final float rotatedRelativeWallLeftWorldSpaceTileX = relativeWallLeftWorldSpaceTileX * cameraAngleCos - relativeWallLeftWorldSpaceTileZ * cameraAngleSin;
		final float rotatedRelativeWallLeftWorldSpaceTileZ = relativeWallLeftWorldSpaceTileZ * cameraAngleCos + relativeWallLeftWorldSpaceTileX * cameraAngleSin;
		final float rotatedRelativeWallRightWorldSpaceTileX = relativeWallRightWorldSpaceTileX * cameraAngleCos - relativeWallRightWorldSpaceTileZ * cameraAngleSin;
		final float rotatedRelativeWallRightWorldSpaceTileZ = relativeWallRightWorldSpaceTileZ * cameraAngleCos + relativeWallRightWorldSpaceTileX * cameraAngleSin;

		// Calculate the vertical wall positions
		final float wallTop = (-0.5f + worldSpaceCameraTileY) * 2;
		final float wallBottom = (0.5f + worldSpaceCameraTileY) * 2;
		
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
		final int actualScreenSpaceLeft = (int) ((visibleWallLeftWorldSpaceTileX / visibleWallLeftWorldSpaceTileZ) * renderPane.height + (renderPane.width / 2.0f));
		final int actualScreenSpaceRight = (int) ((visibleWallRightWorldSpaceTileX / visibleWallRightWorldSpaceTileZ) * renderPane.height + (renderPane.width / 2.0f));
		final float actualScreenWidth = (float) (actualScreenSpaceRight - actualScreenSpaceLeft);

		// If the right hand side of the wall is being drawn to the left of the left hand side of the wall we are seeing the wall from behind, so don't render it
		if(actualScreenSpaceLeft >= actualScreenSpaceRight) {
			return;
		}
		
		// Clip the screen space coordinates to the size of the render pane
		final int screenSpaceLeft = actualScreenSpaceLeft < 0 ? 0 : actualScreenSpaceLeft;
		final int screenSpaceRight = actualScreenSpaceRight > renderPane.width ? renderPane.width : actualScreenSpaceRight;
		
		// Calculate the screen position of the bottom and top of the wall at both the left and right sides
		final int screenSpaceLeftTop = (int) ((wallTop / visibleWallLeftWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f) + Math.cos((screenSpaceLeft - renderPane.width) / renderPane.width));
		final int screenSpaceLeftBottom = (int) ((wallBottom / visibleWallLeftWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f) + Math.cos((screenSpaceLeft - renderPane.width) / renderPane.width));
		final int screenSpaceRightTop = (int) ((wallTop / visibleWallRightWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f) + Math.cos((screenSpaceRight - renderPane.width) / renderPane.width));
		final int screenSpaceRightBottom = (int) ((wallBottom / visibleWallRightWorldSpaceTileZ) * renderPane.height + (renderPane.height / 2.0f) + Math.cos((screenSpaceRight - renderPane.width) / renderPane.width));
		
		for(int screenSpaceX = screenSpaceLeft; screenSpaceX < screenSpaceRight; screenSpaceX++) {
			final float xPercentage = (float) (screenSpaceX - screenSpaceLeft) / (screenSpaceRight - screenSpaceLeft);
			final float currentZ = visibleWallLeftWorldSpaceTileZ + (visibleWallRightWorldSpaceTileZ - visibleWallLeftWorldSpaceTileZ) * xPercentage;

			final int currentActualWallTop = (int) (screenSpaceLeftTop + (screenSpaceRightTop - screenSpaceLeftTop) * xPercentage);
			final int currentActualWallBottom = (int) (screenSpaceLeftBottom + (screenSpaceRightBottom - screenSpaceLeftBottom) * xPercentage);
			final float currentActualWallHeight = (float) (currentActualWallBottom - currentActualWallTop);
			
			final int currentWallTop = currentActualWallTop < 0 ? 0 : currentActualWallTop;
			final int currentWallBottom = currentActualWallBottom > renderPane.height ? renderPane.height : currentActualWallBottom;

			final int spritePixelX = (int) (((screenSpaceX - screenSpaceLeft) + (screenSpaceLeft - actualScreenSpaceLeft)) / actualScreenWidth * sprite.width);
			
			for(int screenSpaceY = currentWallTop; screenSpaceY < currentWallBottom; screenSpaceY++) {
				final int spritePixelY = (int) (((screenSpaceY - currentWallTop) + (currentWallTop - currentActualWallTop)) / currentActualWallHeight * sprite.height);
				
				int colour = sprite.pixels[spritePixelX + spritePixelY * sprite.width];
				if(spritePixelX == 0) colour = 0xff00ff00;
				if(spritePixelY == 0) colour = 0xff0000ff;
				
				renderPane.setPixel(screenSpaceX, screenSpaceY, currentZ, colour);
			}
		}
	}
	
}