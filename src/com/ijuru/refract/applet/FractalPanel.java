/**
 * Copyright 2011 Rowan Seymour
 * 
 * This file is part of Refract.
 *
 * Refract is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Refract is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Refract. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ijuru.refract.applet;
 
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import com.ijuru.refract.FractalGenerator;
import com.ijuru.refract.Function;
import com.ijuru.refract.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener class for changes to a FractalPanel
 */
interface FractalPanelListener
{
	/**
	 * Called when the zoom/xpos/ypos coords of a FractalPanel are changed
	 */
	public void coordsChanged(FractalPanel panel);
}

/**
 * Panel for generating and displaying mandel or julia sets
 */
public class FractalPanel extends JPanel implements KeyListener, MouseWheelListener
{		
	private static final long serialVersionUID = 1L;
	
	private int[] buffer = null;								// Buffer of RGB pixels which holds image		
	private MemoryImageSource memImage = null;	// 
	private Image image = null;
	private int width, height;									// The width and height of the fractal image in pixels
	private int setColor = 0x000000;						// Color of pixels within the set (default black)
	
	private FractalGenerator generator = new FractalGenerator();
	
	private long startTime = 0;
	private long frameMillis = 0;								// Time taken to render last frame in millis
	
	private boolean sizeChanged = true;					// True if the size of this component has changed	
	private boolean palChanged = true;					// True if palette has changed
	private boolean palAutoScale = false;	
	
	private final int DEF_PALSIZE = 64;					// The default number of colors in the palette
	private final int MIN_PALSIZE = 8;					// The minimum palette size allowed
	private Palette palette = null;
	private int[] colors = null;
	private int palSize = DEF_PALSIZE;					// The number of colors in the palette
	private int palOffset = 0;									// Offset for iters -> colors mapping
	
	private double oldXPos, oldYPos;						// Used for panning with mouse
	private double oldMouseX, oldMouseY;				// "
	private boolean isBeingDragged = false;			// True when image is being dragged with the mouse
	
	private List<FractalPanelListener> listeners = new ArrayList<FractalPanelListener>();
	
	/**
	 * Constructor
	 */	
	public FractalPanel(Function func, Palette palette)
	{	
		this.palette = palette;
		
		generator.setFunction(func);		
	
		// Set flag for buffer reallocation on resize event
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				sizeChanged = true;
			}
		});
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e)
			{
				isBeingDragged = true;
				
				oldMouseX = e.getX();
				oldMouseY = e.getY();
				oldXPos = generator.getXPos();
				oldYPos = generator.getYPos();
				
				requestFocus();		
			}
			
			public void mouseReleased(MouseEvent e)
			{
				isBeingDragged = false;	
			}			
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e)
			{
				double x = oldXPos + (oldMouseX - e.getX()) / generator.getZoom();
				double y = oldYPos - (oldMouseY - e.getY()) / generator.getZoom();
				setCoords(generator.getZoom(), x, y);			
			}
		});				
		
		setCursor(new Cursor(Cursor.MOVE_CURSOR));
		
		setFocusable(true);
		addKeyListener(this);
		addMouseWheelListener(this);				
	}
	
	public boolean hasResized()
	{
		return sizeChanged;
	}
	
	public void initialize()
	{
		width = getWidth();
		height = getHeight();
		buffer = null;
		
		generator.initialize(width, height);
					
		buffer = new int[width * height];
		memImage = new MemoryImageSource(width, height, Palette.MODEL, buffer, 0, width);
		memImage.setAnimated(true);
		image = createImage(memImage);
	
		sizeChanged = false;	// Clear flag to buffers aren't reallocated until next resize
	}	

	public void paint(Graphics g)
	{
		// Copy the memory image to the screen
		g.drawImage(image, 0, 0, this);								
	}
	
	/**
	 * Overidden to stop background clearing which causes flickering
	 */
	public void update(Graphics g)
	{
		paint(g);
	}	
	
	/**
	 * Executed within the thread that this applet created.
	 */
	public void render()
	{	
		if (startTime == 0)
			startTime = System.currentTimeMillis();
					
		generator.update();
		
		if (palAutoScale) {
			calcAutoScalePalette();
			colors = palette.createInterpolation(palSize);	
			palAutoScale = false;
		}
		else if (palChanged) {
			colors = palette.createInterpolation(palSize);
			palChanged = false;
		}
		
		// Make a positive version of the offset, using the actual size of palette which
		// may have already changed as this function is being called from the render thread
		int curPalSize = colors.length;
		int paloff_safe = palOffset % curPalSize + curPalSize;
		
		// Write pixel colors to buffer
		int[] itersbuf = generator.getIterBuffer();
		int maxIters = generator.getMaxIters();			
		for (int index = 0; index < (width * height); ++index) {
			int iters = itersbuf[index];
			buffer[index] = (iters == maxIters) ? setColor : colors[(iters + paloff_safe) % curPalSize];
		}
		
		// Add central cross hair if mouse is being dragged
		if (isBeingDragged) {
			int crossHairSize = 40;
			int halfW = width / 2;
			int halfH = height / 2;
			int halfC = crossHairSize / 2;			
			for (int y = halfH - halfC; y < halfH + halfC; y += 2)
				buffer[y * width + halfW] = 0xFFFFFFFF;
			for (int x = halfW - halfC; x < halfW + halfC; x += 2)
				buffer[halfH * width + x] = 0xFFFFFFFF;				
		}
		
		memImage.newPixels();		
		repaint();
		
		// Calculate time taken to render this frame
		long endTime = System.currentTimeMillis();
		frameMillis = endTime - startTime;
		startTime = endTime;
	}
	
	/**
	 * Sets the color used to render pixels in the set
	 */		
	public void setSetColor(Color color)
	{
		setColor = color.getRGB();	
	}	
	
	/**
	 * Sets the color used to render pixels in the set
	 */		
	public Color getSetColor()
	{
		return new Color(setColor);	
	}		
	
	/**
	 * Sets the coords for fractal generation
	 */	
	public void setCoords(double zoom, double x, double y)
	{
		generator.setZoom(zoom);		
		generator.setCoords(x, y);
		
		// Notify listeners that coords have changed
		for (int l = 0; l < listeners.size(); l++)
			((FractalPanelListener)listeners.get(l)).coordsChanged(this);							
	}		
	
	public FractalGenerator getGenerator()
	{
		return generator;		
	}
	
	public long getFrameMillis()
	{
		return frameMillis;		
	}
	
	/**
	 * Creates an image suitable for I/O from our memory buffer
	 */
	public RenderedImage createImage()
	{		
		BufferedImage b = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = b.getGraphics();
		g.drawImage(image, 0, 0, null);
		return b;
	}				
	
	/**
	 * Processes keyboard presses
	 */
	public void keyPressed(KeyEvent e)
	{
		double zoom = generator.getZoom();
		double xpos = generator.getXPos();
		double ypos = generator.getYPos();				
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_MINUS:
				setCoords(zoom / 1.02, xpos, ypos);
				break;
			case KeyEvent.VK_EQUALS:
				setCoords(zoom * 1.02, xpos, ypos);
				break;
			case KeyEvent.VK_LEFT:
				setCoords(zoom, xpos - (10 / zoom), ypos);
				break;
			case KeyEvent.VK_RIGHT:
				setCoords(zoom, xpos + (10 / zoom), ypos);
				break;
			case KeyEvent.VK_UP:
				setCoords(zoom, xpos, ypos + (10 / zoom));
				break;
			case KeyEvent.VK_DOWN:
				setCoords(zoom, xpos, ypos - (10 / zoom));
				break;
			case KeyEvent.VK_Z:
				palOffset -= 1;
				break;
			case KeyEvent.VK_X:
				palOffset += 1;
				break;
			case KeyEvent.VK_C:
				setPaletteSize(Math.max(palSize - 1, MIN_PALSIZE));
				break;
			case KeyEvent.VK_V:
				setPaletteSize(palSize + 1);
				break;
			case KeyEvent.VK_I:
				invertPalette();	
				break;
			case KeyEvent.VK_R:
				reversePalette();
				break;																																
		}
	}
	
	public void setPalette(Object[] pairs)
	{
		palette = new Palette(pairs);
		palChanged = true;
	}
	
	public void setPaletteSize(int size)
	{
		palSize = size;
		palChanged = true;
	}		
	
	public int getPaletteSize()
	{
		return palSize;
	}
	
	public void invertPalette()
	{
		palette.invert();
		palChanged = true;		
	}	
	
	/**
	 * Reverses the palette colors
	 */
	public void reversePalette()
	{
		palette.reverse();
		palChanged = true;		
	}
	
	/**
	 * Flags that the palette should be autoscaled in the next render call
	 */	
	public void autoScalePalette()
	{
		palAutoScale = true;				
	}
	
	/**
	 * Creates an autoscaled palette based on the histogram of iteration values
	 */	
	private void calcAutoScalePalette()
	{
		int[] histo = generator.calcIterHistogram();
				
		// Find minimum iteration value for palette start
		int minIVal = 0;
		for (int i = 0; i < histo.length; ++i) {
			if (histo[i] > 0) {
				minIVal = i;
				break;
			}					
		}
		
		// Find value that covers all but top 0.5% of remaining iteration values for palette end			
		int cumulHisto = 0;
		int threshold = (5 * width * height) / 1000;
		int maxIVal = 0;
		// Don't include pixels which are in the set, hence the "-2"
		for (int i = histo.length - 2; i >= 0; --i) {
			cumulHisto += histo[i];
			if (cumulHisto >= threshold) {
				maxIVal = i;
				break;
			}		
		}
		
		palOffset	= -minIVal;			
		palSize	= maxIVal - minIVal;
	}
	
 	public void keyReleased(KeyEvent e) {}
 	public void keyTyped(KeyEvent e) {}
 	
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		double factor = (e.getUnitsToScroll() > 0) ? 1.1 : (1 / 1.1);
		setCoords(generator.getZoom() * factor, generator.getXPos(), generator.getYPos());
	}
	
	public synchronized void addFractalPanelListener(FractalPanelListener listener)
	{
		listeners.add(listener);
	}
}

