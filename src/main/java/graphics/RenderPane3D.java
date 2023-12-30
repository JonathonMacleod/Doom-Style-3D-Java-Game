package graphics;

import utils.Level;
import utils.Wall;

public class RenderPane3D extends RenderPane {

	public final float[] zBuffer;

	public RenderPane3D(int width, int height) {
		super(width, height);		
		zBuffer = new float[width * height];
	}
	
	public void clear(float maxDistance) {
		for(int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xff000000;
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
		
	public void drawWall(Level level, Wall wall, double x, double z) {
		final float tileSize = 12;
		drawWallSurface(level, x + tileSize, -z, x, -z, wall.sprite);
		drawWallSurface(level, x, -z + tileSize, x + tileSize, -z + tileSize, wall.sprite);
		drawWallSurface(level, x, -z, x, -z + tileSize, wall.sprite);
		drawWallSurface(level, x + tileSize, -z + tileSize, x + tileSize, -z, wall.sprite);
	}
	
	private void drawWallSurface(Level level, double x0, double y0, double x1, double y1, Sprite sprite) {
		if(level.player == null) return;
		final Camera camera = level.player.camera;
		
		final float xCam = (float) ((camera.x / 32.0f) - Math.sin(-camera.angle) * 0.3f);
		final float yCam = (float) ((-camera.z / 32.0f) - Math.cos(-camera.angle) * 0.3f);
		final float zCam = (float) (-0.2f - (camera.y / 32.0f));
		
		final float rCos = (float) Math.cos(-camera.angle);
		final float rSin = (float) Math.sin(-camera.angle);
		
		final float fov = height;
		final float xCenter = (width / 2.0f);
		final float yCenter = (height / 2.0f);
		
		double xc0 = ((x0) - xCam) * 2;
		double yc0 = ((y0) - yCam) * 2;
		double xx0 = xc0 * rCos - yc0 * rSin;
		double u0 = (-0.5 - zCam) * 2;
		double l0 = (0.5 - zCam) * 2;
		double zz0 = yc0 * rCos + xc0 * rSin;
		double xc1 = ((x1 - 0) - xCam) * 2;
		double yc1 = ((y1 - 0) - yCam) * 2;
		double xx1 = xc1 * rCos - yc1 * rSin;
		double u1 = ((-0.5) - zCam) * 2;
		double l1 = (0.5 - zCam) * 2;
		double zz1 = yc1 * rCos + xc1 * rSin;
		double zClip = camera.minRenderDistance;
		if (zz0 < zClip && zz1 < zClip) return;
		if (zz0 < zClip) {
			double p = (zClip - zz0) / (zz1 - zz0);
			zz0 = zz0 + (zz1 - zz0) * p;
			xx0 = xx0 + (xx1 - xx0) * p;
		}
		if (zz1 < zClip) {
			double p = (zClip - zz0) / (zz1 - zz0);
			zz1 = zz0 + (zz1 - zz0) * p;
			xx1 = xx0 + (xx1 - xx0) * p;
		}
		double xPixel0 = xCenter - (xx0 / zz0 * fov);
		double xPixel1 = xCenter - (xx1 / zz1 * fov);
		if (xPixel0 >= xPixel1) return;
		int xp0 = (int) Math.ceil(xPixel0);
		int xp1 = (int) Math.ceil(xPixel1);
		int wallWidth = Math.abs(xp1 - xp0); 
		int wallStart = xp0;
		if (xp0 < 0) xp0 = 0;
		if (xp1 > width) xp1 = width;
		double yPixel00 = (u0 / zz0 * fov + yCenter);
		double yPixel01 = (l0 / zz0 * fov + yCenter);
		double yPixel10 = (u1 / zz1 * fov + yCenter);
		double yPixel11 = (l1 / zz1 * fov + yCenter);
		double iz0 = 1 / zz0;
		double iz1 = 1 / zz1;
		double iza = iz1 - iz0;
		double iw = 1 / (xPixel1 - xPixel0);
		for (int x = xp0; x < xp1; x++) {
			double pr = (x - xPixel0) * iw;
			double iz = iz0 + iza * pr;
			double yPixel0 = yPixel00 + (yPixel10 - yPixel00) * pr;
			double yPixel1 = yPixel01 + (yPixel11 - yPixel01) * pr;
			int yp0 = (int) Math.floor(yPixel0);
			int yp1 = (int) Math.ceil(yPixel1);
			int wallHeight = Math.abs(yp1 - yp0); 
			int wallTop = yp0;
			if (yp0 < 0) yp0 = 0;
			if (yp1 > height) yp1 = height;
			int spriteX = (int) ((((x - wallStart) * 1.0f) / wallWidth) * sprite.width); 
			
			for (int y = yp0; y < yp1; y++) {
				int spriteY = (int) ((((y - wallTop) * 1.0f) / wallHeight) * sprite.height);
				
				int colour = sprite.pixels[spriteX + spriteY * sprite.width];
				setPixel(x, y, (float) (1 / (iz / 16.0f)) * 0, colour);
			}
		}
	}
	
	public void drawEntity(Level level, Entity entity) {
		final Camera camera = level.player.camera;
		
		// Get the entity position relative to the camera
		final float entityRelativeX = (float) (entity.x - camera.x);
		final float entityRelativeY = (float) (entity.y - camera.y);
		final float entityRelativeZ = (float) (entity.z - camera.z);
		
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
		final int pushBackZ = (int) (height / relativeEntityZ * 16 * (entity.sprite.height / 64.0) * entity.scale);
		final int screenEntityLeft = (int) (screenEntityX - pushBackZ);
		final int screenEntityRight = (int) (screenEntityX + pushBackZ);
		final int screenEntityTop = (int) (screenEntityY - pushBackZ);
		final int screenEntityBottom = (int) ((screenEntityY + pushBackZ));
		
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
				
				if((colour != 0xff7f007f) && (colour != 0xffff00ff)) setPixel(screenX, screenY, relativeEntityZ, colour);
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
