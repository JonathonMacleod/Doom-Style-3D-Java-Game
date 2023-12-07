package utils;

import graphics.Sprite;

public class Tile {
	
	public static final Tile TILE_WOOD = new Tile(new Sprite("assets/art/floor.png"));
	
	public static Tile getTile(int colour) {
		if(colour == 0xff9E631A) return TILE_WOOD;
		return null;
	}
	
	public final Sprite sprite;

	public Tile(Sprite sprite) {
		this.sprite = sprite;
	}
	
}
