package graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Sprite {

	public final int[] pixels;
	public final int width, height;
	
	public Sprite(String path) {
		int[] pixelResults = null;
		int widthResult = 0;
		int heightResult = 0;
		
		try {
			final BufferedImage image = ImageIO.read(new File(path));			
			widthResult = image.getWidth();
			heightResult = image.getHeight();
			pixelResults = new int[widthResult * heightResult];
			for(int y = 0; y < heightResult; y++) {
				for(int x = 0; x < widthResult; x++) {
					pixelResults[x + y * widthResult] = image.getRGB(x, y);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		pixels = pixelResults;
		width = widthResult;
		height = heightResult;
	}
	
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
