package utils;

import graphics.Art;
import graphics.Sprite;

public class Tile {
	
	public static final Tile TILE_GRASS = new Tile(Art.TILE_GRASS);
	
	public static Tile getFloorTile(int colour) {
		if(colour == 0xff9E631A) return TILE_GRASS;
		return null;
	}
	
	public static Tile getCeilingTile(int colour) {
		if(colour == 0xff9E631A) return TILE_GRASS;
		return null;
	}
	
	public final Sprite sprite;

	public Tile(Sprite sprite) {
		this.sprite = sprite;
	}
	
}
