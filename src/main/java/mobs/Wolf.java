package mobs;

import graphics.Art;
import ui.InputHandler;
import utils.Level;

public class Wolf extends Mob {

	public Wolf(Level level, float x, float y, float z) {
		super(level, Art.MOB_WOLF_1, x, y, z);
	}
	
	@Override
	public void update(InputHandler inputHandler, float delta) {
		// Switch between the two sprite options every second
		sprite = System.currentTimeMillis() % 2000 > 1000 ? Art.MOB_WOLF_1 : Art.MOB_WOLF_2;
		
		float xDiff = level.player.camera.x - x;
		float zDiff = level.player.camera.z - z;

		final float maxSpeed = 0.3f;
		final float minSpeed = -0.3f;
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
