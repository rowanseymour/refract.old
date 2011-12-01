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

package com.ijuru.refract;

/**
 * Class for generating Mandelbrot or Julia sets
 */
public class FractalGenerator
{
	/**
	 * Function type constants
	 */
	public static final int MANDELBROT = 0;
	public static final int MANDELBROT_3 = 1;
	public static final int MANDELBROT_4 = 2;		
	public static final int JULIA = 3;
	public static final int JULIA_3 = 4;
	public static final int JULIA_4 = 5;	
	
	/**
	 * Iteration parameter defaults
	 */
	private static final int DEF_MINITERS = 25;	// The default min/initial iterations value
	private static final int DEF_INCITERS = 10;	// The default iterations increment value	
	
	/**
	 * Iteration parameters
	 */	
	private int minIters = DEF_MINITERS; // Initial number of iterations to perform	
	private int maxIters = minIters; // Max number of iterations to perform
	private int incIters = DEF_INCITERS; // Number of iterations performd per frame
	
	/**
	 * Fractal parameters
	 */	
	private int func = MANDELBROT;	
	private double juliaX, juliaY;	
	private double zoom = 200; // Zoom factor for pixel space -> complex space mapping
	private double xpos = 0; // X(j) offset in complex space
	private double ypos = 0; // Y(i) offset in complex space	
	
	/**
	 * Cache stuff
	 */
	private double[] cacheX = null; // Cache of Real(Z) values
	private double[] cacheY = null; // Cache of Imag(Z) values
	private boolean cacheValid = false; // True if cached values can be used
	
	private int[] iters = null; // Iteration values
	private int width, height; // Dimensions
	
	/**
	 * Initializes this generator to the specified dimensions
	 * @param width the width
	 * @param height the height
	 */	
	public void initialize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		cacheX = null;
		cacheY = null;		
		iters = null;
	
		System.gc(); // Since we potentially released a lot of memory
			
		// Allocate buffers
		cacheX = new double[width * height];
		cacheY = new double[width * height];		
		iters = new int[width * height];
		cacheValid = false;
	}
	
	/**
	 * Updates iteration values by refinement
	 */
	public void update()
	{	
		// Store the parameters as they may be changed in a separate thread during this render	
		double thisZoom = zoom;	
		double thisRe = xpos;
		double thisIm = ypos;
		double thisJulRe = juliaX;
		double thisJulIm = juliaY;
		boolean useCache = cacheValid;
		
		if (useCache) {	
			// We haven't moved, so increase the max iters value for more detail
			maxIters += incIters;					
		}
		else {
			// We have moved, so drop the max iters value to speed up rendering
			maxIters = minIters;
			cacheValid = true;
		}
			
		switch (func) {
			case MANDELBROT:
				iterateZ2(useCache, false, thisZoom, thisRe, thisIm, 0, 0);
				break;	
			case MANDELBROT_3:
				iterateZ3(useCache, false, thisZoom, thisRe, thisIm, 0, 0);
				break;
			case MANDELBROT_4:
				iterateZ4(useCache, false, thisZoom, thisRe, thisIm, 0, 0);
				break;				
			case JULIA:
				iterateZ2(useCache, true, thisZoom, thisRe, thisIm, thisJulRe, thisJulIm);
				break;
			case JULIA_3:
				iterateZ3(useCache, true, thisZoom, thisRe, thisIm, thisJulRe, thisJulIm);
				break;
			case JULIA_4:
				iterateZ4(useCache, true, thisZoom, thisRe, thisIm, thisJulRe, thisJulIm);
				break;																				
		}						
	}
	
	/**
	 * Calculates the standard z = z^2 + c mandelbrot/julia sets
	 */
	private void iterateZ2(boolean useCache, boolean julia, double zoom, double re, double im, double jr, double ji)
	{
		int halfCX = width / 2;
		int halfCY = height / 2;
				
		for (int y = 0, index = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x, ++index) {
				double zr, zi, cr, ci;
				int niters;
				
				// Convert from pixel space to complex space
				cr = (x - halfCX) / zoom + re;
				ci = (y - halfCY) / zoom - im;				
				
				if (useCache) {
					// Load X, Y and ITERS from cache if refinement
					zr = cacheX[index];
					zi = cacheY[index];		
					niters = iters[index];
				}
				else {
					zr = cr;
					zi = ci;
					niters = 0;
				}
				if (julia) {
					cr = jr;
					ci = ji;
				}	
				
				// Precalculate squares
				double zr2 = zr * zr;
				double zi2 = zi * zi;
				
				// Iterate z = z^2 + c
				while ((zr2 + zi2 < 4) && niters < maxIters) {
					zi = 2 * zr * zi + ci;
					zr = zr2 - zi2 + cr;
					zr2 = zr * zr;
					zi2 = zi * zi;
					++niters;
				}
				
				// Store X, Y and ITERS in cache for next frame which maybe a refinement
				cacheX[index] = zr;
				cacheY[index] = zi;		
				iters[index] = niters;
			}
		}
	}
	
	/**
	 * Calculates the z = z^3 + c mandelbrot/julia sets
	 */
	private void iterateZ3(boolean useCache, boolean julia, double zoom, double re, double im, double jr, double ji)
	{
		int halfCX = width / 2;
		int halfCY = height / 2;
				
		for (int y = 0, index = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x, ++index) {
				double zr, zi, cr, ci;
				int niters;
				
				// Convert from pixel space to complex space
				cr = (x - halfCX) / zoom + re;
				ci = (y - halfCY) / zoom - im;				
				
				if (useCache) {
					// Load X, Y and ITERS from cache if refinement
					zr = cacheX[index];
					zi = cacheY[index];		
					niters = iters[index];
				}
				else {
					zr = cr;
					zi = ci;
					niters = 0;
				}
				if (julia) {
					cr = jr;
					ci = ji;
				}	
				
				// Precalculate squares
				double zr2 = zr * zr;
				double zi2 = zi * zi;
				
				// Iterate z = z^3 + c
				while ((zr2 + zi2 < 4) && niters < maxIters) {
					zi = zi * (3 * zr2 - zi2) + ci;
					zr = zr * (zr2 - 3 * zi2) + cr;
					
					zr2 = zr * zr;
					zi2 = zi * zi;
					++niters;
				}
				
				// Store X, Y and ITERS in cache for next frame which maybe a refinement
				cacheX[index] = zr;
				cacheY[index] = zi;		
				iters[index] = niters;
			}
		}
	}
	
	/**
	 * Calculates the z = z^4 + c mandelbrot/julia sets
	 */
	private void iterateZ4(boolean useCache, boolean julia, double zoom, double re, double im, double jr, double ji)
	{
		int halfCX = width / 2;
		int halfCY = height / 2;
				
		for (int y = 0, index = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x, ++index) {
				double zr, zi, cr, ci;
				int niters;
				
				// Convert from pixel space to complex space
				cr = (x - halfCX) / zoom + re;
				ci = (y - halfCY) / zoom - im;				
				
				if (useCache) {
					// Load X, Y and ITERS from cache if refinement
					zr = cacheX[index];
					zi = cacheY[index];		
					niters = iters[index];
				}
				else {
					zr = cr;
					zi = ci;
					niters = 0;
				}
				if (julia) {
					cr = jr;
					ci = ji;
				}	
				
				// Precalculate squares
				double zr2 = zr * zr;
				double zi2 = zi * zi;
				
				// Iterate z = z^4 + c
				while ((zr2 + zi2 < 4) && niters < maxIters) {
					zi = 4 * zr * zi * (zr2 - zi2) + ci;
					zr = zr2 * zr2 - 6 * zr2 * zi2 + zi2 * zi2 + cr;
					
					zr2 = zr * zr;
					zi2 = zi * zi;
					++niters;
				}
				
				// Store X, Y and ITERS in cache for next frame which maybe a refinement
				cacheX[index] = zr;
				cacheY[index] = zi;		
				iters[index] = niters;
			}
		}
	}
	
	/**
	 * Calculates the z = (1 - z)^2 + c mandelbrot/julia sets
	 */
	/*private void iterate1MinZ2(boolean useCache, boolean julia, double zoom, double re, double im, double jr, double ji)
	{
		int halfCX = width / 2;
		int halfCY = height / 2;
				
		for (int y = 0, index = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x, ++index) {
				double zr, zi, cr, ci;
				int niters;
				
				// Convert from pixel space to complex space
				cr = (x - halfCX) / zoom + re;
				ci = (y - halfCY) / zoom - im;				
				
				if (useCache) {
					// Load X, Y and ITERS from cache if refinement
					zr = cacheX[index];
					zi = cacheY[index];		
					niters = iters[index];
				}
				else {
					zr = cr;
					zi = ci;
					niters = 0;
				}
				if (julia) {
					cr = jr;
					ci = ji;
				}	
				
				// Precalculate squares
				double zr2 = zr * zr;
				double zi2 = zi * zi;
				
				//
				while ((zr2 + zi2 < 4) && niters < maxIters) {
					zi = 2 * (1 - zr) * zi + ci;
					zr = (1 - zr) * (1 - zr) - zi2 + cr;
					zr2 = zr * zr;
					zi2 = zi * zi;
					++niters;
				}
				
				// Store X, Y and ITERS in cache for next frame which maybe a refinement
				cacheX[index] = zr;
				cacheY[index] = zi;		
				iters[index] = niters;
			}
		}
	}*/
	
	public int[] calcIterHistogram()
	{
		int[] histo = new int[maxIters + 1];
		int buffsize = width * height;
		for (int i = 0; i < buffsize; ++i)
			++histo[iters[i]];
			
		return histo; 
	}		
	
	/**
	 * Gets the width of the iteration value buffer
	 * @return the width
	 */	
	public int getWidth()
	{
		return width;
	}
	
	/**
	 * Gets the height of the iteration value buffer
	 * @return the height
	 */	
	public int getHeight()
	{
		return height;
	}		
	
	/**
	 * Gets the iteration value buffer
	 */	
	public int[] getIterBuffer()
	{
		return iters;
	}
	
	/**
	 * Gets the maximum iteration value for the last render
	 */
	public int getMaxIters()
	{
		return maxIters;
	}	
	
	/**
	 * Gets the iteration function
	 */		
	public int getFunction()
	{
		return func;
	}
	
	/**
	 * Sets the iteration function
	 */	
	public void setFunction(int func)
	{
		cacheValid = false;
		this.func = func;
	}
	
	/**
	 * Gets the current zoom value
	 */
	public double getZoom()
	{
		return zoom;		
	}
		
	/**
	 * Sets the current zoom value
	 */
	public void setZoom(double zoom)
	{
		cacheValid = false;
		this.zoom = zoom;			
	}	
	
	/**
	 * Gets the current X / Real(Z) position value
	 */	
	public double getXPos()
	{
		return xpos;		
	}
	
	/**
	 * Gets the current Y / Imag(Z) position value
	 */	
	public double getYPos()
	{
		return ypos;		
	}
		
	/**
	 * Sets the coords for fractal generation
	 */
	public void setCoords(double x, double y)
	{
		cacheValid = false;
		this.xpos = x;
		this.ypos = y;		
	}
		
	/**
	 * Sets the iteration parameters that effect performance
	 */	
	public void setIterParams(int minIters, int incIters)
	{
		this.minIters = minIters;
		this.incIters = incIters;							
	}	

	/**
	 * Sets the coords for Julia set generation
	 */
	public void setJuliaCoords(double x, double y)
	{
		cacheValid = false;
		juliaX = x;
		juliaY = y;	
	}	
}
