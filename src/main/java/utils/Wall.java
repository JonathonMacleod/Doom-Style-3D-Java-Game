package utils;

import graphics.Art;
import graphics.Sprite;

public class Wall {

	public static final Wall WALL_STONE = new Wall(Art.WALL_BRICK);
	
	public static Wall getWall(int colour) {
		if(colour == 0xff7F7F7F) return WALL_STONE;
		return null;
	}
	
	public final Sprite sprite;

	public Wall(Sprite sprite) {
		this.sprite = sprite;
	}
	
}
