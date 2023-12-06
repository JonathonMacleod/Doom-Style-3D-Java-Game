package utils;

public class Wall {

	public static final Wall WALL_STONE = new Wall(0xff006600);
	
	public static Wall getWall(int colour) {
		if(colour == 0xff7F7F7F) return WALL_STONE;
		return null;
	}
	
	public final int colour;

	public Wall(int colour) {
		this.colour = colour;
	}
	
}
