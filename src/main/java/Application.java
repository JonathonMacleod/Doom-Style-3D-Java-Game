import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import graphics.Camera;
import graphics.RenderPane3D;
import ui.Window;
import utils.Level;

public class Application {

	private final Window window;
	private final Thread mainGameThread;
	private volatile boolean isGameRunning = false;

	private Level currentLevel;
	private final RenderPane3D renderPane = new RenderPane3D(400, 240);
	
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

		// Now that the game loop should finish, wait for the main game thread to finish
		try {
			mainGameThread.join();
		} catch (InterruptedException e) {
			System.err.println("Failed to wait for game thread to close - " + e.getMessage() + ": " + e.getStackTrace());
		}
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

			nanosecondsSinceLastUpdate += timeSinceLastLoop;
			nanosecondsSinceLastRender += timeSinceLastLoop;
			
			// Update the game logic
			if(nanosecondsSinceLastUpdate >= maxNanosecondsBetweenUpdates) {
				double delta = (nanosecondsSinceLastUpdate / 1000000000.0);
				nanosecondsSinceLastUpdate = 0;
				updateGame(delta);
				currentUps++;
			}
			
			// Render the latest frame
			if(allowUnlimitedFPS || (nanosecondsSinceLastRender >= maxNanosecondsBetweenRenders)) {
				nanosecondsSinceLastRender = 0;
				renderGame();
				currentFps++;
			}
			
			// Each second reset the UPS and FPS counters
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
		// Load the test level
		currentLevel = new Level("test");
	}
	
	private void onShutdown() {
		
	}
	
	private void updateGame(double delta) {
		// Update the level and all entities within it
		currentLevel.update(window.inputHandler, (float) delta);
	}
	
	private void renderGame() {
		// Get the Graphics instance for the JFrame to draw to the hidden buffer
		final BufferStrategy bufferStrategy = window.getBufferStrategy();
		final Graphics graphics = window.getDrawGraphics();
		
		// Clear, draw to, and display the render pane on the canvas draw graphics
		final Camera camera = currentLevel.player.camera;
		if(camera != null) renderPane.clear(camera.maxRenderDistance);
		renderPane.drawLevel(currentLevel);
		graphics.drawImage(renderPane.getBufferedImage(), 0, 0, window.getWidth(), window.getHeight(), null);
		
		// Show the hidden buffer with the new game frame on it
		graphics.dispose();
		bufferStrategy.show();
	}
	
}
