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
package autoleveller.probe;

import autoleveller.AutoLeveller;
import autoleveller.SimplePoint3DCNC;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public abstract class Probe {
	public static final String MILLIMETERS = "millimeters";
	public static final String INCHES = "inches";

	private static final String LINE_START = "<Probe,MPos:";
	private static final String LINE_WPOS = "WPos:";

	private SimplePoint3DCNC[][] probePoints;

	// probing values
	private String units;
	private Rectangle2D.Double probeArea;
	private double probeFeed;
	private double probeDepth;
	private double finishHeight;
	private double probeClearance;

	protected Probe(String units, double xStart, double yStart, double millWidth, double millHeight, double probeFeed, double probeDepth, double probeSpacing,
		double finishHeight, double probeClearance) {
		this(units, xStart, yStart, millWidth, millHeight, probeFeed, probeDepth, probeSpacing, finishHeight, probeClearance, createProbePoints(xStart, yStart,
			millWidth, millHeight, probeSpacing));
	}

	protected Probe(String units, double xStart, double yStart, double millWidth, double millHeight, double probeFeed, double probeDepth, double probeSpacing,
		double finishHeight, double probeClearance, SimplePoint3DCNC[][] probePoints) {
		setProbeVars(units, xStart, yStart, millWidth, millHeight, probeFeed, probeDepth, finishHeight, probeClearance);
		this.probePoints = probePoints;
		double width = Math.abs(probePoints[probePoints.length - 1][probePoints[0].length - 1].getX() - probePoints[0][0].getX());
		double height = Math.abs(probePoints[probePoints.length - 1][probePoints[0].length - 1].getY() - probePoints[0][0].getY());
		probeArea = new Rectangle2D.Double(probePoints[0][0].getX(), probePoints[0][0].getY(), width, height);
	}

	public void setProbeVars(String units, double xStart, double yStart, double millWidth, double millHeight, double probeFeed, double probeDepth,
		double finishHeight, double probeClearance) {
		this.units = units;
		this.probeFeed = probeFeed;
		this.probeDepth = probeDepth;
		this.finishHeight = finishHeight;
		this.probeClearance = probeClearance;
	}

	public static Probe createProbe(String units, double xStart, double yStart, double millWidth, double millHeight, double probeFeed, double probeDepth,
		double probeSpacing, double finishHeight, double probeClearance) {
		return new LinuxCNCProbe(units, xStart, yStart, millWidth, millHeight, probeFeed, probeDepth, probeSpacing, finishHeight, probeClearance);
	}

	public static Probe createProbe(String units, double xStart, double yStart, double millWidth, double millHeight, double probeFeed, double probeDepth,
		double probeSpacing, double finishHeight, double probeClearance, SimplePoint3DCNC[][] points) {
		return new LinuxCNCProbe(units, xStart, yStart, millWidth, millHeight, probeFeed, probeDepth, probeSpacing, finishHeight, probeClearance, points);
	}

	public String getUnits() {
		return units;
	}

	public SimplePoint3DCNC[][] getProbePoints() {
		return probePoints;
	}

	public Rectangle2D getArea() {
		return probeArea;
	}

	public static SimplePoint3DCNC[][] createProbePoints(double xStart, double yStart, double width, double height, double desiredSpacing) {
		double xSpaces = Math.abs(width / desiredSpacing);
		double ySpaces = Math.abs(height / desiredSpacing);
		// add 1 for start and end points
		int yPoints = ((int) ySpaces) + 1;
		int xPoints = ((int) xSpaces) + 1;

		double xEvenSpacing = width / (int) xSpaces;
		double yEvenSpacing = height / (int) ySpaces;

		SimplePoint3DCNC[][] points = new SimplePoint3DCNC[yPoints][xPoints];

		int zVariable = 500;

		for (int j = 0; j < yPoints; j++) {
			for (int i = 0; i < xPoints; i++) {

				double xPoint = (xStart + (i * xEvenSpacing));
				double yPoint = (yStart + (j * yEvenSpacing));
				points[j][i] = new SimplePoint3DCNC(xPoint, yPoint, zVariable++);
			}

		}
		return points;
	}

	public static SimplePoint3DCNC[][] createProbePoints(File probeFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(probeFile));
		Map<Double, Map<Double, SimplePoint3DCNC>> pointsMap = new HashMap<Double, Map<Double, SimplePoint3DCNC>>();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.indexOf(LINE_START) >= 0) {
				line = line.substring(line.indexOf(LINE_WPOS) + LINE_WPOS.length(), line.indexOf('>', LINE_WPOS.length()));

				String[] strings = line.split(",");
				double x = Double.parseDouble(strings[0]);
				double y = Double.parseDouble(strings[1]);
				double z = Double.parseDouble(strings[2]);
				SimplePoint3DCNC point = new SimplePoint3DCNC(x, y, z);
				Map<Double, SimplePoint3DCNC> pointMap = pointsMap.get(y);
				if (pointMap == null) {
					pointMap = new HashMap<Double, SimplePoint3DCNC>();
					pointsMap.put(y, pointMap);
				}
				pointMap.put(x, point);
			}
		}
		reader.close();
		int xSize = -1;
		for (Map<Double, SimplePoint3DCNC> pointMap : pointsMap.values()) {
			if (xSize == -1) {
				xSize = pointMap.size();
			} else if (xSize != pointMap.size()) {
				System.out.println("Scrued map sizes");
				return null;
			}
		}
		SimplePoint3DCNC[][] points = new SimplePoint3DCNC[pointsMap.size()][xSize];

		int y = 0;
		for (Map<Double, SimplePoint3DCNC> pointMap : pointsMap.values()) {
			int x = 0;
			for (SimplePoint3DCNC point : pointMap.values()) {
				points[y][x] = point;
				x++;
			}
			y++;
		}
		return points;
	}

	public boolean isPointInside(Point2D point) {
		return isPointInside(getArea(), point);
	}

	public static boolean isPointInside(Rectangle2D area, Point2D point) {
		boolean xInside = isBetween(area.getX(), (area.getWidth() + area.getX()), point.getX());
		boolean yInside = isBetween(area.getY(), (area.getHeight() + area.getY()), point.getY());

		return xInside && yInside;
	}

	private static boolean isBetween(double firstNum, double secondNum, double numToTest) {
		double smallest = Math.min(firstNum, secondNum);
		double biggest = Math.max(firstNum, secondNum);

		return ((numToTest >= smallest) && (numToTest <= biggest));
	}

	public void writeProbe(PrintWriter file, String rawFileName) {

		AutoLeveller.writeHeader(file, rawFileName);
		file.println("(prerequisites)");
		file.println("(1. need a working probe)");
		if (units.equals(MILLIMETERS)) {
			file.println("(2. tool needs to be within 10mm of copper board for the 1st probe, )");
			file.println("(i.e. Z0.000 should be no more than 10mm above the board initially)");
		} else {
			file.println("(2. tool needs to be within 3/8\" of copper board for the 1st probe, )");
			file.println("(i.e. Z0.000 should be no more than 3/8\" above the board initially)");
		}
		file.println("(Note: The first probe will touch off Z to 0.000 when it first touches to copper, )");
		file.println("(all other probe values are relative to this first point)");
		file.println();
		if (units.equals(MILLIMETERS)) {
			file.println("G21 (millimeters)");
		} else {
			file.println("G20 (Inches)");
		}
		file.println("G90 (absolute distance mode, not incremental)");
		file.println();
		openLog(file);
		file.println("(begin initial probe and set Z to 0)");
		file.println("G0 X" + probeArea.getX() + " Y" + probeArea.getY() + " Z0");
		if (units.equals(MILLIMETERS)) {
			probeInit(file, "-10");
		} else {
			probeInit(file, "-0.375");
		}
		for (int i = 0; i < probePoints.length; i++) {
			if ((i % 2) == 0) {
				for (int j = 0; j < probePoints[0].length; j++)
					moveNProbe(file, i, j);
			} else {
				for (int j = probePoints[0].length - 1; j >= 0; j--)
					moveNProbe(file, i, j);
			}
		}
		file.println("G0 Z" + probeClearance);
		file.println("G0 X" + probeArea.getX() + " Y" + probeArea.getY() + " Z" + finishHeight);
		// file.println("(Set S value to ensure Speed has a value otherwise the spindle will not start on an M3 command)");
		// file.println("S20000");
		// closeLog(file);
		// file.println();
		// file.println("(The program will pause to allow the probe to be detached)");
		// file.println("(press cycle start to resume from current line)");
		// file.println("M0");
	}

	private void probeInit(PrintWriter writer, String depth) {
		writer.println(probeCommand(depth, String.valueOf(probeFeed)));
		writer.println(zeroZ());
		writer.println("G0 Z" + probeClearance);
		writer.println(probeCommand(String.valueOf(probeDepth), String.valueOf((probeFeed / 2))));
		writer.println(zeroZ());
	}

	private void moveNProbe(PrintWriter writer, int row, int col) {
		writer.println("G0 Z" + probeClearance);
		writer.println("G0 X" + probePoints[row][col].getX() + " Y" + probePoints[row][col].getY());
		writer.println(probeCommand(String.valueOf(probeDepth), String.valueOf(probeFeed)));
		// writer.println(_probePoints[row][col].getZ() + "=" + currentZ());
		logProbePoint(writer, probePoints[row][col]);
	}

	protected abstract String probeCommand(String depth, String feed);

	protected abstract String currentZ();

	protected abstract String zeroZ();

	protected abstract void openLog(PrintWriter writer);

	protected abstract void logProbePoint(PrintWriter writer, SimplePoint3DCNC point);

	protected abstract void closeLog(PrintWriter writer);

	public abstract void writeSubs(PrintWriter writer);

	public SimplePoint3DCNC getTRPoint(SimplePoint3DCNC pointToFind) {
		return getPoint(pointToFind, false, true);
	}

	public SimplePoint3DCNC getTLPoint(SimplePoint3DCNC pointToFind) {
		return getPoint(pointToFind, true, true);
	}

	public SimplePoint3DCNC getBRPoint(SimplePoint3DCNC pointToFind) {
		return getPoint(pointToFind, false, false);
	}

	public SimplePoint3DCNC getBLPoint(SimplePoint3DCNC pointToFind) {
		return getPoint(pointToFind, true, false);
	}

	private SimplePoint3DCNC getPoint(SimplePoint3DCNC pointToFind, boolean isLeft, boolean isTop) {
		SimplePoint3DCNC nearestPoint = getNearestPoint(getProbePoints(), pointToFind.toPoint2D());

		try {
			if (!isPointInside(pointToFind.toPoint2D()))
				return nearestPoint;

			int col = getNearestColumn(getProbePoints(), pointToFind.toPoint2D());
			int row = getNearestRow(getProbePoints(), pointToFind.toPoint2D());

			boolean leftOf = isLeft(nearestPoint, pointToFind.toPoint2D());
			boolean topOf = isTop(nearestPoint, pointToFind.toPoint2D());

			if (leftOf && !isLeft)
				col++;
			else if (!leftOf && isLeft)
				col--;

			if (topOf && !isTop)
				row--;
			else if (!topOf && isTop)
				row++;

			SimplePoint3DCNC returnPoint = getProbePoints()[row][col];
			return returnPoint;
		} catch (ArrayIndexOutOfBoundsException ae) {
			return nearestPoint;
		}

	}

	public static int getNearestColumn(SimplePoint3DCNC[][] points, Point2D point) {
		double distance = (Math.abs(point.getX() - points[0][0].getX()));
		int col = 0;

		for (int i = 1; i < points[0].length; i++) {
			if ((Math.abs(point.getX() - points[0][i].getX())) <= distance) {
				distance = (Math.abs(point.getX() - points[0][i].getX()));
				col = i;
			}
		}
		return col;
	}

	public static int getNearestRow(SimplePoint3DCNC[][] points, Point2D point) {
		double distance = (Math.abs(point.getY() - points[0][0].getY()));
		int row = 0;

		for (int i = 1; i < points.length; i++) {
			if ((Math.abs(point.getY() - points[i][0].getY())) <= distance) {
				distance = (Math.abs(point.getY() - points[i][0].getY()));
				row = i;
			}
		}

		return row;
	}

	public static SimplePoint3DCNC getNearestPoint(SimplePoint3DCNC[][] points, Point2D point) {
		return points[getNearestRow(points, point)][getNearestColumn(points, point)];
	}

	public static boolean isLeft(SimplePoint3DCNC nearestProbePoint, Point2D originalPoint) {
		double distance = originalPoint.getX() - nearestProbePoint.getX();

		// if distance is positive then nearestProbePoint is left of the
		// original point
		return distance > 0;
	}

	public static boolean isTop(SimplePoint3DCNC nearestProbePoint, Point2D originalPoint) {
		double distance = originalPoint.getY() - nearestProbePoint.getY();

		// if distance is positive then nearestProbePoint is bottom of the
		// original point
		return distance < 0;
	}

	private double divZeroTest(double secondCoord, double firstCoord) {
		double addition = 0;
		if (secondCoord - firstCoord == 0) {
			addition += 0.001;
		}
		return addition;
	}

	public SimplePoint3DCNC interpolateY(SimplePoint3DCNC firstPoint, SimplePoint3DCNC secondPoint, SimplePoint3DCNC intPoint) {
		SimplePoint3DCNC returnPoint = new SimplePoint3DCNC(firstPoint.getX(), intPoint.getY(), Double.NaN);

		if (firstPoint.equals(secondPoint))
			returnPoint.setZ(firstPoint.getZ());
		else {
			double addition = divZeroTest(secondPoint.getY(), firstPoint.getY()); // ensures
																					// no
																					// division
																					// by
																					// zero
																					// errors
			double inner = (intPoint.getY() - firstPoint.getY()) / ((secondPoint.getY() + addition) - firstPoint.getY());
			returnPoint.setZ(firstPoint.getZ() + inner * secondPoint.getZ() - inner * firstPoint.getZ());
		}

		return returnPoint;
	}

	public SimplePoint3DCNC interpolateX(SimplePoint3DCNC firstPoint, SimplePoint3DCNC secondPoint, SimplePoint3DCNC intPoint) {
		SimplePoint3DCNC returnPoint = new SimplePoint3DCNC(intPoint.getX(), firstPoint.getY(), Double.NaN);

		if (firstPoint.equals(secondPoint))
			returnPoint.setZ(firstPoint.getZ());
		else {
			double addition = divZeroTest(secondPoint.getX(), firstPoint.getX());
			double inner = (intPoint.getX() - firstPoint.getX()) / ((secondPoint.getX() + addition) - firstPoint.getX());
			returnPoint.setZ(firstPoint.getZ() + inner * secondPoint.getZ() - inner * firstPoint.getZ());
		}

		return returnPoint;
	}
}