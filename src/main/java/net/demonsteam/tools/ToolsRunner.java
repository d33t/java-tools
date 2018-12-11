/*
 * Copyright (C) <2018> <eggs unimedia GmbH>
 */
package net.demonsteam.tools;

import net.demonsteam.tools.parsers.log4j.Log4jParser;

/**
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 10 Dec 2018
 */
public class ToolsRunner {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		new Log4jParser(args);
	}

}
