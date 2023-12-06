package utils;

public class Tile {
	
	public static final Tile TILE_WOOD = new Tile(0xff7f7f7f);
	
	public static Tile getTile(int colour) {
		if(colour == 0xff9E631A) return TILE_WOOD;
		return null;
	}
	
	public final int colour;

	public Tile(int colour) {
		this.colour = colour;
	}
	
}
