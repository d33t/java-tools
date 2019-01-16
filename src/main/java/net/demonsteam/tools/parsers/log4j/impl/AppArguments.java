/* Copyright (C) <2018> <d33t>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE. */
package net.demonsteam.tools.parsers.log4j.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * This class is responsible for parsing, transforming and holding the application arguments
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public class AppArguments {

	public static final String DEFAULT_LOG_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss.SSSS";
	public static final String DEFAULT_VALUE_OPT_SORT = "date";

	private static final String OPT_DATE_FORMAT = "dateFormat";
	private static final String OPT_LOG_LEVEL = "loglevel";
	private static final String OPT_USER_PATTERN = "pattern";
	private static final String OPT_INPUT_SOURCE_PATH = "inputFile";
	private static final String OPT_OUTPUT_FILE_PATH = "outputFile";
	private static final String OPT_SORT = "sort";
	private static final String FLAG_UNIQUE = "unique";

	private List<LogLevel> logLevels = new ArrayList<>();
	private String regexLogLevels = StringUtils.EMPTY;
	private String optUserPattern;
	private String optInputSourcePath;
	private File inputFile;
	private String optOutputFilePath;
	private String optSort;
	private boolean flagUnique;
	private String logDateFormat;
	private File tempDir;

	public AppArguments(final String[] args) throws IllegalArgumentException {
		final Options cmdOptions = new Options();
		cmdOptions.addOption(createOption(OPT_DATE_FORMAT, "Specify the log format of the log entries. Defaults to: " + DEFAULT_LOG_DATE_FORMAT, true, false));
		cmdOptions.addOption(createOption(OPT_LOG_LEVEL, "A valid log4J log level: " + Arrays.toString(LogLevel.values()) + ". Multiple values can be separated by comma or space.", true, true));
		cmdOptions.addOption(createOption(OPT_USER_PATTERN, "Pattern to match", true, false));
		cmdOptions.addOption(createOption(OPT_INPUT_SOURCE_PATH, "Absolute or relative to the current directory path to the logfile (text or zip)", true, true));
		cmdOptions.addOption(createOption(OPT_OUTPUT_FILE_PATH, "Absolute or relative to the current directory path to the output file. If omitted the standard output is used.", true, false));
		cmdOptions.addOption(createOption(FLAG_UNIQUE, "Unique lines with occurrence count", false, false));
		cmdOptions.addOption(createOption(OPT_SORT, "Sort either by date or unique count. This option is only used when '" + FLAG_UNIQUE + "' flag is set. Default to date.", true, false));

		final CommandLineParser parser = new DefaultParser();
		final HelpFormatter helpFormatter = new HelpFormatter();
		try {
			final CommandLine cmd = parser.parse(cmdOptions, args);
			this.logDateFormat = cmd.getOptionValue(OPT_DATE_FORMAT, DEFAULT_LOG_DATE_FORMAT);
			final String[] logLevelArgs = cmd.getOptionValue(OPT_LOG_LEVEL).split(",\\s*|\\s+");
			boolean firstIteration = true;
			for(final String logLevel: logLevelArgs) {
				final String tmpLevel = logLevel.toUpperCase();
				this.logLevels.add(LogLevel.valueOf(tmpLevel));
				if(firstIteration) {
					firstIteration = false;
					this.regexLogLevels += tmpLevel;
				} else {
					this.regexLogLevels += "|" + tmpLevel;
				}
			}
			this.regexLogLevels = "\\*(" + this.regexLogLevels + ")\\*";
			this.optUserPattern = cmd.getOptionValue(OPT_USER_PATTERN, ".*"); // defaults to any match

			final String executionPath = System.getProperty("user.dir");
			this.optInputSourcePath = cmd.getOptionValue(OPT_INPUT_SOURCE_PATH);
			if(this.optInputSourcePath.charAt(0) != '/') {
				this.optInputSourcePath = executionPath + "/" + this.optInputSourcePath;
			}
			this.inputFile = new File(this.optInputSourcePath);
			this.optOutputFilePath = null;
			if(cmd.hasOption(OPT_OUTPUT_FILE_PATH)) {
				this.optOutputFilePath = cmd.getOptionValue(OPT_OUTPUT_FILE_PATH);
				if(this.optOutputFilePath.charAt(0) != '/') {
					this.optOutputFilePath = executionPath + "/" + this.optOutputFilePath;
				}
			}
			this.optSort = cmd.getOptionValue(OPT_SORT, DEFAULT_VALUE_OPT_SORT);
			this.flagUnique = cmd.hasOption(FLAG_UNIQUE);
			this.tempDir = new File(this.inputFile.getParent() + "/" + this.inputFile.getName() + ".d");
		} catch(final ParseException e) {
			System.out.println(e.getMessage());
			helpFormatter.printHelp("log4jparser", cmdOptions);
			System.exit(-1);
		}
	}

	private Option createOption(final String longOpt, final String description, final boolean hasArgs, final boolean required) {
		final Option opt = new Option(longOpt.charAt(0) + "", longOpt, hasArgs, (required ? "" : "(optional) ") + description);
		opt.setRequired(required);
		return opt;
	}

	/**
	 * @return the logLevels
	 */
	public List<LogLevel> getLogLevels() {
		return this.logLevels;
	}

	public boolean hasLogLevel(final LogLevel level) {
		return this.logLevels.contains(level);
	}

	/**
	 * @return the logDateFormat
	 */
	public String getLogDateFormat() {
		return this.logDateFormat;
	}

	public String getRegexLogLevels() {
		return this.regexLogLevels;
	}

	public String getOptUserPattern() {
		return this.optUserPattern;
	}

	public String getOptInputSourcePath() {
		return this.optInputSourcePath;
	}

	public File getInputFile() {
		return this.inputFile;
	}

	/**
	 * @return the tmpDir
	 */
	public File getTempDir() {
		return this.tempDir;
	}

	public String getOutputFilePath() {
		return this.optOutputFilePath;
	}

	public boolean isWriteToFileEnabled() {
		return !"".equals(this.optOutputFilePath) && this.optOutputFilePath != null;
	}

	public String getOptSort() {
		return this.optSort;
	}

	public Boolean isFlagUnique() {
		return this.flagUnique;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
