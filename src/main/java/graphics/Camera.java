package graphics;

import utils.Level;
import utils.Wall;

public class Camera {

	public final float fovRadians;
	public float x, y, z, angle;
	
	public Camera(float fovDegrees) {
		fovRadians = (float) Math.toRadians(fovDegrees);
		
		x = 41 * 32 + 16;
		y = 0;
		z = 39 * 32 - 16;
		angle = (float) Math.PI;
	}
	
	public void applyAxisMovements(Level level, float xDelta, float yDelta, float zDelta) {
		final float rotatedXDelta = (float) (xDelta * Math.cos(angle) + zDelta * Math.sin(angle));
		final float rotatedZDelta = (float) (zDelta * Math.cos(angle) - xDelta * Math.sin(angle));		

		// Calculate the X and Z movement separately so that we can move in one axis even if we are blocked in the other
		applyXMovement(level, rotatedXDelta);
		applyZMovement(level, rotatedZDelta);

		y += yDelta;
	}
	
	private void applyXMovement(Level level, float delta) {
		// Reserve some space around the edges of each tile, to ensure the player is not too close to a wall.
		// This prevents the near clipping plane from touching a wall at small angles
		final float collisionBuffer = 8.0f / 32.0f;
		
		final float prevTileXPos = (x / 32.0f);
		final int prevTileX = (int) prevTileXPos;
		final int prevTileZ = ((int) z / 32) + 1;
		
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
		
		float newX = (float) (x + delta);
		final float newTileXPos = (newX / 32.0f);
		final int newTileX = (int) newTileXPos;

		if((newTileX >= 0) && (prevTileZ >= 0) && (newTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
			final int newWallColour = level.tileMap.pixels[newTileX + prevTileZ * level.tileMap.width];
			final Wall newWall = Wall.getWall(newWallColour);
			
			// If the position we are moving to has a wall then we can't move there
			if(newWall != null) {
				return;
			}
		}
		
		// At this point we have not encountered a wall, so the position is valid. However, we need to check that we aren't 
		// too close to another wall
		int bufferTileX = -1;
		if(prevTileXPos < newTileXPos) {
			final float xSpaceRemaining = 1 - (newTileXPos % 1);
			if(xSpaceRemaining < collisionBuffer) bufferTileX = newTileX + 1;
		} else {
			final float xSpaceRemaining = (newTileXPos % 1);
			System.out.println(xSpaceRemaining);
			if(xSpaceRemaining < collisionBuffer) bufferTileX = newTileX - 1;
		}
		
		if(bufferTileX == -1) {
			x = newX;
		} else {			
			if((bufferTileX >= 0) && (prevTileZ >= 0) && (bufferTileX < level.tileMap.width) && (prevTileZ < level.tileMap.height)) {
				final int bufferWallColour = level.tileMap.pixels[bufferTileX + prevTileZ * level.tileMap.width];
				final Wall bufferWall = Wall.getWall(bufferWallColour);
				
				// If the position we are moving to doesn't have a wall then we can move
				if(bufferWall == null) {
					x = newX;
				}
			}
		}
	}
	
	private void applyZMovement(Level level, float delta) {
		// Reserve some space around the edges of each tile, to ensure the player is not too close to a wall.
		// This prevents the near clipping plane from touching a wall at small angles
		final float collisionBuffer = 8.0f / 32.0f;
		
		final int prevTileX = (int) (x / 32);
		final float prevTileZPos = (z / 32.0f) + 1;
		final int prevTileZ = (int) prevTileZPos;
		
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

		final float newZ = (float) (z + delta);
		final float newTileZPos = (newZ / 32.0f) + 1;
		final int newTileZ = (int) newTileZPos;
		
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
