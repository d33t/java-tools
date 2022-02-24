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

import net.demonsteam.tools.Tool;
import net.demonsteam.tools.args.BaseArgs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * This class is responsible for parsing, transforming and holding the application arguments
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public class Log4jParserArgs extends BaseArgs {

	public static final String DEFAULT_LOG_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss.SSSS";
	public static final String DEFAULT_VALUE_OPT_SORT = "date";

	private static final String OPT_DATE_FORMAT = "dateFormat";
	private static final String OPT_LOG_LEVEL = "loglevel";
	private static final String OPT_USER_PATTERN = "pattern";
	private static final String OPT_INPUT_SOURCE_PATH = "inputFile";
	private static final String OPT_OUTPUT_FILE_PATH = "outputFile";
	private static final String OPT_SORT = "sort";
	private static final String FLAG_UNIQUE = "unique";
	private static final String FLAG_URL_INFO = "urlInfo";

	@Getter
	private List<LogLevel> logLevels = new ArrayList<>();
	@Getter
	private String regexLogLevels = StringUtils.EMPTY;
	@Getter
	private String optUserPattern;
	@Getter
	private String optInputSourcePath;
	@Getter
	private File inputFile;
	@Getter
	private String optOutputFilePath;
	@Getter
	private String optSort;
	@Getter
	private boolean flagUnique;
	@Getter
	private String logDateFormat;
	@Getter
	private File tempDir;
	@Getter
	private boolean flagUrlInfo;
	@Getter(lazy = true)
	private final Options cmdOptions = initCmdOptions();
	
	public Log4jParserArgs(final String[] args) {
		super(Tool.LOG4J_PARSER.toString(), args);
	}

	public Options initCmdOptions() {
		final Options options = new Options();
		options.addOption(createOption(OPT_DATE_FORMAT, "Specify the log format of the log entries. Defaults to: " + DEFAULT_LOG_DATE_FORMAT, true, false));
		options.addOption(createOption(OPT_LOG_LEVEL, "A valid log4J log level: " + Arrays.toString(LogLevel.values()) + ". Multiple values can be separated by comma or space.", true, true));
		options.addOption(createOption(OPT_USER_PATTERN, "Pattern to match", true, false));
		options.addOption(createOption(OPT_INPUT_SOURCE_PATH, "Absolute or relative to the current directory path to the logfile (text or zip)", true, true));
		options.addOption(createOption(OPT_OUTPUT_FILE_PATH, "Absolute or relative to the current directory path to the output file. If omitted the standard output is used.", true, false));
		options.addOption(createOption(FLAG_UNIQUE, "Unique lines with occurrence count", false, false));
		options.addOption(createOption(OPT_SORT, "Sort either by date or unique count. This option is only used when '" + FLAG_UNIQUE + "' flag is set. Default to date.", true, false));
		options.addOption(createOption(FLAG_URL_INFO, "Prints all unique urls where the exception occured", false, false));
		return options;
	}

	@Override
	public void postInit() {
		CommandLine cmd = getCommandLine();
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
		this.flagUrlInfo = cmd.hasOption(FLAG_URL_INFO);
		this.tempDir = new File(this.inputFile.getParent(), this.inputFile.getName() + ".d");
	}

	public boolean hasLogLevel(final LogLevel level) {
		return this.logLevels.contains(level);
	}

	public boolean isWriteToFileEnabled() {
		return !"".equals(this.optOutputFilePath) && this.optOutputFilePath != null;
	}
}
