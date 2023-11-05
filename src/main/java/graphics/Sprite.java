package graphics;

public class Sprite {

	public final int[] pixels;
	public final int width, height;
	
	public Sprite(int width, int height, int colour) {
		this.width = width;
		this.height = height;

		this.pixels = new int[width * height];
		for(int i = 0; i < pixels.length; i++) {
			this.pixels[i] = colour;
		}
	}
	
	public Sprite(int width, int height, int[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = new int[width * height];
		for(int i = 0; i < Math.min(this.pixels.length, pixels.length); i++) {
			this.pixels[i] = pixels[i];
		}
	}
	
}
