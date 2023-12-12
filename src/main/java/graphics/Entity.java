package graphics;

public class Entity {

	public float x, y, z;
	public Sprite sprite;
	public float scale = 1.0f;
	
	public Entity(float x, float y, float z, Sprite sprite) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.sprite = sprite;
	}
	
	public Entity(float x, float y, float z, Sprite sprite, float scale) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.sprite = sprite;
		this.scale = scale;
	}
	
}
