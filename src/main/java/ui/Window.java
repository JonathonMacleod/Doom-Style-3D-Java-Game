package ui;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;

public class Window extends Canvas {
	private static final long serialVersionUID = 1L;

	private final JFrame jframe;
	public final InputHandler inputHandler;
	
	public Window(String title, int width, int height) {
		inputHandler = new InputHandler();
		addKeyListener(inputHandler);
		addMouseListener(inputHandler);
		addMouseMotionListener(inputHandler);
		
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
	}
	
	@Override
	public BufferStrategy getBufferStrategy() { 
		final BufferStrategy initialBufferStrategy = super.getBufferStrategy(); 
		if(initialBufferStrategy == null) {
			super.createBufferStrategy(2);
			return super.getBufferStrategy();
		}
		return initialBufferStrategy;
	}
	public Graphics getDrawGraphics() { return getBufferStrategy().getDrawGraphics(); }
	
}
