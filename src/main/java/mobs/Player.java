package mobs;

import java.awt.event.KeyEvent;

import graphics.Camera;
import graphics.Sprite;
import ui.InputHandler;
import utils.Level;

public class Player extends Mob {

	public final Camera camera;
	
	public Player(Level level, Sprite sprite, float x, float y, float z) {
		super(level, sprite, x, y, z);
		camera = new Camera(60.0f, 0.1f, 250.0f);
	}

	@Override
	public void update(InputHandler inputHandler, float delta) {
		// Apply rotation if the LEFT or RIGHT arrow keys are pressed
		final float rotationSpeed = (float) ((2 * Math.PI) * 0.65f * delta);
		if(inputHandler.keyStates[KeyEvent.VK_LEFT]) camera.angle -= rotationSpeed;
		if(inputHandler.keyStates[KeyEvent.VK_RIGHT]) camera.angle += rotationSpeed;

		// Work out the movement speed of the player (e.g. are they sprinting via the SHIFT key)
		float movementSpeed = 48f;
		if(inputHandler.keyStates[KeyEvent.VK_SHIFT]) movementSpeed = 92f;
		
		// Apply movement if the WASD keys are pressed
		float xMovement = 0, zMovement = 0;
		if(inputHandler.keyStates[KeyEvent.VK_W]) zMovement -= 1;
		if(inputHandler.keyStates[KeyEvent.VK_S]) zMovement += 1;
		if(inputHandler.keyStates[KeyEvent.VK_A]) xMovement += 1;
		if(inputHandler.keyStates[KeyEvent.VK_D]) xMovement -= 1;
		final float speed = (float) (movementSpeed * delta);
		
		// If no movement is requested then stop processing to avoid dividing by 0 when normalising
		if((xMovement != 0) || (zMovement != 0)) {
			float magnitude = (float) Math.sqrt((xMovement * xMovement) + (zMovement * zMovement));
			
			xMovement /= magnitude;
			zMovement /= magnitude;
			
			final float xDelta = xMovement * speed;
			final float zDelta = zMovement * speed;
			camera.applyAxisMovements(level, xDelta, 0, zDelta);
		}
		
		// Teleport the player to the center of the level if the player presses the X key
		if(inputHandler.keyStates[KeyEvent.VK_X]) {
			level.resetPlayer();
		}
	}

}
