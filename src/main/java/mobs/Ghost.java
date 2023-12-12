package mobs;

import graphics.Art;
import ui.InputHandler;
import utils.Level;

public class Ghost extends Mob {

	int rand = (int) Math.random() * 1000;
	
	public Ghost(Level level, float x, float y, float z) {
		super(level, Art.MOB_GHOST, x, y, z, 2.0f);
	}
	
	@Override
	public void update(InputHandler inputHandler, float delta) {
		// Add ghost bobbing up and down
		y = (float) Math.sin(Math.toRadians(System.currentTimeMillis() / 4 + rand)) * 2;
		
		float xDiff = level.player.camera.x - x;
		float zDiff = level.player.camera.z - z;

		final float maxSpeed = 0.1f;
		final float minSpeed = -0.1f;
		if(xDiff > 0) {
			if(xDiff > maxSpeed) xDiff = maxSpeed;
		} else {
			if(xDiff < minSpeed) xDiff = minSpeed;
		}
		if(zDiff > 0) {
			if(zDiff > maxSpeed) zDiff = maxSpeed;
		} else {
			if(zDiff < minSpeed) zDiff = minSpeed;
		}
		
		x += xDiff;
		z += zDiff;
	}
	
}
