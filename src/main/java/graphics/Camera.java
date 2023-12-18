package graphics;

import utils.Level;
import utils.Wall;

public class Camera {
	
	public final float fovRadians;
	public float x, y, z, angle;
	public final float minRenderDistance, maxRenderDistance;
	
	public Camera(float fovDegrees, float nearPlane, float farPlane) {
		fovRadians = (float) Math.toRadians(fovDegrees);
		
		x = 0;
		y = 0;
		z = 0;
		angle = 0;
		
		minRenderDistance = nearPlane;
		maxRenderDistance = farPlane;
	}
	
	public void applyAxisMovements(Level level, float xDelta, float yDelta, float zDelta) {
		// Rotate the provided X and Z translations by the angle using a unit circle
		final float rotatedXDelta = (float) (xDelta * Math.cos(angle) + zDelta * Math.sin(angle));
		final float rotatedZDelta = (float) (zDelta * Math.cos(angle) - xDelta * Math.sin(angle));		

		// Calculate the X and Z movement separately so that we can move in one axis even if we are blocked in the other
		applyXMovement(level, rotatedXDelta);
		applyZMovement(level, rotatedZDelta);

		// Since there is no collision detection on the Y axis just accept any Y translations
		y += yDelta;
	}
	
	private void applyXMovement(Level level, float delta) {
		// Reserve some space around the edges of each tile, to ensure the player is not too close to a wall. This prevents the near clipping plane from 
		// touching a wall at small angles.
		final float collisionBuffer = 2 * minRenderDistance;
		
		// Calculate where the player is currently standing within the level (to the tile) before moving
		final float prevTileXPos = (x / level.tileSize);
		final int prevTileX = (int) prevTileXPos;
		final int prevTileZ = (int) (z / level.tileSize);

		// Check whether the player is colliding with any walls before moving
		if((prevTileX >= 0) && (prevTileZ >= 0) && (prevTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
			final int prevWallColour = level.tileMap.pixels[prevTileX + prevTileZ * level.tileMap.width];
			final Wall prevWall = Wall.getWall(prevWallColour);
			
			// If we are in a wall currently, no matter where we move, we can't collide more, so just accept the movement
			//TODO: Shuffle the player back into a non-colliding space
			if(prevWall != null) {
				x += delta;
				return;
			}
		}

		// Calculate where the player will be standing once the translation is accepted
		float newX = (float) (x + delta);
		final float newTileXPos = (newX / level.tileSize);
		final int newTileX = (int) newTileXPos;

		// Check whether the location the player is moving to collides with any walls
		if((newTileX >= 0) && (prevTileZ >= 0) && (newTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
			final int newWallColour = level.tileMap.pixels[newTileX + prevTileZ * level.tileMap.width];
			final Wall newWall = Wall.getWall(newWallColour);
			
			// If the position we are moving to has a wall then we can't move there
			if(newWall != null) {
				return;
			}
		}
		
		// At this point we have not encountered a wall, so the position is valid. However, we need to check that we aren't 
		// too close to another wall (to avoid the camera moving through nearby walls)
		int bufferTileX = -1;
		if(prevTileXPos < newTileXPos) {
			final float xSpaceRemaining = 1 - (newTileXPos % 1);
			if(xSpaceRemaining < collisionBuffer) bufferTileX = newTileX + 1;
		} else {
			final float xSpaceRemaining = (newTileXPos % 1);
			if(xSpaceRemaining < collisionBuffer) bufferTileX = newTileX - 1;
		}
		
		if(bufferTileX == -1) {
			x = newX;
		} else {
			if((bufferTileX >= 0) && (prevTileZ >= 0) && (bufferTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
				final int bufferWallColour = level.tileMap.pixels[bufferTileX + prevTileZ * level.tileMap.width];
				final Wall bufferWall = Wall.getWall(bufferWallColour);
				
				// If the position we are moving to doesn't have a wall too nearby then we can move
				if(bufferWall == null) {
					x = newX;
				}
			}
		}
	}
	
	private void applyZMovement(Level level, float delta) {
		// Reserve some space around the edges of each tile, to ensure the player is not too close to a wall. This prevents the near clipping plane from 
		// touching a wall at small angles.
		final float collisionBuffer = 2 * minRenderDistance;

		// Calculate where the player is currently standing within the level (to the tile) before moving
		final int prevTileX = (int) (x / level.tileSize);
		final float prevTileZPos = (z / level.tileSize);
		final int prevTileZ = (int) prevTileZPos;

		// Check whether the player is colliding with any walls before moving
		if((prevTileX >= 0) && (prevTileZ >= 0) && (prevTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
			final int prevWallColour = level.tileMap.pixels[prevTileX + prevTileZ * level.tileMap.width];
			final Wall prevWall = Wall.getWall(prevWallColour);
			
			// If we are in a wall currently, no matter where we move, we can't collide more, so just accept the movement
			//TODO: Shuffle the player back into a non-colliding space
			if(prevWall != null) {
				z += delta;
				return;
			}
		}

		// Calculate where the player will be standing once the translation is accepted
		final float newZ = (float) (z + delta);
		final float newTileZPos = (newZ / level.tileSize);
		final int newTileZ = (int) newTileZPos;

		// Check whether the location the player is moving to collides with any walls
		if((prevTileX >= 0) && (newTileZ >= 0) && (prevTileX < level.tileMap.width) && (newTileZ < level.tileMap.height)) {
			final int newWallColour = level.tileMap.pixels[prevTileX + newTileZ * level.tileMap.width];
			final Wall newWall = Wall.getWall(newWallColour);

			// If the position we are moving to has a wall then we can't move there
			if(newWall != null) {
				return;
			}
		}	
		
		// At this point we have not encountered a wall, so the position is valid. However, we need to check that we aren't 
		// too close to another wall
		int bufferTileZ = -1;
		if(prevTileZPos < newTileZPos) {
			final float zSpaceRemaining = 1 - (newTileZPos % 1);
			if(zSpaceRemaining < collisionBuffer) bufferTileZ = newTileZ + 1;
		} else {
			final float zSpaceRemaining = (newTileZPos % 1);
			if(zSpaceRemaining < collisionBuffer) bufferTileZ = newTileZ - 1;
		}
		
		if(bufferTileZ == -1) {
			z = newZ;
		} else {			
			if((prevTileX >= 0) && (bufferTileZ >= 0) && (prevTileX < level.tileMap.width) && (bufferTileZ < level.tileMap.height)) {
				final int bufferWallColour = level.tileMap.pixels[prevTileX + bufferTileZ * level.tileMap.width];
				final Wall bufferWall = Wall.getWall(bufferWallColour);
				
				// If the position we are moving to doesn't have a wall then we can move
				if(bufferWall == null) {
					z = newZ;
				}
			}
		}
	}
	
}
