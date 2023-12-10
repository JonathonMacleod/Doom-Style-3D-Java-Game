package mobs;

import graphics.Entity;
import graphics.Sprite;
import ui.InputHandler;
import utils.Level;

public abstract class Mob extends Entity {

	public final Level level;
	
	public Mob(Level level, Sprite sprite, float x, float y, float z) {
		super(x, y, z, sprite);
		this.level = level;
	}

	public abstract void update(InputHandler inputHandler, float delta);
	
}
