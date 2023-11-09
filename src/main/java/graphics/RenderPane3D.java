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
	
	public void drawFloorAndCeiling(float floorDepth, float ceilingHeight, int tileSize) {
		for(int yPixel = 0; yPixel < height; yPixel++) {
			// Translate the current y pixel position to the bounds -(height / 2) to (height / 2)
			final float relativeScreenY = ((yPixel - (height / 2.0f)) / (height / 2.0f));

			// Since we are in the bounds -(height / 2) to (height / 2), anything positive is the floor, anything negative is the ceiling
			boolean isFloor = (relativeScreenY >= 0);
			float relativeScreenZ = 0;
			if(isFloor) {
				// Find the relative Z position of the pixel once projected by calculating the ceiling position and applying the bounded Y position
				relativeScreenZ = (ceilingHeight * tileSize) / -relativeScreenY;
			} else {
				// Find the relative Z position of the pixel once projected by calculating the floor position and applying the bounded Y position
				relativeScreenZ = (floorDepth * tileSize) / relativeScreenY;
			}
			
			for(int xPixel = 0; xPixel < width; xPixel++) {
				// Translate the current X pixel to -(width / 2) to (width / 2) and project by the Z position calculated earlier
				float relativeScreenX = ((xPixel - (width / 2.0f)) / (width / 2.0f)) * relativeScreenZ;
				
				// Translate the projected position of the current floor/ceiling pixel, and move it to 0 to width and 0 to height bounds
				float worldX = (float) (relativeScreenX * Math.cos(camera.angle) + relativeScreenZ * Math.sin(camera.angle) + (width / 2.0f) * tileSize) + camera.x;
				float worldY = (float) (relativeScreenZ * Math.cos(camera.angle) - relativeScreenX * Math.sin(camera.angle) + (height / 2.0f) * tileSize) + camera.z;
				
				int textureCol = (int) (worldX % tileSize);
				int textureRow = (int) (worldY % tileSize);
				
				int red = (int) (255 * textureCol / tileSize);
				int green = isFloor ? (int) (255 * textureRow / tileSize) : 0;
				int blue = isFloor ? 0 : (int) (255 * textureRow / tileSize);
				
				int colour = (255 << 24 | red << 16 | green << 8 | blue);
				setPixel(xPixel, yPixel, -relativeScreenZ, colour);
			}
		}
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
		final int screenEntityLeft = (int) (screenEntityX - pushBackZ);
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
	
	public void applyFog(int fogColour, float fogStrength) {
		final int fogRed = (fogColour & 0x00ff0000) >> 16;
		final int fogGreen = (fogColour & 0x0000ff00) >> 8;
		final int fogBlue = (fogColour & 0x000000ff);
		
		for(int i = 0; i < pixels.length; i++) {
			final float z = zBuffer[i];
						
			if(z >= maxRenderDistance) {
				pixels[i] = fogColour;
			} else {
				final int sourceColour = pixels[i];
				
				final int sourceRed = (sourceColour & 0x00ff0000) >> 16;
				final int sourceGreen = (sourceColour & 0x0000ff00) >> 8;
				final int sourceBlue = (sourceColour & 0x000000ff);
				
				final float fogAlpha = (z / maxRenderDistance) * fogStrength;
				
				final int resultRed = (int) ((sourceRed * (1.0f - fogAlpha)) + (fogRed * fogAlpha));
				final int resultGreen = (int) ((sourceGreen * (1.0f - fogAlpha)) + (fogGreen * fogAlpha));
				final int resultBlue = (int) ((sourceBlue * (1.0f - fogAlpha)) + (fogBlue * fogAlpha));
				final int resultArgb = ((255 << 24) | (resultRed << 16) | (resultGreen << 8) | resultBlue);
				
				pixels[i] = resultArgb;
			}
		}
	}
	
}
