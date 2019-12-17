/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.args;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import lombok.Getter;


public class IngoreUnknownParser extends DefaultParser {

	@Getter
	private String[] lastParsedUnknownArgs;

	// Source taken from https://stackoverflow.com/a/53296095/777679
	@Override
	public CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption) throws ParseException {
		if(stopAtNonOption) {
			return parse(options, arguments);
		}
		List<String> knownArguments = new ArrayList<>();
		List<String> unknownArgs = new ArrayList<>();
		boolean nextArgument = false;
		for(String arg: arguments) {
			if(options.hasOption(arg) || nextArgument) {
				knownArguments.add(arg);
			} else {
				unknownArgs.add(arg);
			}

			nextArgument = options.hasOption(arg) && options.getOption(arg).hasArg();
		}
		this.lastParsedUnknownArgs = unknownArgs.toArray(new String[unknownArgs.size()]);
		return super.parse(options, knownArguments.toArray(new String[knownArguments.size()]));
	}
}
