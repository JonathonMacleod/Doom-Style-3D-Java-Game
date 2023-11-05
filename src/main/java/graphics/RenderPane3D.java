package graphics;

public class RenderPane3D extends RenderPane {

	private final float[] zBuffer;
	
	public final Camera camera;
	public final float minRenderDistance, maxRenderDistance;
	
	public RenderPane3D(int width, int height, Camera camera, float minRenderDistance, float maxRenderDistance) {
		super(width, height);
		this.camera = camera;
		this.minRenderDistance = minRenderDistance;
		this.maxRenderDistance = maxRenderDistance;
		
		zBuffer = new float[width * height];
	}
	
	public void clear() {
		for(int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xff000000;
			zBuffer[i] = maxRenderDistance;
		}
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				setPixel(x, y, maxRenderDistance, 0xff);
			}
		}
	}
	
	public void setPixel(int x, int y, int colour) { setPixel(x, y, 0, colour); }
	public void setPixel(int x, int y, float z, int colour) {
		final int pixelIndex = (x + y * width);

		// Check there's nothing already in-front of the pixel before rendering
		if(zBuffer[pixelIndex] <= z)
			return;
		
		zBuffer[pixelIndex] = z;
		pixels[pixelIndex] = colour;
	}
	
	public void drawEntity(Entity entity) {
		// Get the entity position relative to the camera
		final float entityRelativeX = (float) (entity.x - camera.x);
		final float entityRelativeY = (float) (entity.y - camera.y);
		final float entityRelativeZ = (float) (entity.z - camera.z);
		
		// Rotate the entity location around the camera (relative to the camera angle)
		final float relativeEntityX = (float) ((entityRelativeX * Math.cos(camera.angle)) - (entityRelativeZ * Math.sin(camera.angle)));
		final float relativeEntityY = entityRelativeY;
		final float relativeEntityZ = (float) ((entityRelativeZ * Math.cos(camera.angle)) + (entityRelativeX * Math.sin(camera.angle)));
		
		// Check that the entity is in-front of the camera
		if(relativeEntityZ < minRenderDistance) 
			return;
		
		// Calculate the position of the entity on the screen
		final float screenEntityX = (width / 2.0f) - (relativeEntityX / relativeEntityZ * (width / 2.0f));
		final float screenEntityY = (height / 2.0f) + (relativeEntityY / relativeEntityZ) * (height / 2.0f);
		
		// Calculate the boundaries of the entity drawn on the screen
		final int pushBackZ = (int) (height / entityRelativeZ);
		final int screenEntityLeft = (int) (screenEntityX - 16);
		final int screenEntityRight = (int) (screenEntityX + pushBackZ);
		final int screenEntityTop = (int) (screenEntityY - pushBackZ);
		final int screenEntityBottom = (int) (screenEntityY + pushBackZ);
		
		for(int screenY = screenEntityTop; screenY < screenEntityBottom; screenY++) {
			if(screenY < 0)
				continue;
			if(screenY >= height)
				break;
			
			final float relativePositionInHeight = (screenY - screenEntityTop) / (1.0f * (screenEntityBottom - screenEntityTop));
			final int textureRow = (int) (relativePositionInHeight * entity.sprite.height);
			
			for(int screenX = screenEntityLeft; screenX < screenEntityRight; screenX++) {
				if(screenX < 0)
					continue;
				if(screenX >= width)
					break;

				final float relativePositionInWidth = (screenX - screenEntityLeft) / (1.0f * (screenEntityRight - screenEntityLeft));
				final int textureColumn = (int) (relativePositionInWidth * entity.sprite.width);
				
				final int textureIndex = (textureColumn + textureRow * entity.sprite.width);
				final int colour = entity.sprite.pixels[textureIndex];
				setPixel(screenX, screenY, relativeEntityZ, colour);
			}
		}
	}
	
}
