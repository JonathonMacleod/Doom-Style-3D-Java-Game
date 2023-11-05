package graphics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;

public class Application extends Canvas {
	private static final long serialVersionUID = 1L;

	private final RenderPane renderPane = new RenderPane(800, 480);

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
		// Create a buffer strategy for the canvas that we can draw to
		createBufferStrategy(2);
		
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
				nanosecondsSinceLastUpdate -= maxNanosecondsBetweenUpdates;
				double delta = (nanosecondsSinceLastUpdate / 1000000000.0);
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
	}
	
	int x = 0;
	int y = 0;
	private void updateGame(double delta) {		
		x++;
		y++;
		if(x >= renderPane.width) x = 0;
		if(y >= renderPane.height) y = 0;
	}
	
	private void renderGame() {
		final BufferStrategy bufferStrategy = getBufferStrategy();
		final Graphics graphics = bufferStrategy.getDrawGraphics();
		
		renderPane.clear();
		renderPane.setPixel(x, y, 0xffff0000);
		graphics.drawImage(renderPane.getBufferedImage(), 0, 0, applicationWidth, applicationHeight, null);
		
		graphics.dispose();
		bufferStrategy.show();
	}
	
}
