/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.ascii.tree.impl;

import net.demonsteam.tools.ascii.tree.Node;
import net.demonsteam.tools.ascii.tree.Visitor;

/**
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 13 Nov 2019
 */
public class PrintIndentedAsciiTreeVisitorImpl implements Visitor<String> {

	private final static int INDENT_STEP = 2;

	private final int indent;

	public PrintIndentedAsciiTreeVisitorImpl(int indent) {
		this.indent = indent;
	}

	@Override
	public Visitor<String> visitTree(Node<String> node) {
		int indentStep = node.getParent() != null && !node.getParent().isPrintable() ? this.indent : this.indent + INDENT_STEP;
		return new PrintIndentedAsciiTreeVisitorImpl(indentStep);
	}

	@Override
	public void visitData(Node<String> node) {
		if(!node.isPrintable()) {
			return;
		}
		StringBuilder output = new StringBuilder();
		output.append(getIndentString(0, '+')).append(node.getName());
		for(String property: node.getProperties()) {
			if(property == null) {
				continue;
			}
			output.append('\n').append(getIndentString(INDENT_STEP, '-')).append(property);
		}
		System.out.println(output.toString());
	}

	private String getIndentString(int shiftIndent, char lastChar) {
		StringBuilder output = new StringBuilder();
		for(int i = 0;i < (this.indent + shiftIndent) - 1;i++) {
			output.append(' ');
		}
		output.append(lastChar);
		return output.toString();
	}
}
