

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.Random;

import graphics.Camera;
import graphics.Entity;
import graphics.RenderPane3D;
import graphics.Sprite;
import ui.Window;

public class Application {

	private final int maxEntities = 256;
	private final Entity[] entities = new Entity[maxEntities];
	private final RenderPane3D renderPane = new RenderPane3D(400, 240, new Camera(60.0f), 0.1f, 250.0f);
	
	private final Window window;
	private final Thread mainGameThread;
	private volatile boolean isGameRunning = false;
	
	public Application(String title, int width, int height) {
		window = new Window(title, width, height);

		// Create the main game thread (but don't invoke it)
		mainGameThread = new Thread(() -> {
			System.out.println("Invoking main game thread");
			gameLoop();
			System.out.println("Main game thread has finished");
		}, "Main Game Thread");
	}
	
	public void start() {
		if(isGameRunning)
			return;
		isGameRunning = true;

		// Invoke the main game thread
		mainGameThread.start();
	}
	
	public void stop() {
		if(!isGameRunning)
			return;
		
		isGameRunning = false;
	}
	
	public boolean isGameRunning() { return isGameRunning; }
	
	private void gameLoop() {
		// Before starting the game loop, prepare anything necessary for the main application thread
		onStartup();
		
		int currentFps = 0;
		int currentUps = 0;
		
		long nanosecondsSinceLastRender = 0;
		long nanosecondsSinceLastUpdate = 0;
		long lastTickTime = System.currentTimeMillis();
		long lastLoopTime = System.nanoTime();
		
		boolean allowUnlimitedFPS = true;
		long maxNanosecondsBetweenRenders = 1000000000 / 60;
		long maxNanosecondsBetweenUpdates = 1000000000 / 60;
				
		while(isGameRunning) {
			// Calculate the time in milliseconds since we last ran the game loop
			long currentNanoTime = System.nanoTime();
			long timeSinceLastLoop = (currentNanoTime - lastLoopTime);
			lastLoopTime = currentNanoTime;

			nanosecondsSinceLastRender += timeSinceLastLoop;
			nanosecondsSinceLastUpdate += timeSinceLastLoop;
			
			// Update the game logic
			if(nanosecondsSinceLastUpdate >= maxNanosecondsBetweenUpdates) {
				double delta = (nanosecondsSinceLastUpdate / 1000000000.0);
				nanosecondsSinceLastUpdate -= maxNanosecondsBetweenUpdates;
				updateGame(delta);
				currentUps++;
			}
			
			// Render the latest frame
			if(allowUnlimitedFPS || (nanosecondsSinceLastRender >= maxNanosecondsBetweenRenders)) {
				nanosecondsSinceLastRender -= maxNanosecondsBetweenRenders;
				renderGame();
				currentFps++;
			}
			
			// If a 1000 milliseconds have passed, a second has passed and we can update the 
			long currentMillis = System.currentTimeMillis();
			if((currentMillis - lastTickTime) >= 1000) {
				System.out.println(currentUps + "ups, " + currentFps + "fps!");
				currentFps = currentUps = 0;
				lastTickTime = currentMillis;
			}
		}
		
		// Cleanup anything used for the main application thread
		onShutdown();
	}
	
	private void onStartup() {
		final int RED = 0xffff0000;
		final int GRE = 0xff00ff00;
		final int BLU = 0xff0000ff;
		
		// Create a random sample of sprites for testing
		final Random random = new Random();
		final Sprite sprite = new Sprite(16, 16, new int[] {
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, GRE, GRE, GRE, RED, RED, RED, RED, RED, RED, RED, RED, GRE, GRE, GRE, RED,
			RED, GRE, RED, GRE, RED, RED, RED, RED, RED, RED, RED, RED, GRE, RED, GRE, RED,
			RED, GRE, GRE, GRE, RED, RED, RED, RED, RED, RED, RED, RED, GRE, GRE, GRE, RED,
			RED, GRE, GRE, GRE, RED, RED, RED, RED, RED, RED, RED, RED, GRE, GRE, GRE, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, BLU, BLU, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, BLU, BLU, BLU, BLU, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, BLU, BLU, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
			RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED,
		});
		for(int i = 0; i < maxEntities; i++) {
			final float x = random.nextFloat(-100, 100);
			final float y = random.nextFloat(-100, 100);
			final float z = random.nextFloat(-100, 100);
			final Entity entity = new Entity(x, y, z, sprite);
			entities[i] = entity;
		}
	}
	
	private void onShutdown() {
		
	}
	
	private void updateGame(double delta) {
		int xMovement = 0, zMovement = 0;
		if(window.inputHandler.keyStates[KeyEvent.VK_W]) zMovement -= 1;
		if(window.inputHandler.keyStates[KeyEvent.VK_S]) zMovement += 1;
		if(window.inputHandler.keyStates[KeyEvent.VK_A]) xMovement += 1;
		if(window.inputHandler.keyStates[KeyEvent.VK_D]) xMovement -= 1;

		final float speed = 0.1f;
		renderPane.camera.x += (xMovement * speed);
		renderPane.camera.z += (zMovement * speed);
		
//		renderPane.camera.angle += 0.01 * 2 * Math.PI * delta;
	}
	
	private void renderGame() {
		final BufferStrategy bufferStrategy = window.getBufferStrategy();
		final Graphics graphics = window.getDrawGraphics();
		
		// Clear, draw to, and display the render pane on the canvas draw graphics
		renderPane.clear();
		renderPane.drawFloorAndCeiling(2, 2, 32);
		for(int i = 0; i < maxEntities; i++) {
			renderPane.drawEntity(entities[i]);
		}
		renderPane.applyFog(0xff010401, 1f);
		graphics.drawImage(renderPane.getBufferedImage(), 0, 0, window.getWidth(), window.getHeight(), null);
		
		graphics.dispose();
		bufferStrategy.show();
	}
	
}
