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

import java.awt.geom.Point2D;

import javax.vecmath.Point3d;

public class SimplePoint3DCNC {
	private double x;
	private double y;
	private double z;

	public SimplePoint3DCNC(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public SimplePoint3DCNC() {
		this(0.0, 0.0, 0.0);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other == this) {
			return true;
		}
		if (!(other instanceof SimplePoint3DCNC)) {
			return false;
		}
		SimplePoint3DCNC clazz = (SimplePoint3DCNC) other;
		return ((this.getX() == clazz.getX()) && (this.getY() == clazz.getY()) && (this.getZ() == clazz.getZ()));
	}

	public Point2D toPoint2D() {
		return new Point2D.Double(this.getX(), this.getY());
	}

	public static SimplePoint3DCNC point3dToSimplePoint3DCNC(Point3d point) {
		return new SimplePoint3DCNC(point.getX(), point.getY(), point.getZ());
	}

	@Override
	public String toString() {
		return "x = " + x + " y = " + y + " z = " + z;
	}
}
