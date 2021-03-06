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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a single line of log file
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public class LogEntry implements Comparable<LogEntry> {

	private static final String FORMAT_REGEX_LINE_START = "^([^\\*]+)\\*(%s)\\*\\s";
	private static final Pattern PATTERN_LOG_LINE_START = Pattern.compile(String.format(FORMAT_REGEX_LINE_START + "(.*)", Arrays.stream(LogLevel.values()).map(l -> l.toString()).collect(Collectors.joining("|"))));

	private String line;
	private long lineNumber;
	private final SimpleDateFormat sdf;
	private String md5Hex;
	private String bodyMd5Hex;
	private Date firstOccurrenceDate;
	private Date lastOccurrenceDate;
	private long count = 0l;
	private String sortBy;
	private boolean newLogEntry;
	private boolean lineFilterMatching;
	private File tempFile;
	private boolean multiline;
	private final String tempDirPath;

	public LogEntry(final String line, final AppArguments appArgs) throws ParseException, IOException {
		this.line = line;
		this.tempDirPath = appArgs.getTempDir().getPath();
		this.sdf = new SimpleDateFormat(appArgs.getLogDateFormat());
		this.sortBy = appArgs.getOptSort();
		final Matcher matcher = PATTERN_LOG_LINE_START.matcher(line);
		if(matcher.find()) {
			this.newLogEntry = true;
			this.firstOccurrenceDate = this.lastOccurrenceDate = this.sdf.parse(matcher.group(1).trim());
			String hashable = StringUtils.defaultString(matcher.group(3));
			if(hashable.charAt(0) == '[' && hashable.indexOf(']') > 0) {
				hashable = StringUtils.substringAfterLast(hashable, "]").trim();
			}
			this.md5Hex = DigestUtils.md5Hex(hashable);
		} else {
			this.md5Hex = DigestUtils.md5Hex(line);
		}
		this.lineFilterMatching = line.matches(String.format(FORMAT_REGEX_LINE_START + appArgs.getOptUserPattern(), appArgs.getLogLevels().stream().map(l -> l.toString()).collect(Collectors.joining("|"))));

	}

	/**
	 * @return the line
	 */
	public String getLine() {
		return this.line;
	}

	/**
	 * @param lineNumber the lineNumber to set
	 */
	public void setLineNumber(final long lineNumber) {
		this.lineNumber = lineNumber;
	}

	/**
	 * @return the lineNumber
	 */
	public long getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * @return the multiline
	 */
	public boolean isMultiline() {
		return this.multiline;
	}

	public String getMd5() {
		return this.md5Hex;
	}

	public String getBodyMd5() throws IOException {
		if(this.bodyMd5Hex == null && this.tempFile != null && this.tempFile.exists()) {
			this.bodyMd5Hex = DigestUtils.md5Hex(new FileInputStream(this.tempFile));
		}
		return this.bodyMd5Hex;
	}

	public boolean isNewLine() {
		return this.newLogEntry;
	}

	/**
	 * @return the lineMatches
	 */
	public boolean isLineFiltertMatching() {
		return this.lineFilterMatching;
	}

	public long addDuplicate(final String line) throws ParseException {
		final String logDate = extractLogDate(line);
		if(logDate != null) {
			this.lastOccurrenceDate = this.sdf.parse(logDate);
		}
		return ++this.count;
	}

	private String extractLogDate(final String line) {
		final Matcher matcher = PATTERN_LOG_LINE_START.matcher(line);
		return matcher.find() ? matcher.group(1) : null;
	}

	public long getCount() {
		return this.count;
	}

	public void appendBody(final String line) throws IOException {
		if(this.tempFile == null) {
			this.tempFile = new File(this.tempDirPath + "/" + this.md5Hex);
		}
		this.multiline = true;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.tempFile, true))) {
			bw.write(line);
		}
	}

	public String readBody() throws IOException {
		return new String(Files.readAllBytes(Paths.get(this.tempFile.getPath())), StandardCharsets.UTF_8);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final LogEntry o) {
		if(AppArguments.DEFAULT_VALUE_OPT_SORT.equals(this.sortBy)) {
			if(this.firstOccurrenceDate == null && o.firstOccurrenceDate == null) {
				return 0;
			}
			if(this.firstOccurrenceDate == null) {
				return 1;
			}
			if(o.firstOccurrenceDate == null) {
				return -1;
			}
			return o.firstOccurrenceDate.compareTo(this.firstOccurrenceDate);
		}
		return ((Long) o.count).compareTo(this.count);
	}

	@Override
	public boolean equals(final Object other) {
		if(other == null || !(other instanceof LogEntry)) {
			return false;
		}
		final LogEntry o = (LogEntry) other;
		return new EqualsBuilder().append(this.md5Hex, o.md5Hex).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 117).append(this.md5Hex).hashCode();
	}

	@Override
	public String toString() {
		return String.format("%s -> Count: %d, Date first match: %s, Date last match: %s, First match line number: %d, Multiline: %s",
		                     this.md5Hex, this.count, this.sdf.format(this.firstOccurrenceDate), this.sdf.format(this.lastOccurrenceDate), this.lineNumber, this.multiline);
	}

	/**
	 * @param writer
	 * @throws IOException
	 */
	public void writeLogEntryData(final BufferedWriter writer) throws IOException {
		try {
			writer.write(this.line + "\n");
			if(this.tempFile != null && this.tempFile.exists()) {
				writer.write(this.readBody() + "\n");
			}
		} catch(final IOException e) {
			LogLevel.ERROR.printlnToConsole("Can't read the exception contents: " + e.getMessage());
		}
	}

}
