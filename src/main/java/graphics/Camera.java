package graphics;

public class Camera {

	public final float fovRadians;
	public float x, y, z, angle;
	
	public Camera(float fovDegrees) {
		fovRadians = (float) Math.toRadians(fovDegrees);
		
		x = 80;
		y = 0;
		z = 48;
		angle = (float) Math.PI;
	}
	
	public void applyAxisMovements(float xDelta, float yDelta, float zDelta) {
		x += (xDelta * Math.cos(angle) + zDelta * Math.sin(angle));
		y += yDelta;
		z += (zDelta * Math.cos(angle) - xDelta * Math.sin(angle));
	}
	
}
