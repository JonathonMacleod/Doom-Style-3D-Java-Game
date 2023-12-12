package graphics;

public class Art {

	// Tiles
	public static final Sprite TILE_GRASS;
	
	// Walls
	public static final Sprite WALL_BRICK;
	
	// Mobs
	public static final Sprite MOB_WOLF;
	
	static {
		final Sprite spritesheet = new Sprite("assets/art/spritesheet.png");
		
		TILE_GRASS = spritesheet.getSubsection(0, 0, 16, 16);
		WALL_BRICK = spritesheet.getSubsection(16, 0, 16, 16);
		MOB_WOLF = spritesheet.getSubsection(0, 16, 32, 64);
	}
	
}
