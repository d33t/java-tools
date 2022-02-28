/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.args;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;

/**
 * Base class for parsing command line args
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 16 Dec 2019
 */
public abstract class BaseArgs {

	private final String toolName;
	@Getter
	private IgnoreUnknownParser parser;
	private final HelpFormatter helpFormatter;
	private String[] args;
	private List<String> shortOptions = new ArrayList<>();
	
	protected BaseArgs(String toolName, String[] args) {
		this.toolName = toolName;
		this.args = args;
		this.parser = new IgnoreUnknownParser();
		this.helpFormatter = new HelpFormatter();
	}

	public abstract void postInit();

	public abstract Options getCmdOptions();

	public CommandLine getCommandLine() {
		CommandLine cmd = null;
		try {
			cmd = this.parser.parse(getCmdOptions(), this.args, false);
		} catch(final ParseException e) {
			System.out.println(e.getMessage());
			printUsageAndExit();
		}
		return cmd;
	}

	public void printUsageAndExit() {
		this.helpFormatter.printHelp(this.toolName, getCmdOptions());
		System.exit(-1);
	}

	protected Option createOption(final String shortOpt, final String longOpt, final String description, final boolean hasArgs, final boolean required) {
		final Option opt = new Option(shortOpt, longOpt, hasArgs, (required ? "" : "(optional) ") + description);
		opt.setRequired(required);
		return opt;
	}
	
	protected Option createOption(final String longOpt, final String description, final boolean hasArgs, final boolean required) {
		String shortOpt = computeShortOpt(longOpt);
		return createOption(shortOpt, longOpt, description, hasArgs, required);
	}
	
	protected String computeShortOpt(String longOpt) {
		StringBuilder soBuilder = new StringBuilder();
		for(int i = 0; i < longOpt.length(); i++) {
			soBuilder.append(longOpt.charAt(i));
			if(!shortOptions.contains(soBuilder.toString())) {
				break;
			}
		}
		String shortOption = soBuilder.toString();
		shortOptions.add(shortOption);
		return shortOption;
	}
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
