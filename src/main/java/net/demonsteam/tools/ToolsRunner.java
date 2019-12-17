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
package net.demonsteam.tools;

import java.util.Arrays;

import net.demonsteam.tools.args.BaseArgs;
import net.demonsteam.tools.ascii.TextPathToAscii;
import net.demonsteam.tools.parsers.log4j.Log4jParser;

import org.apache.commons.cli.Options;

import lombok.Getter;

/**
 * Application main class and starting point for running the different tools
 *
 * @author d33t
 * @date 10 Dec 2018
 */
public class ToolsRunner {

	/**
	 * The tool args. <br>
	 * Required --tool/-t to choose the tool to run
	 *
	 * @param args - the tool args
	 */
	public static void main(final String[] args) {
		ToolsRunnerArgs toolsArgs = new ToolsRunnerArgs(args);
		toolsArgs.postInit();
		String[] appArgs = toolsArgs.getParser().getLastParsedUnknownArgs();

		switch(toolsArgs.getTool()) {
			case LOG4J_PARSER: {
				Log4jParser.main(appArgs);
				break;
			}
			case PATH_TO_ASCII: {
				TextPathToAscii.main(appArgs);
				break;
			}
			default: {
				toolsArgs.printUsageAndExit();
			}
		}
	}

	private static class ToolsRunnerArgs extends BaseArgs {

		private static final String OPT_TOOL = "tool";

		@Getter
		private Tool tool;

		public ToolsRunnerArgs(final String[] args) {
			super("ToolsRunner", args);
		}

		@Override
		public void postInit() {
			try {
				this.tool = Tool.valueOf(getCommandLine().getOptionValue(OPT_TOOL, Tool.DEFAULT.toString()).toUpperCase());
			} catch(IllegalArgumentException e) {
				this.tool = Tool.DEFAULT;
			}
		}

		@Override
		public Options getCmdOptions() {
			final Options cmdOptions = new Options();
			cmdOptions.addOption(createOption(OPT_TOOL, "Specify the tool to run. Tools: " + Arrays.toString(Tool.values()), true, true));
			return cmdOptions;
		}
	}
}
