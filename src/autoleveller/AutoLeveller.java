/*  	GRBL AutoLeveller (https://github.com/henols/GrblAutoLeveller) is a stand-alone PC application written in Java which is designed
 *  	to measure precisely the height of the material to be milled / etched in several places,
 *  	then use the information gathered to make adjustments to the Z height
 *  	during the milling / etching process so that a more consistent and accurate result can be achieved. 
 *   
 *   	Copyright (C) 2013 James Hawthorne PhD, daedelus1982@gmail.com
 *   	Copyright (C) 2013 Henrik Olsson, henols@gmail.com
 *
 *   	This program is free software; you can redistribute it and/or modify
 *   	it under the terms of the GNU General Public License as published by
 *   	the Free Software Foundation; either version 2 of the License, or
 *   	(at your option) any later version.
 *
 *   	This program is distributed in the hope that it will be useful,
 *   	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   	GNU General Public License for more details.
 *
 *   	You should have received a copy of the GNU General Public License along
 *   	with this program; if not, see http://www.gnu.org/licenses/
 */

package autoleveller;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AutoLeveller {
	public static final int MAJOR = 0;
	public static final double MINOR = 7.7;
	public static final String VERSION = MAJOR + "." + MINOR;

	public static void writeHeader(PrintWriter file,String rawFileName) {
		DateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat time = new SimpleDateFormat("HH:mm");
		Date rawDate = new Date();

		file.println("(GRBL AutoLeveller, Version: " + AutoLeveller.VERSION
				+ ", https://github.com/henols/GrblAutoLeveller)");
		file.println("(Copyright 2013 Henrik Olsson)");
		file.println("(Original file: " + rawFileName + ")");
		file.println("(Creation date: " + date.format(rawDate) + " time: " + time.format(rawDate) + ")");
		file.println();
		file.println("(This program and any of its output is licensed under GPLv2 and as such...)");
		file.println("(GRBL AutoLeveller comes with ABSOLUTELY NO WARRANTY; for details, see sections 11 and 12 of the GPLv2)");
		file.println();

	}

}
