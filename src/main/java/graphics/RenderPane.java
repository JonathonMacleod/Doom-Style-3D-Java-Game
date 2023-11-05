package graphics;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class RenderPane {

	public final int width, height;

	public final int[] pixels;
	public final BufferedImage bufferedImage;
	
	public RenderPane(int width, int height) {
		this.width = width;
		this.height = height;
		
		// Create a buffered image and set the local pixels array to reference the contents of the image
		bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
	}
	
	public void clear() {
		for(int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xff000000;
		}
	}
	
	public void setPixel(int x, int y, int colour) { pixels[x + y * width] = colour; }
	
	public BufferedImage getBufferedImage() { return bufferedImage; }
	
}
