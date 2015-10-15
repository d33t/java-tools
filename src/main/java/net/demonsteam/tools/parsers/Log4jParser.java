package net.demonsteam.tools.parsers;

/* Copyright (C) <2015> <d33t>
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Parses log4j formatted files
 *
 * @author d33t
 * @date Jan 28, 2015
 */
public class Log4jParser {

	private static final String REGULAR_EXPRESSION_STACKTRACE_MATCH_LINE = "^\\s*(at\\s|Caused\\sby|\\.{3}|[a-zA-Z\\.]+Exception).*";
	private static final Pattern PATTERN_STACKTRACE_MATCH_LINE = Pattern.compile(REGULAR_EXPRESSION_STACKTRACE_MATCH_LINE);

	private static final int ARG_POSITION_LOG_LEVEL = 0;
	private static final int ARG_POSITION_USER_PATTERN = 1;
	private static final int ARG_POSITION_INPUT_SOURCE_PATH = 2;
	private static final int ARG_POSITION_OUTPUT_FILE = 3;

	public static void main(String... args) {
		if(args.length < 3) {
			throw new IllegalArgumentException(String.format("Syntax java %s <loglevel> <pattern> <path to file|directory|zip> [outputfile]", Log4jParser.class));
		}
		AppArguments appArgs = new AppArguments(args);
		BufferedWriter writer = null;
		try {
			if(appArgs.isWriteToFileEnabled()) {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(appArgs.getOutputFilePath()), "UTF-8"));
			} else {
				writer = new BufferedWriter(new OutputStreamWriter(System.out));
			}
			parseFile(appArgs.getFile(), writer, appArgs);
		} catch(ZipException e) {
			LogLevel.ERROR.print("Cannot read source from zip file: %s", e.getMessage());
		} catch(IOException e) {
			LogLevel.ERROR.print("Cannot read source from file: %s", e.getMessage());
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch(IOException e) {
					LogLevel.WARN.print("It seems that there is a problem with the outputstream");
				}
			}
		}
	}

	private static void parseFile(File fileArg, BufferedWriter writer, AppArguments appArgs) throws ZipException, IOException {

		if(fileArg.getPath().matches(".*\\.zip$")) {
			ZipFile zipFile = new ZipFile(fileArg);

			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			List<? extends ZipEntry> contentEntries = Collections.list(entries);
			Collections.sort(contentEntries, new Comparator<ZipEntry>() {

				@Override
				public int compare(ZipEntry first, ZipEntry second) {
					return first.getName().compareTo(second.getName());
				}
			});
			for(ZipEntry contentEntry: contentEntries) {
				if(contentEntry.isDirectory()) {
					continue; //TODO implement directory tree walking
				}
				parseFile(zipFile.getInputStream(contentEntry), writer, fileArg.getPath() + "/" + contentEntry.getName(), appArgs);
			}
			zipFile.close();
		} else if(fileArg.isDirectory()) {
			File[] files = fileArg.listFiles();
			if(files.length > 0) {
				for(File file: files) {
					parseFile(file, writer, appArgs);
				}
			}
		} else {
			parseFile(new FileInputStream(fileArg), writer, fileArg.getPath(), appArgs);
		}

	}

	private static void parseFile(InputStream inputStream, BufferedWriter writer, String path, AppArguments appArgs) {
		LogLevel.INFO.print("###################################### START Parsing file %s ######################################", path);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			boolean continueMatching = false;
			while((line = br.readLine()) != null) {
				Matcher userDefinedMatcher = appArgs.getPattern().matcher(line);
				boolean userDefinedMatches = userDefinedMatcher.matches();
				boolean stackTraceMatches = (continueMatching && !userDefinedMatches && appArgs.hasLogLevel(LogLevel.ERROR) && PATTERN_STACKTRACE_MATCH_LINE.matcher(line).matches());
				if(userDefinedMatches
					|| stackTraceMatches) {
					writer.write(line + "\n");
				}
				continueMatching = continueMatching && stackTraceMatches || userDefinedMatches;
			}
		} catch(IOException e) {
			LogLevel.ERROR.print("Cannot read source from file %s", path);
		} finally {
			if(inputStream != null) {
				try {
					inputStream.close();
				} catch(IOException e) {
					LogLevel.WARN.print("It seems that the stream to the file %s was never opened", path);
				}
			}
		}
		LogLevel.INFO.print("###################################### END Parsing file %s ######################################", path);
	}

	public static class AppArguments {

		private List<LogLevel> logLevels = new ArrayList<LogLevel>();
		private String expression;
		private String path;
		private File file;
		private Pattern pattern;
		private String outputFilePath;

		public AppArguments(String[] args) throws IllegalArgumentException {
			String[] logLevelArgs = args[ARG_POSITION_LOG_LEVEL].split(",\\s*|\\s+");
			boolean firstIteration = true;
			String logLevelExpression = "";
			for(String logLevel: logLevelArgs) {
				String tmpLevel = logLevel.toUpperCase();
				logLevels.add(LogLevel.valueOf(tmpLevel));
				if(firstIteration) {
					firstIteration = false;
					logLevelExpression += tmpLevel;
				} else {
					logLevelExpression += "|" + tmpLevel;
				}
			}
			//expression = Pattern.quote(args[1]);
			expression = args[ARG_POSITION_USER_PATTERN];
			path = args[ARG_POSITION_INPUT_SOURCE_PATH];
			file = new File(path);
			if(!expression.contains("\\*(" + logLevelExpression + ")\\*")) {
				expression = ".*\\*(" + logLevelExpression + ")\\*" + expression;
			}
			pattern = Pattern.compile(expression);
			outputFilePath = null;
			if(args.length > 3) {
				outputFilePath = args[ARG_POSITION_OUTPUT_FILE];
			}
		}

		public List<LogLevel> getLogLevels() {
			return logLevels;
		}

		public boolean hasLogLevel(LogLevel level) {
			return logLevels.contains(level);
		}

		public String getExpression() {
			return expression;
		}

		public String getPath() {
			return path;
		}

		public File getFile() {
			return file;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public String getOutputFilePath() {
			return outputFilePath;
		}

		public boolean isWriteToFileEnabled() {
			return !"".equals(outputFilePath) && outputFilePath != null;
		}
	}

	/**
	 * Enumeration for log levels
	 */
	public enum LogLevel {
		FATAL {

			@Override
			public void print(String msg, Object... msgArgs) {
				System.err.println(formatMessage(msg, msgArgs));
			}
		},
		ERROR {

			@Override
			public void print(String msg, Object... msgArgs) {
				System.err.println(formatMessage(msg, msgArgs));
			}
		},
		WARN,
		INFO,
		DEBUG,
		TRACE;

		public void print(String msg, Object... msgArgs) {
			System.out.println(formatMessage(msg, msgArgs));
		}

		public String formatMessage(String msg, Object... msgArgs) {
			ArrayDeque<Object> args = new ArrayDeque<Object>(Arrays.asList(msgArgs));
			args.addFirst(this);
			return String.format("*%s* " + msg, args.toArray());
		}
	}
}
