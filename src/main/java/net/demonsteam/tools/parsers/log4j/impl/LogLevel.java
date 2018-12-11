/*
 * Copyright (C) <2018> <eggs unimedia GmbH>
 */
package net.demonsteam.tools.parsers.log4j.impl;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 10 Dec 2018
 */
/**
 * Enumeration for log levels
 */
public enum LogLevel {
	FATAL {

		@Override
		public void printToConsole(final String msg, final Object... msgArgs) {
			System.err.println(formatMessage(msg, msgArgs));
		}
	},
	ERROR {

		@Override
		public void printToConsole(final String msg, final Object... msgArgs) {
			System.err.println(formatMessage(msg, msgArgs));
		}
	},
	WARN,
	INFO,
	DEBUG,
	TRACE;

public void printToConsole(final String msg, final Object... msgArgs) {
	System.out.println(formatMessage(msg, msgArgs));
}

public String formatMessage(final String msg, final Object... msgArgs) {
	final ArrayDeque<Object> args = new ArrayDeque<Object>(Arrays.asList(msgArgs));
	args.addFirst(this);
	return String.format("*%s* " + msg, args.toArray());
}
}
