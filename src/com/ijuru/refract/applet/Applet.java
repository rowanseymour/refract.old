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
import java.awt.datatransfer.*;
import javax.swing.*;
import java.security.*;
import java.io.*;
import javax.imageio.*;

import com.ijuru.refract.FractalGenerator;
import com.ijuru.refract.Palette;
import com.ijuru.refract.Utils;

/**
 * Main applet class
 */
public class Applet extends JApplet implements Runnable, ActionListener, FractalPanelListener
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constants
	 */		
	private final String appTitle = "Refract";									// Application title
	private final String appVersion = "1.03 BETA";								// Application version		
	private final int DEF_ZOOM = 100;											// The default zoom value
	
	/**
	 * Components
	 */	
	private FractalPanel manView = null;										// Panel that does the Mandelbrot rendering
	private FractalPanel julView = null;										// Panel that does the Julia rendering
	private FractalPanel selView = null;										// Reference to the selected view
	private JSplitPane splitter = null;											// The splitpane to hold the 2 fractal views
	private StatusPanel status = new StatusPanel(this);			// Panel that shows status info		
	private final JLabel lblDock = new JLabel("Close detached window to reattach", JLabel.CENTER);
	private JFrame frame = null;														// Frame used when applet is detached from web page
		
	private Thread renderThread = null;											// Thread used to render in background
	private boolean threadSuspended = false;								// True if render thread is suspended	
	private boolean isDetached;															// True if applet has been detached
	private boolean hasFileAccess = true;										// True if applet has file write permissions
	
	/**
	 * Menu items
	 */	
	private JMenuItem itemSaveM, itemSaveJ, itemDetach, itemPause;
	private JMenuItem itemCopyCoords, itemResetCoords;
	private JRadioButtonMenuItem itemFuncZ2, itemFuncZ3, itemFuncZ4;
	private JMenuItem itemPalReverse, itemPalAutoScale, itemPalInvert, itemPalSetColor;		
	private JMenuItem[] palItems = new JMenuItem[6];
	private String[] palNames = { "Sunset", "Hubble", "Rainbow", "Chrome", "Evening", "Electric" };	
	private JMenuItem itemControls, itemAbout;
	private JMenuItem[] exampleItems = new JMenuItem[5];
	private JMenuBar menubar = new JMenuBar();
	
	/**
	 * Initializes the applet
	 */
	public void init()
	{		
		initMenu();	
		
		// Initialize the 2 fractal views
		manView = new FractalPanel(FractalGenerator.MANDELBROT, new Palette(Palette.SUNSET));
		manView.setCoords(DEF_ZOOM, 0, 0);	
		manView.addFractalPanelListener(this);
		julView = new FractalPanel(FractalGenerator.JULIA, new Palette(Palette.HUBBLE));	
		julView.setCoords(DEF_ZOOM, 0, 0);
		
		// Start with the mandelbrot view selected
		selView = manView;
		
		// Create a horz split pane with 2 evenly size panes for each fractal view
		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
		splitter.setOneTouchExpandable(true);
		splitter.setResizeWeight(0.5);
		splitter.add(manView, JSplitPane.LEFT);
		splitter.add(julView, JSplitPane.RIGHT);		
		
		// Add listeners which disable the save buttons when a fractal view has 0 pixels
		// and surrenders keyboard focus/selection
		manView.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				boolean hasPixels = (manView.getWidth() > 0 && manView.getHeight() > 0);
				if (!hasPixels) {
					selView = julView;
					selView.requestFocus();
				}
				itemSaveM.setEnabled(hasFileAccess && hasPixels);
			}
		});
		julView.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				boolean hasPixels = (julView.getWidth() > 0 && julView.getHeight() > 0);
				if (!hasPixels) {
					selView = manView;
					selView.requestFocus();
				}				
				itemSaveJ.setEnabled(hasFileAccess && hasPixels);				
			}
		});
		
		// Add focus listeners to change the selected fractal view when one has gained focus
		manView.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e)
			{
				selView = manView;
				status.setSelMode(true);				
			}
		});
		julView.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e)
			{
				selView = julView;
				status.setSelMode(false);								
			}
		});
		
		setLayout(new BorderLayout());							
		setDetached(false);
		checkPermissions();						
	}
	
	/**
	 * Initializes the applet's menu
	 */
	private void initMenu()
	{
		itemSaveM = new JMenuItem("Save M Image");
		itemSaveM.addActionListener(this);
		itemSaveJ = new JMenuItem("Save J Image");
		itemSaveJ.addActionListener(this);		
		itemDetach = new JMenuItem("Detach");
		itemDetach.addActionListener(this);
		//itemPerformance = new JMenuItem("Performance...");
		//itemPerformance.addActionListener(this);
		//itemPerformance.setEnabled(false);									// Need to add dialog box
		itemPause = new JMenuItem("Pause");
		itemPause.addActionListener(this);
			
		ButtonGroup funcGroup = new ButtonGroup();
		itemFuncZ2 = new JRadioButtonMenuItem("z^2 + c", true);
		itemFuncZ2.addActionListener(this);	
		funcGroup.add(itemFuncZ2);	
		itemFuncZ3 = new JRadioButtonMenuItem("z^3 + c");
		itemFuncZ3.addActionListener(this);
		funcGroup.add(itemFuncZ3);
		itemFuncZ4 = new JRadioButtonMenuItem("z^4 + c");
		itemFuncZ4.addActionListener(this);
		funcGroup.add(itemFuncZ4);
				
		JMenu menuFunction = new JMenu("Function");
		menuFunction.add(itemFuncZ2);
		menuFunction.add(itemFuncZ3);
		menuFunction.add(itemFuncZ4);													
			
		itemResetCoords = new JMenuItem("Reset coords");
		itemResetCoords.addActionListener(this);				
		itemCopyCoords = new JMenuItem("Copy coords");
		itemCopyCoords.addActionListener(this);
		
		JMenu menuPalPresets = new JMenu("Presets");		
		for (int i = 0; i < 6; ++i) {
			palItems[i] = new JMenuItem(palNames[i]);
			palItems[i].addActionListener(this);
			menuPalPresets.add(palItems[i]);			
		}
		
		itemPalSetColor = new JMenuItem("Set color...");
		itemPalSetColor.addActionListener(this);
		itemPalInvert = new JMenuItem("Invert");
		itemPalInvert.addActionListener(this);		
		itemPalAutoScale = new JMenuItem("Auto scale");
		itemPalAutoScale.addActionListener(this);
		itemPalReverse = new JMenuItem("Reverse");
		itemPalReverse.addActionListener(this);	
		
		JMenu menuExamples = new JMenu("Examples");
		for (int i = 0; i < 5; ++i) {
			exampleItems[i] = new JMenuItem("Example #" + (i + 1));
			exampleItems[i].addActionListener(this);
			menuExamples.add(exampleItems[i]);			
		}
												
		itemControls = new JMenuItem("Controls...");
		itemControls.addActionListener(this);
		itemAbout = new JMenuItem("About...");
		itemAbout.addActionListener(this);
		
		JMenu menuFile = new JMenu("File");		
		menuFile.add(itemSaveM);
		menuFile.add(itemSaveJ);		
		menuFile.add(new JSeparator());		
		//menuFile.add(itemPerformance);
		menuFile.add(itemDetach);		
		menuFile.add(itemPause);
		JMenu menuFractal = new JMenu("Fractal");
		menuFractal.add(menuFunction);		
		menuFractal.add(itemResetCoords);	
		menuFractal.add(itemCopyCoords);						
		JMenu menuPalette = new JMenu("Palette");		
		menuPalette.add(menuPalPresets);
		menuPalette.add(itemPalSetColor);
		menuPalette.add(itemPalInvert);		
		menuPalette.add(new JSeparator());
		menuPalette.add(itemPalAutoScale);
		menuPalette.add(itemPalReverse);
		JMenu menuHelp = new JMenu("Help");
		menuHelp.add(menuExamples);
		menuHelp.add(new JSeparator());
		menuHelp.add(itemControls);					
		menuHelp.add(itemAbout);
		
		// Add submenus to main menu
		menubar.add(menuFile);
		menubar.add(menuFractal);			
		menubar.add(menuPalette);						
		menubar.add(menuHelp);
	}
	
	/**
	 * Disables menuitems depending on the security restrictions
	 */	 
	private void checkPermissions()
	{
		// Check to see if we have access to the clipboard, and if not, disable the copy menuitem
		try {
			System.getSecurityManager().checkPermission(new AWTPermission("systemClipboard"));
		}
		catch (AccessControlException e) {
			itemCopyCoords.setEnabled(false);
		}
		// Check to see if we have write access to the current directory, and if not, disable the
		// images save menuitems		
		try {
			System.getSecurityManager().checkPermission(new FilePermission(".", "write"));
		}
		catch (AccessControlException e) {
			hasFileAccess = false;
			itemSaveM.setEnabled(false);
			itemSaveJ.setEnabled(false);			
		}
	}
	
	/**
	 * Places main components on either a JApplet or a JFrame
	 */	
	private void placeComponents(Container c)
	{
		c.add(menubar, BorderLayout.NORTH);
		c.add(splitter, BorderLayout.CENTER);
		c.add(status, BorderLayout.SOUTH);		
	}
	
	/**
	 * Switches between attached mode (main components in applet) and detached mode
	 * (main components in a frame).
	 */	
	private void setDetached(boolean detach)
	{
		isDetached = detach;
		
		if (isDetached) {
			// Remove main components from applet						
			Container c = getContentPane();
			c.remove(menubar);
			c.remove(splitter);
			c.remove(status);		
			c.add(lblDock, BorderLayout.CENTER);
			c.validate();
			c.repaint();					
			
			// Create new frame and add main components
			frame = new JFrame(appTitle);
			placeComponents(frame.getContentPane());					
			frame.addWindowListener(
				new WindowAdapter() {
					public void windowClosing(WindowEvent e)
					{
						setDetached(false);
					}	
				}
			);
			frame.setSize(getWidth(), getHeight());		
			frame.setVisible(true);
			
			itemDetach.setText("Dock");			
		}
		else {
			// Destroy frame if it exists
			if (frame != null)
				frame.dispose();
			
			// Place main components back on applet					
			Container c = getContentPane();
			c.remove(lblDock);
			placeComponents(c);			
			c.validate();
			c.repaint();
			
			itemDetach.setText("Detach");
		}
	}
	
	/**
	 * Called after applet is initialized or when user returns to the page containing the applet.
	 */	
	public void start()
	{		
		if (renderThread == null) {
			renderThread = new Thread(this);
			threadSuspended = false;
			renderThread.start();
		}
		else {
			if (threadSuspended) {
				threadSuspended = false;
				synchronized(this) {
					notify();
				}
			}
		}
		itemPause.setText("Pause");
	}
	
	/**
	 * Executed within the thread that this applet created.
	 */
	public void run()
	{
		try {
			while (true) {
				// If a view has been resized, the buffers need reallocated
				if (manView.hasResized())
					manView.initialize();
				if (julView.hasResized())
					julView.initialize();								
				
				manView.render();
				julView.render();
				
				status.setInfo(selView.getGenerator().getMaxIters() + " iters in " + selView.getFrameMillis() + "ms");				
				
				// Now the thread checks to see if it should suspend itself
				if (threadSuspended) {
					synchronized(this) {
						while (threadSuspended) {
							wait();
						}
					}
				}
			}
		}
		catch (InterruptedException e) { }
	}
	
	/**
	 * Called whenever the user leaves the page containing the applet.
	 */
	public void stop()
	{
		threadSuspended = true;
		itemPause.setText("Resume");
	}
	
	/**
	 * Saves the pixel contents of one of the fractal views to an image file
	 */	
	private void saveImage(boolean julia)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new ImageFileFilter());
		int retVal = chooser.showSaveDialog(this);
		if (retVal != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = chooser.getSelectedFile();
		String ext = Utils.getExtension(file);
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(this, "Overwite existing file?", appTitle, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;	
		}
		try {
			RenderedImage image = julia ? julView.createImage() : manView.createImage();
			if (ext.equals("jpg"))
				ImageIO.write(image, "JPEG", file);
			else if (ext.equals("png"))
				ImageIO.write(image, "PNG", file);
			else
				JOptionPane.showMessageDialog(this, "Unrecognized image file extension", "Error", JOptionPane.ERROR_MESSAGE);				
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Unable to save JPEG image", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Invoked when an action occurs
	 * @param		e	The action event object
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		
		if (src == itemSaveM) {
			stop();
			saveImage(false);
			start();
		}
		else if (src == itemSaveJ) {
			stop();			
			saveImage(true);
			start();						
		}		
		else if (src == itemDetach) {
			setDetached(!isDetached);
		}
		/*else if (src == itemPerformance) {
			JDialog dlg = new JDialog();
			dlg.add(new JButton("OK"));
			
			dlg.pack();
			dlg.setTitle("Performance Options");
			//dlg.setParent(this);			
			dlg.setModal(true);
			dlg.setVisible(true);
		}*/	
		else if (src == itemPause) {
			if (threadSuspended)
				start();
			else
				stop();
		}
		else if (src == itemFuncZ2) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(DEF_ZOOM, 0, 0);					
		}
		else if (src == itemFuncZ3) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT_3);
			julView.getGenerator().setFunction(FractalGenerator.JULIA_3);
			setCoords(DEF_ZOOM, 0, 0);					
		}
		else if (src == itemFuncZ4) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT_4);
			julView.getGenerator().setFunction(FractalGenerator.JULIA_4);
			setCoords(DEF_ZOOM, 0, 0);			
		}					
		else if (src == itemResetCoords) {
			setCoords(DEF_ZOOM, 0, 0);				
		}		
		else if (src == itemCopyCoords) {
			try {
				// Copy coords from the mandelbrot view
				FractalGenerator fg = manView.getGenerator();
				String params = fg.getZoom() + "\n" + fg.getXPos() + "\n" + fg.getYPos();
				Clipboard board = getToolkit().getSystemClipboard();
				board.setContents(new StringSelection(params), null);
			}
			catch (Exception ex) {}
		}
		else if (src == palItems[0]) {
			selView.setPalette(Palette.SUNSET);			
		}
		else if (src == palItems[1]) {
			selView.setPalette(Palette.HUBBLE);				
		}
		else if (src == palItems[2]) {
			selView.setPalette(Palette.RAINBOW);				
		}
		else if (src == palItems[3]) {
			selView.setPalette(Palette.CHROME);				
		}
		else if (src == palItems[4]) {
			selView.setPalette(Palette.EVENING);				
		}
		else if (src == palItems[5]) {
			selView.setPalette(Palette.ELECTRIC);				
		}		
		else if (src == itemPalReverse) {
			selView.reversePalette();			
		}
		else if (src == itemPalAutoScale) {
			selView.autoScalePalette();			
		}		
		else if (src == itemPalInvert) {
			selView.invertPalette();			
		}
		else if (src == itemPalSetColor) {
			stop();
			Color color = JColorChooser.showDialog(this, "Set color", selView.getSetColor());
			if (color != null) {
				selView.setSetColor(color);				
			}
			start();		
		}								
		else if (src == exampleItems[0]) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(409680.0429170958, -0.7711496426797392, 0.11529120855296526);
		}
		else if (src == exampleItems[1]) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(1.3312128175744123E10, -0.5644303291616849, -0.6436946946061423);
		}
		else if (src == exampleItems[2]) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(169289.27393268436, -0.1906007280355749, 0.6698834550467907);
		}
		else if (src == exampleItems[3]) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(2.4789605647075914E13, 0.33602211703385265, 0.05478487479148234);
		}
		else if (src == exampleItems[4]) {
			manView.getGenerator().setFunction(FractalGenerator.MANDELBROT);
			julView.getGenerator().setFunction(FractalGenerator.JULIA);
			setCoords(6498792.609450064, -1.1200968970340854, 0.219436264812675);
		}
		else if (src == itemControls) {
			String msg = "Pan: mouse drag or arrow keys\n" +
									 "Zoom: mouse wheel or -/+ keys\n" +
									 "Palette shift: Z/X\n" +
									 "Palette scale: C/V\n" +
									 "Palette invert: I\n" +
									 "Palette reverse: R";									 
			JOptionPane.showMessageDialog(this, msg);
		}					
		else if (src == itemAbout) {
			JOptionPane.showMessageDialog(this, appTitle + " " + appVersion + "\nAuthor: Rowan Seymour\nContact: rowanseymour@hotmail.com");
		}				
	}
	
	/**
	 * Sets the coords of both of fractal views
	 */	
	public void setCoords(double zoom, double xpos, double ypos)
	{
		manView.setCoords(zoom, xpos, ypos);
		julView.setCoords(DEF_ZOOM, 0, 0);				
	}
	
	/**
	 * Called when the zoom/xpos/ypos coords of a FractalPanel are changed
	 */
	public void coordsChanged(FractalPanel panel)
	{
		FractalGenerator fg = panel.getGenerator();
		status.setCoords(fg.getZoom(), fg.getXPos(), fg.getYPos());
		julView.getGenerator().setJuliaCoords(fg.getXPos(), fg.getYPos());
	}	
}