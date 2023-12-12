package mobs;

import graphics.Art;
import ui.InputHandler;
import utils.Level;

public class Wolf extends Mob {

	public Wolf(Level level, float x, float y, float z) {
		super(level, Art.MOB_WOLF, x, y, z);
	}
	
	@Override
	public void update(InputHandler inputHandler, float delta) {
		
	}
	
}
