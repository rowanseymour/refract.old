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

import java.io.File;
import javax.swing.filechooser.FileFilter;

import com.ijuru.refract.Utils;

/**
 * Simple file filter to allow only JPEG and PNG files
 */
public class ImageFileFilter extends FileFilter
{
	/**
	 * Whether the given file is accepted by this filter. 
	 */
	public boolean accept(File f)
	{
		// Required to display directories
		if (f.isDirectory())
			return true;
			
		String ext = Utils.getExtension(f);
		System.out.println(ext);
		return (ext != null && (ext.equals("jpg") || ext.equals("png")));
	}
	
	/**
	 * The description of this filter. 
	 */
	public String getDescription()
	{
		return "Image Files (.jpg;.png)";
	}
}
