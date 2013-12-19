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

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JOptionPane;
import javax.vecmath.Point3d;

import autoleveller.probe.Probe;

public class Surface {
	private Probe probe;
	private GCodeBreaker segFile;
	public Surface(Probe probe, File outputFile, File inputFile) throws IOException {
		this(probe, new GCodeBreaker(inputFile, (probe.getUnits().equalsIgnoreCase(Probe.MILLIMETERS)) ? 5 : 0.187));
	}

	public Surface(Probe probe, GCodeBreaker segFile) throws IOException {
		this.probe = probe;
		this.segFile = segFile;
	}

	public boolean probeAreaGEJobArea() {
		return probeAreaGEJobArea(probe.getArea(), segFile.getArea());
	}

	public static boolean probeAreaGEJobArea(Rectangle2D probeArea, Rectangle2D millArea) {
		return probeArea.contains(millArea);
	}

	public void writeLeveledFile(PrintWriter lvldFile) {
		try {
			// PrintWriter lvldFile = new NoExponentWriter(new
			// BufferedWriter(new FileWriter(outputFile, false)));
			AutoLeveller.writeHeader(lvldFile, segFile.getOriginalFile().getName());
			// lvldFile.println();
			// probe.writeProbe(lvldFile);
			// lvldFile.println();
			// probe.writeSubs(lvldFile);
			// lvldFile.println();
			writeMillFile(lvldFile, segFile);
			lvldFile.println();
			segFile.close();
			lvldFile.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "File error occured: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private SimplePoint3DCNC writeBilinear(PrintWriter writer, Point3d point) {
		SimplePoint3DCNC currentPoint = SimplePoint3DCNC.point3dToSimplePoint3DCNC(point);
		SimplePoint3DCNC intPointLeft = probe.interpolateY(probe.getBLPoint(currentPoint),
				probe.getTLPoint(currentPoint), currentPoint);
		SimplePoint3DCNC intPointRight = probe.interpolateY(probe.getBRPoint(currentPoint),
				probe.getTRPoint(currentPoint), currentPoint);
		// writer.println("#102=" + intPointLeft.getZ());
		// writer.println("#101=" + intPointRight.getZ());
		intPointLeft.setZ(intPointLeft.getZ());
		intPointRight.setZ(intPointRight.getZ());
		SimplePoint3DCNC intPointHoriz = probe.interpolateX(intPointLeft, intPointRight, currentPoint);
		currentPoint.setZ(intPointHoriz.getZ());
		// writer.println("#100=" + currentPoint.getZ());
		return currentPoint;
	}

	private void writeMillFile(PrintWriter file, GCodeBreaker original) throws IOException {
		String current;
		file.println("(The original mill file is now rewritten with z depth replaced with a)");
		file.println("(bilinear interpolated value based on the initial probing)");
		file.println();

		while ((current = original.readNextLine()) != null) {
			if (original.getCurrentCoords().getZ() < 0) {
				SimplePoint3DCNC bilinear = writeBilinear(file, original.getCurrentCoords());
				String modifiedLine = current;

 				if (original.doesContain(current, 'Z')) {
					modifiedLine = current.replaceAll("Z *" + original.getStringDoubleFromChar(current, 'Z'), "Z"
							+ (bilinear.getZ() + original.getCurrentCoords().getZ()));
				} else if (original.doesContain(current, 'Y')) {
					modifiedLine = current.replaceAll("Y *" + original.getStringDoubleFromChar(current, 'Y'), "Y"
							+ original.getCurrentCoords().getY() + " Z"
							+ (bilinear.getZ() +  original.getCurrentCoords().getZ()));
				} else if (original.doesContain(current, 'X')) {
					modifiedLine = current.replaceAll("X *" + original.getStringDoubleFromChar(current, 'X'), "X"
							+ original.getCurrentCoords().getX() + " Z"
							+ (bilinear.getZ() + original.getCurrentCoords().getZ()));
				}
				modifiedLine.trim();
				file.println(modifiedLine);
			} else {
				file.println(current);
			}
		}
	}


}
