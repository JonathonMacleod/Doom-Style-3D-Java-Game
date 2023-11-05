package graphics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.util.Random;

import javax.swing.JFrame;

public class Application extends Canvas {
	private static final long serialVersionUID = 1L;

	private final int maxEntities = 256;
	private final Entity[] entities = new Entity[maxEntities];
	private final RenderPane3D renderPane = new RenderPane3D(800, 480, new Camera(60.0f), 0.1f, 1000.0f);

	private final JFrame jframe;
	private int applicationWidth, applicationHeight;
	
	private final Thread mainGameThread;
	private volatile boolean isGameRunning = false;
	
	public Application(String title, int width, int height) {
		applicationWidth = width;
		applicationHeight = height;
		
		// Set canvas dimensions and create buffer strategy
		setSize(width, height);
		
		// Create a JFrame window
		jframe = new JFrame();
		jframe.add(this);
		jframe.pack();
		jframe.setResizable(false);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setLocationRelativeTo(null);
		jframe.setTitle(title);
		jframe.setVisible(true);

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
			if(nanosecondsSinceLastRender >= maxNanosecondsBetweenRenders) {
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
		// Create a buffer strategy for the canvas that we can draw to
		createBufferStrategy(2);

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
		renderPane.camera.angle += 0.1 * 2 * Math.PI * delta;
	}
	
	private void renderGame() {
		final BufferStrategy bufferStrategy = getBufferStrategy();
		final Graphics graphics = bufferStrategy.getDrawGraphics();
		
		// Clear, draw to, and display the render pane on the canvas draw graphics
		renderPane.clear();
		for(int i = 0; i < maxEntities; i++) {
			renderPane.drawEntity(entities[i]);
		}
		graphics.drawImage(renderPane.getBufferedImage(), 0, 0, applicationWidth, applicationHeight, null);
		
		graphics.dispose();
		bufferStrategy.show();
	}
	
}
