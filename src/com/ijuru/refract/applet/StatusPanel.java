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
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * The status bar component on the applet
 */
public class StatusPanel extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private Applet parent = null;
	
	private JLabel selText = new JLabel("M");	
	private JTextField editZoom = new JTextField(7);
	private JTextField editXPos = new JTextField(7);	
	private JTextField editYPos = new JTextField(7);
	private JLabel infoText = new JLabel("");
		
	public StatusPanel(Applet parent)
	{
		this.parent = parent;
		
		selText.setPreferredSize(new Dimension(18, 18));		
		selText.setMinimumSize(new Dimension(18, 18));
		selText.setOpaque(true);
		selText.setForeground(Color.black);				
		selText.setBackground(Color.white);
		selText.setBorder(new LineBorder(Color.black));
		selText.setHorizontalAlignment(SwingConstants.CENTER);
		selText.setVerticalAlignment(SwingConstants.CENTER);			
		
		editZoom.addActionListener(this);
		editXPos.addActionListener(this);	
		editYPos.addActionListener(this);			
		
		JPanel coordsPanel = new JPanel();
		coordsPanel.add(selText);		
		coordsPanel.add(new JLabel("Zoom"));
		coordsPanel.add(editZoom);
		coordsPanel.add(new JLabel("Re"));
		coordsPanel.add(editXPos);
		coordsPanel.add(new JLabel("Im"));
		coordsPanel.add(editYPos);
		
		setLayout(new BorderLayout());
		add(coordsPanel, BorderLayout.WEST);
		add(infoText, BorderLayout.EAST);												
	}
	
	public void setSelMode(boolean mandelbrot)
	{
		selText.setText(mandelbrot ? "M" : "J");
	}
	
	public void setCoords(double zoom, double x, double y)
	{
		editZoom.setText("" + zoom);
		editXPos.setText("" + x);
		editYPos.setText("" + y);
		
		editZoom.setCaretPosition(0);
		editXPos.setCaretPosition(0);		
		editYPos.setCaretPosition(0);		
	}
	
	public void setInfo(String info)
	{
		infoText.setText(info + " ");
	}
		
	public double getZoom()
	{
		return Double.parseDouble(editZoom.getText());
	}
	
	public double getXPos()
	{
		return Double.parseDouble(editXPos.getText());
	}
	
	public double getYPos()
	{
		return Double.parseDouble(editYPos.getText());
	}

	/**
	 * Invoked when an action occurs
	 * @param		e	The action event object
	 */
	public void actionPerformed(ActionEvent e)
	{
		parent.setCoords(getZoom(), getXPos(), getYPos());			
	}
}
