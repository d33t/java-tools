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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Defines the available log levels in a log filea and gives some apis for printing formatted messages to the given
 * stream
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public enum LogLevel {
	FATAL {

		@Override
		public void printlnToConsole(final String msg, final Object... msgArgs) {
			System.err.println(formatMessage(msg, msgArgs));
		}
	},
	ERROR {

		@Override
		public void printlnToConsole(final String msg, final Object... msgArgs) {
			System.err.println(formatMessage(msg, msgArgs));
		}
	},
	WARN,
	INFO,
	DEBUG,
	TRACE;

	public void println(Writer writer, final String msg, final Object... msgArgs) throws IOException {
		writer.write(formatMessage(msg, msgArgs) + "\n");
	}

	public void printlnToConsole(final String msg, final Object... msgArgs) {
		System.out.println(formatMessage(msg, msgArgs));
	}

	public String formatMessage(final String msg, final Object... msgArgs) {
		final ArrayDeque<Object> args = new ArrayDeque<>(Arrays.asList(msgArgs));
		args.addFirst(this);
		return String.format("*%s* " + msg, args.toArray());
	}
}
