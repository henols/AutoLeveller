/*  	GRBL AutoLeveller (https://github.com/henols/GrblAutoLeveller) is a stand-alone PC application written in Java which is designed
 *  	to measure precisely the height of the material to be milled / etched in several places,
 *  	then use the information gathered to make adjustments to the Z height
 *  	during the milling / etching process so that a more consistent and accurate result can be achieved. 
 *   
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import autoleveller.probe.Probe;

public class GCodeLeveller {

	/**
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption("h", "help", false, "This message");

		options.addOption(OptionBuilder.withDescription("Probing file").withArgName("probe").hasArg().isRequired().create('p'));
		options.addOption(OptionBuilder.withDescription("Output directory, if not set output goes on system out").hasArg().withArgName("dir").create('D'));

		int probeFeed = 100;
		int probeDepth = -1;
		int probeClearance = 2;
		int probeSpacing = 10;
		int finishHeight = 20;
		File probeFile = null;
		File gCodeFile = null;
		OutputStream out = System.out;

		for (String arg : args) {
			if (arg.endsWith("-h")) {
				helpMessage(options);
				System.exit(0);
			}
		}

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				helpMessage(options);
				System.exit(0);
			}

			probeFeed = Integer.parseInt(line.getOptionValue('f', "100"));
			probeDepth = Integer.parseInt(line.getOptionValue('d', "-1"));
			probeClearance = Integer.parseInt(line.getOptionValue('c', "2"));
			probeSpacing = Integer.parseInt(line.getOptionValue('s', "10"));
			finishHeight = Integer.parseInt(line.getOptionValue("height", "20"));
			probeFile = new File(line.getOptionValue("p"));

			@SuppressWarnings("rawtypes")
			List argList = line.getArgList();
			if (argList.size() == 0) {
				System.out.println("Argument error: gCodeFile must be specified");
				helpMessage(options);
				System.exit(0);
			}
			if (argList.size() > 1) {
				System.out.println("Argument error: only one gCodeFile can be specified");
				helpMessage(options);
				System.exit(0);
			}
			gCodeFile = new File((String) argList.get(0));

			if (line.hasOption('D')) {
				File outDir = new File(line.getOptionValue('D'));
				if (!outDir.isDirectory()) {
					System.out.println("Argument error: directory '" + line.getOptionValue('D') + "' does not exist");
					helpMessage(options);
					System.exit(0);
				}

				File probingFile = createFile(gCodeFile, outDir);
				System.out.println("Creating levelled file at: " + probingFile.getCanonicalPath());
				out = new FileOutputStream(probingFile);
			}

		} catch (ParseException exp) {
			System.out.println("Argument error:" + exp.getMessage());
			helpMessage(options);
			System.exit(0);
		}

		GCodeBreaker gCodeBreaker = new GCodeBreaker(gCodeFile, 5);
		Rectangle2D area = gCodeBreaker.getArea();
		double x = area.getX();
		double y = area.getY();
		double width = area.getWidth();
		double height = area.getHeight();
		SimplePoint3DCNC[][] points = Probe.createProbePoints(probeFile);

		Probe probe = Probe.createProbe(Probe.MILLIMETERS, x, y, width, height, probeFeed, probeDepth, probeSpacing, finishHeight, probeClearance, points);
		Surface surface = new Surface(probe, gCodeBreaker);
		surface.writeLeveledFile(new NoExponentWriter(new BufferedWriter(new PrintWriter(out))));

	}

	private static File createFile(File gCodeFile, File outDir) {
		String gCodeFileName = gCodeFile.getName();
		String name = gCodeFileName;
		String suffix = "";
		int indexOf = gCodeFileName.lastIndexOf('.');
		if (indexOf > 0) {
			name = gCodeFileName.substring(0, indexOf);
			suffix = gCodeFileName.substring(indexOf);
		}

		File probingFile = new File(outDir, name + "-Levelled" + suffix);
		int i = 0;
		while (probingFile.exists()) {
			i++;
			probingFile = new File(outDir, name + "-Levelled_" + i + suffix);
		}
		return probingFile;
	}

	private static void helpMessage(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp(140, "GCodeLeveller [-D <dir>]  [-h] -p <probeFile> gCodeFile", "", options, "", true);
	}

}
