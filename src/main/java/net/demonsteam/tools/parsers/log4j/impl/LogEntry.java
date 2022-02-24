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
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single line of log file
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public class LogEntry implements Comparable<LogEntry> {

	
	// matching lines of the following form
	// 16.02.2022 00:00:00.528 *INFO* [service details] message details 
	private static final String FORMAT_LLS_REGEX = "^([^\\*]+)\\*(%s)\\*\\s((\\[.+(?=\\])\\]))?";
	// log line start (LLS) pattern 
	private static final Pattern LLS_PATTERN = Pattern.compile(String.format(FORMAT_LLS_REGEX + "(.*)", Arrays.stream(LogLevel.values()).map(LogLevel::toString).collect(Collectors.joining("|"))));
	// service details regex
	private static final String SD_REGEX = "\\[.*?(GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE|PATCH)\\s(/[^\\s]+)\\sHTTP/1\\.[0-1]\\]";
	private static final Pattern SD_PATTERN = Pattern.compile(SD_REGEX);
	
	private static final int LLS_GROUP_DATE = 1;
	private static final int LLS_GROUP_LOG_LEVEL = 2;
	private static final int LLS_GROUP_SERVICE_DETAILS = 4;
	private static final int LLS_GROUP_MESSAGE = 5;
	
	private static final int SD_GROUP_HTTP_METHOD = 1;
	private static final int SD_GROUP_HTTP_URL = 2;
	
	@Getter
	private String line;
	@Getter @Setter
	private long lineNumber;
	private final SimpleDateFormat sdf;
	@Getter
	private String md5Hex;
	private String bodyMd5Hex;
	private Date firstOccurrenceDate;
	private Date lastOccurrenceDate;
	@Getter
	private long count;
	private String sortBy;
	@Getter
	private boolean newLogEntry;
	@Getter
	private boolean lineFilterMatching;
	private File tempFile;
	@Getter
	private boolean multiline;
	private final String tempDirPath;
	@Getter
	private LogLevel logLevel;
	@Getter
	private String serviceDetails;
	@Getter
	private String httpMethod;
	@Getter
	private String httpUrl;
	
	private boolean urlInfoEnabled;
	
	public LogEntry(final String line, final Log4jParserArgs appArgs) throws ParseException {
		this.line = line;
		this.tempDirPath = appArgs.getTempDir().getPath();
		this.sdf = new SimpleDateFormat(appArgs.getLogDateFormat());
		this.count = 1l;
		this.sortBy = appArgs.getOptSort();
		final Matcher llsMatcher = LLS_PATTERN.matcher(line);
		if(llsMatcher.find()) {
			this.newLogEntry = true;
			this.firstOccurrenceDate = this.lastOccurrenceDate = this.sdf.parse(llsMatcher.group(LLS_GROUP_DATE).trim());
			String hashable = StringUtils.defaultString(llsMatcher.group(LLS_GROUP_MESSAGE));
			this.md5Hex = DigestUtils.md5Hex(hashable);
			this.logLevel = LogLevel.valueOf(llsMatcher.group(LLS_GROUP_LOG_LEVEL));
			this.serviceDetails = llsMatcher.group(LLS_GROUP_SERVICE_DETAILS);
			if(StringUtils.isNotBlank(serviceDetails)) {
				final Matcher sdMatcher = SD_PATTERN.matcher(this.serviceDetails);
				if(sdMatcher.find()) {
					this.httpMethod = sdMatcher.group(SD_GROUP_HTTP_METHOD);
					this.httpUrl = sdMatcher.group(SD_GROUP_HTTP_URL);
				}
			}
		} else {
			this.md5Hex = DigestUtils.md5Hex(line);
		}
		final String customUserPatternFormat = FORMAT_LLS_REGEX + appArgs.getOptUserPattern();
		String userPattern = String.format(customUserPatternFormat, appArgs.getLogLevels().stream()
		                                   									.map(LogLevel::toString)
		                                   									.collect(Collectors.joining("|")));
		this.lineFilterMatching = line.matches(userPattern);
		this.urlInfoEnabled = appArgs.isFlagUrlInfo();
	}

	public String getBodyMd5() throws IOException {
		if(this.bodyMd5Hex == null && this.tempFile != null && this.tempFile.exists()) {
			this.bodyMd5Hex = DigestUtils.md5Hex(new FileInputStream(this.tempFile));
		}
		return this.bodyMd5Hex;
	}

	public long addDuplicate(final LogEntry logEntry) throws IOException {
		if(logEntry != null) {
			this.lastOccurrenceDate = logEntry.firstOccurrenceDate;
			appendUrl(logEntry);
		}
		return ++this.count;
	}

	public void appendUrl(final LogEntry logEntry) throws IOException {
		if(urlInfoEnabled && StringUtils.isNotBlank(logEntry.getHttpUrl())) {
			File tempUrlFile = new File(this.tempDirPath, this.md5Hex + "_urls");
			boolean tempUrlFileExists = tempUrlFile.exists();
			boolean entryExists = false;
			if(tempUrlFileExists) {
				try(Stream<String> urlStream = Files.lines(Paths.get(tempUrlFile.getPath()))) {
					entryExists = urlStream.anyMatch(url -> url.contains(logEntry.getHttpUrl()));
				}	
			}
			if(!entryExists) {
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempUrlFile, true))) {
					if(!tempUrlFileExists) {
						bw.write(this.getHttpMethod() + " " + this.getHttpUrl() + "\n");
					}
					bw.write(logEntry.getHttpMethod() + " " + logEntry.getHttpUrl() + "\n");
				}	
			}
		}
	}
	
	public void appendBody(final String line) throws IOException {
		if(this.tempFile == null) {
			this.tempFile = new File(this.tempDirPath, this.md5Hex);
		}
		this.multiline = true;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.tempFile, true))) {
			bw.write(line);
		}
	}

	private String readFile(File file) throws IOException {
		return new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
	}
	
	public void writeLogEntryData(final BufferedWriter writer) throws IOException {
		try {
			writer.write(this.line + "\n");
			if(this.tempFile != null && this.tempFile.exists()) {
				writer.write(this.readFile(this.tempFile) + "\n");
			}
			File tempUrlFile = new File(this.tempDirPath, this.md5Hex + "_urls");
			if(urlInfoEnabled && tempUrlFile.exists()) {
				writer.write(this.readFile(tempUrlFile) + "\n");
			}
		} catch(final IOException e) {
			LogLevel.ERROR.printlnToConsole("Can't read the exception contents: " + e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final LogEntry o) {
		if(Log4jParserArgs.DEFAULT_VALUE_OPT_SORT.equals(this.sortBy)) {
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
		if(!(other instanceof LogEntry)) {
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
}
