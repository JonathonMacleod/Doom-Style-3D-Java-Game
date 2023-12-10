package utils;

import graphics.Sprite;

public class Tile {
	
	public static final Tile TILE_WOOD = new Tile(new Sprite("assets/art/floor.png"));
	public static final Tile TILE_CEIL = new Tile(new Sprite("assets/level/tile_map.png"));
	
	public static Tile getFloorTile(int colour) {
		if(colour == 0xff9E631A) return TILE_WOOD;
		return null;
	}
	
	public static Tile getCeilingTile(int colour) {
		if(colour == 0xff9E631A) return TILE_CEIL;
		return null;		
	}
	
	public final Sprite sprite;

	public Tile(Sprite sprite) {
		this.sprite = sprite;
	}
	
}
