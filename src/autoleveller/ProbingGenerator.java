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
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import autoleveller.probe.Probe;

public class ProbingGenerator {

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
		options.addOption("f", "feed", true, "Probing feed rate (default 100)");
		options.addOption("d", "depth", true, "Probing depth (default -1mm)");
		options.addOption("c", "clearance", true, "Clearence distanse above the surfice (default 2mm)");
		options.addOption("s", "spacing", true, "Spacing between probing points (default 10mm)");
		options.addOption("height", true, "Finish height (default 20mm)");
		options.addOption("D", "dir", true, "Output directory, if not set output goes on system out");
		OptionGroup og = new OptionGroup();
		og.addOption(OptionBuilder.withDescription("Millimiters, (default)").create('m'));
		og.addOption(OptionBuilder.withDescription("Inches").create('i'));
		options.addOptionGroup(og);

		int probeFeed = 100;
		int probeDepth = -1;
		int probeClearance = 2;
		int probeSpacing = 10;
		int finishHeight = 20;
		String unit = null;
		File gCodeFile = null;
		OutputStream out = System.out;

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
			unit = line.hasOption('i') ? Probe.INCHES : Probe.MILLIMETERS;

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
				System.out.println("Creating probing file at: " + probingFile.getCanonicalPath());
				out = new FileOutputStream(probingFile);
			}

		} catch (ParseException exp) {
			System.out.println("Argument error:" + exp.getMessage());
			helpMessage(options);
			System.exit(0);
		}

		GCodeReader gCodeReader = new GCodeReader(gCodeFile);
		Rectangle2D area = gCodeReader.getArea();
		double x = area.getX();
		double y = area.getY();
		double width = area.getWidth();
		double height = area.getHeight();

		Probe probe = Probe.createProbe(unit, x, y, width, height, probeFeed, probeDepth, probeSpacing, finishHeight, probeClearance);
		PrintWriter file = new PrintWriter(out);
		probe.writeProbe(new NoExponentWriter(file), gCodeFile.getName());
		file.flush();
	}

	private static File createFile(File gCodeFile, File outDir) {
		String gCodeFileName = gCodeFile.getName();
		String name = gCodeFileName;
		String suffix = "";
		int indexOf = gCodeFileName.lastIndexOf('.');
		if(indexOf>0){
			name = gCodeFileName.substring(0, indexOf);
			suffix = gCodeFileName.substring(indexOf);
		}
		
		File probingFile = new File(outDir, name+"-Probing"+suffix );
		int i = 0;
		while(probingFile.exists()){
			i++;
			probingFile = new File(outDir, name+"-Probing_"+i+suffix );
		}
		return probingFile;
	}

	private static void helpMessage(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp(140, "ProbeGenerator [-c <n>] [-D <dir>] [-d <n>] [-f <feed>] [-h] [-height <n>] [-i|-m] [-s <n>] gCodeFile", "",
			options, "foot", false);
	}

}
