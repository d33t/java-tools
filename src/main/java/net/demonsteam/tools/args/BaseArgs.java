/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.args;

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
	private IngoreUnknownParser parser;
	private final HelpFormatter helpFormatter;
	private String[] args;

	public BaseArgs(String toolName, String[] args) {
		this.toolName = toolName;
		this.args = args;
		this.parser = new IngoreUnknownParser();
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

	protected Option createOption(final String longOpt, final String description, final boolean hasArgs, final boolean required) {
		final Option opt = new Option(longOpt.charAt(0) + "", longOpt, hasArgs, (required ? "" : "(optional) ") + description);
		opt.setRequired(required);
		return opt;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
