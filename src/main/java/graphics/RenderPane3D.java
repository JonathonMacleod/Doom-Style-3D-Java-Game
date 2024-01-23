package graphics;

import utils.Level;

public class RenderPane3D extends RenderPane {

	public final float[] zBuffer;

	public RenderPane3D(int width, int height) {
		super(width, height);		
		zBuffer = new float[width * height];
	}
	
	public void clear(float maxDistance) {
		for(int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xffff00ff;
			zBuffer[i] = maxDistance;
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
	
	public void drawEntity(Level level, Entity entity) {
		final Camera camera = level.player.camera;
		
		// Get the entity position relative to the camera
		final float entityRelativeX = (float) (camera.x - entity.x);
		final float entityRelativeY = (float) (camera.y - entity.y);
		final float entityRelativeZ = (float) (camera.z - entity.z);
		
		// Rotate the entity location around the camera (relative to the camera angle)
		final float relativeEntityX = (float) ((entityRelativeX * Math.cos(-camera.angle)) + (entityRelativeZ * Math.sin(-camera.angle)));
		final float relativeEntityY = entityRelativeY;
		final float relativeEntityZ = (float) -((entityRelativeZ * Math.cos(-camera.angle)) - (entityRelativeX * Math.sin(-camera.angle)));
		
		// Check that the entity is in-front of the camera
		if(relativeEntityZ < camera.minRenderDistance) 
			return;
		
		// Calculate the position of the entity on the screen
		final float screenEntityX = (width / 2.0f) - (relativeEntityX / relativeEntityZ) * (width / 2.0f);
		final float screenEntityY = (height / 2.0f) + (relativeEntityY / relativeEntityZ) * (height / 2.0f);
		
		// Calculate the boundaries of the entity drawn on the screen
		final int pushBackZ = (int) (height / relativeEntityZ * 8 * (entity.sprite.height / 64.0) * entity.scale);
		final int screenEntityLeft = (int) (screenEntityX - pushBackZ);
		final int screenEntityRight = (int) (screenEntityX + pushBackZ);
		final int screenEntityTop = (int) (screenEntityY - pushBackZ);
		final int screenEntityBottom = (int) ((screenEntityY + pushBackZ));
		
		// Iterate through each row of the entity being drawn
		for(int screenY = Math.max(0, screenEntityTop); screenY < Math.min(height, screenEntityBottom); screenY++) {
			final float relativePositionInHeight = (screenY - screenEntityTop) / (1.0f * (screenEntityBottom - screenEntityTop));
			final int textureRow = (int) (relativePositionInHeight * entity.sprite.height);
			
			// Iterate through each column of the entity being drawn
			for(int screenX = Math.max(0, screenEntityLeft); screenX < Math.min(width, screenEntityRight); screenX++) {
				final float relativePositionInWidth = (screenX - screenEntityLeft) / (1.0f * (screenEntityRight - screenEntityLeft));
				final int textureColumn = (int) (relativePositionInWidth * entity.sprite.width);
				
				final int textureIndex = (textureColumn + textureRow * entity.sprite.width);
				final int colour = entity.sprite.pixels[textureIndex];
				
				// Render the sprite, excluding the transparency colours
				if((colour != 0xff7f007f) && (colour != 0xffff00ff)) 
					setPixel(screenX, screenY, relativeEntityZ, colour);
			}
		}
	}
	
	public void applyFog(float maxDistance, int fogColour, float fogStrength) {
		final int fogRed = (fogColour & 0x00ff0000) >> 16;
		final int fogGreen = (fogColour & 0x0000ff00) >> 8;
		final int fogBlue = (fogColour & 0x000000ff);
		
		for(int i = 0; i < pixels.length; i++) {
			final float z = zBuffer[i];

			if(z >= maxDistance) {
				pixels[i] = fogColour;
			} else {
				final int sourceColour = pixels[i];
				
				final int sourceRed = (sourceColour & 0x00ff0000) >> 16;
				final int sourceGreen = (sourceColour & 0x0000ff00) >> 8;
				final int sourceBlue = (sourceColour & 0x000000ff);

				final float fogAlpha = ((maxDistance - z) / maxDistance) * fogStrength;
				
				final int resultRed = (int) ((sourceRed * fogAlpha) + (fogRed * (1.0f - fogAlpha)));
				final int resultGreen = (int) ((sourceGreen * fogAlpha) + (fogGreen * (1.0f - fogAlpha)));
				final int resultBlue = (int) ((sourceBlue * fogAlpha) + (fogBlue * (1.0f - fogAlpha)));
				final int resultArgb = ((255 << 24) | (resultRed << 16) | (resultGreen << 8) | resultBlue);
				
				pixels[i] = resultArgb;
			}
		}
	}
	
}
