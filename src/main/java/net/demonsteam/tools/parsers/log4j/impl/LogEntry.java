/*
 * Copyright (C) <2018> <eggs unimedia GmbH>
 */
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
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 10 Dec 2018
 */
public class LogEntry implements Comparable<LogEntry> {

	private static final String FORMAT_REGEX_LINE_START = "^(.*)\\s\\*(%s)\\*\\s";
	private static final Pattern PATTERN_LOG_LINE_START = Pattern.compile(String.format(FORMAT_REGEX_LINE_START, Arrays.stream(LogLevel.values()).map(l -> l.toString()).collect(Collectors.joining("|"))));

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

	public LogEntry(final String line, final AppArguments appArgs) throws ParseException, IOException {
		this.line = line;
		this.sdf = new SimpleDateFormat(appArgs.getLogDateFormat());
		this.sortBy = appArgs.getOptSort();
		final String logDate = extractLogDate(line);
		if(logDate != null) {
			this.firstOccurrenceDate = this.lastOccurrenceDate = this.sdf.parse(logDate);
			this.md5Hex = DigestUtils.md5Hex(StringUtils.substringAfter(line, logDate));
			this.tempFile = new File(appArgs.getTempDir() + "/" + this.md5Hex);
			this.newLogEntry = true;
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
			LogLevel.ERROR.printToConsole("Can't write line to temp file. Temp file is not initialized. Line: %s", line);
			return;
		}
		this.multiline = true;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.tempFile, true))) {
			bw.write(line + "\n");
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
		return String.format("\n%s -> Count: %d, Date first match: %s, Date last match: %s, First match line number: %d, Multiline: %s",
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
			writer.write("Can't read the exception contents: " + e.getMessage());
		}
	}

}
