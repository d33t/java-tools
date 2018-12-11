package net.demonsteam.tools.parsers.log4j;

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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.demonsteam.tools.parsers.log4j.impl.AppArguments;
import net.demonsteam.tools.parsers.log4j.impl.LogEntry;
import net.demonsteam.tools.parsers.log4j.impl.LogLevel;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses log4j formatted files
 *
 * @author d33t
 * @date Jan 28, 2015
 */
public class Log4jParser {

	private static final int READ_AHEAD_LIMIT = (512 * 1000) / 8; // limit to 512KB of characters

	public Log4jParser(final String... consoleArgs) {
		final AppArguments appArgs = new AppArguments(consoleArgs);
		LogLevel.INFO.printToConsole("###################################### App arguments ######################################\n%s", appArgs);

		try (BufferedWriter writer = new BufferedWriter(appArgs.isWriteToFileEnabled() ? new OutputStreamWriter(new FileOutputStream(appArgs.getOutputFilePath()), "UTF-8") : new OutputStreamWriter(System.out))) {
			boolean cleanUp = false;
			if(!appArgs.getTempDir().exists()) {
				cleanUp = appArgs.getTempDir().mkdir();
			}

			parseFile(appArgs.getInputFile(), writer, appArgs);

			if(cleanUp) {
				for(final File tmpFile: appArgs.getTempDir().listFiles()) {
					tmpFile.delete();
				}
				appArgs.getTempDir().delete();
			}
		} catch(final ZipException e) {
			LogLevel.ERROR.printToConsole("Cannot read source from zip file: %s", e.getMessage());
		} catch(final IOException e) {
			LogLevel.ERROR.printToConsole("Cannot read source from file: %s", e.getMessage());
			e.printStackTrace();
		}
	}

	private void parseFile(final File fileArg, final BufferedWriter writer, final AppArguments appArgs) throws ZipException, IOException {

		if(fileArg.getPath().matches(".*\\.zip$")) {
			final ZipFile zipFile = new ZipFile(fileArg);

			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			final List<? extends ZipEntry> contentEntries = Collections.list(entries);
			Collections.sort(contentEntries, new Comparator<ZipEntry>() {

				@Override
				public int compare(final ZipEntry first, final ZipEntry second) {
					return first.getName().compareTo(second.getName());
				}
			});
			for(final ZipEntry contentEntry: contentEntries) {
				if(contentEntry.isDirectory()) {
					continue; //TODO implement directory tree walking within the archive
				}
				parseFile(zipFile.getInputStream(contentEntry), writer, fileArg.getPath() + "/" + contentEntry.getName(), appArgs);
			}
			zipFile.close();
		} else if(fileArg.isDirectory()) {
			final File[] files = fileArg.listFiles();
			if(files.length > 0) {
				for(final File file: files) {
					parseFile(file, writer, appArgs);
				}
			}
		} else {
			parseFile(new FileInputStream(fileArg), writer, fileArg.getPath(), appArgs);
		}

	}

	// TODO build md5 over the exception body and group such exceptions, ignore the first line as there may be other parameters making them look completely different
	private void parseFile(final InputStream inputStream, final BufferedWriter writer, final String path, final AppArguments appArgs) {
		LogLevel.INFO.printToConsole("###################################### START Parsing file %s ######################################", path);
		final Map<String, LogEntry> uniqueLogEntries = new HashMap<>();
		final Map<String, String> uniqueLogEntryBodyMap = new HashMap<>();
		try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream))) {
			final long lineNumber = 0l;
			LogEntry logEntry;
			while((logEntry = findNextEntry(writer, appArgs, uniqueLogEntries, inputReader, lineNumber)) != null) {
				if(!logEntry.isLineFiltertMatching()) {
					continue;
				}
				if(appArgs.isFlagUnique()) {
					final String bodyMd5 = logEntry.getBodyMd5();
					if(uniqueLogEntryBodyMap.containsKey(bodyMd5)) {
						final String mappedEntryMd5 = uniqueLogEntryBodyMap.get(bodyMd5);
						uniqueLogEntries.get(mappedEntryMd5).addDuplicate(logEntry.getLine());
					} else {
						uniqueLogEntries.put(logEntry.getMd5(), logEntry);
						if(StringUtils.isNotBlank(bodyMd5)) {
							uniqueLogEntryBodyMap.put(bodyMd5, logEntry.getMd5());
						}
					}
				} else {
					logEntry.writeLogEntryData(writer);
				}
			}

			if(appArgs.isFlagUnique() && uniqueLogEntries.size() > 0) {
				final List<LogEntry> sortedEntries = new ArrayList<>(uniqueLogEntries.values());
				Collections.sort(sortedEntries);
				for(final LogEntry entry: sortedEntries) {
					writer.write(entry.toString()); // write stats
					entry.writeLogEntryData(writer);
				}
			}
			writer.flush();
		} catch(final IOException | ParseException e) {
			LogLevel.ERROR.printToConsole("Cannot read source from file %s", path);
			e.printStackTrace();
		}
		LogLevel.INFO.printToConsole("###################################### END Parsing file %s ######################################", path);
	}

	private LogEntry findNextEntry(final BufferedWriter writer, final AppArguments appArgs, final Map<String, LogEntry> localUniqueEntries, final BufferedReader inputReader, long lineNumber) throws IOException {
		String line = null;
		try {
			if((line = inputReader.readLine()) != null) {
				final LogEntry logEntry = new LogEntry(line, appArgs);
				logEntry.setLineNumber(++lineNumber);

				if(!logEntry.isLineFiltertMatching()) {
					return logEntry;
				}
				if(localUniqueEntries.containsKey(logEntry.getMd5())) {
					localUniqueEntries.get(logEntry.getMd5()).addDuplicate(line);
					// ok, we have this one already, skip all body bytes if any other related lines
					inputReader.mark(READ_AHEAD_LIMIT);
					while((line = inputReader.readLine()) != null) {
						lineNumber++;
						final LogEntry nextLogEntry = new LogEntry(line, appArgs);
						if(nextLogEntry.isNewLine()) {
							lineNumber--;
							inputReader.reset(); // we don't want to read any new entries, so reset the last line
							break;
						}
						// just skip any content here
						inputReader.mark(READ_AHEAD_LIMIT);
					}
					return localUniqueEntries.get(logEntry.getMd5());
				} else { // new entry
					localUniqueEntries.put(logEntry.getMd5(), logEntry);

					inputReader.mark(READ_AHEAD_LIMIT);
					// read any body lines if any
					while((line = inputReader.readLine()) != null) {
						lineNumber++;
						final LogEntry nextLogEntry = new LogEntry(line, appArgs);
						if(nextLogEntry.isNewLine()) {
							lineNumber--;
							inputReader.reset(); // we don't want to read any new entries, so reset the last line
							return logEntry;
						}
						logEntry.appendBody(line);
						inputReader.mark(READ_AHEAD_LIMIT);
					}
					return logEntry;
				}
			}
		} catch(final ParseException e) {
			LogLevel.FATAL.printToConsole("Can't parse the line '%s'. Details: %s. Writing the match to the output", line, e.getMessage());
		}
		return null;
	}
}
