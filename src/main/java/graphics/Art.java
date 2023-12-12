package graphics;

public class Art {

	// Tiles
	public static final Sprite TILE_GRASS;
	
	// Walls
	public static final Sprite WALL_BRICK;
	
	// Mobs
	public static final Sprite MOB_WOLF_1;
	public static final Sprite MOB_WOLF_2;
	
	public static final Sprite MOB_GHOST;
	
	static {
		final Sprite spritesheet = new Sprite("assets/art/spritesheet.png");
		
		TILE_GRASS = spritesheet.getSubsection(0, 0, 16, 16);
		WALL_BRICK = spritesheet.getSubsection(16, 0, 16, 16);
		MOB_WOLF_1 = spritesheet.getSubsection(0, 16, 32, 64);
		MOB_WOLF_2 = spritesheet.getSubsection(32, 16, 32, 64);
		MOB_GHOST = spritesheet.getSubsection(0, 80, 16, 16);
	}
	
}
