/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.ascii.tree.impl;

import java.util.StringTokenizer;

import net.demonsteam.tools.ascii.tree.Node;
import net.demonsteam.tools.ascii.tree.Tree;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 13 Nov 2019
 */
public class AsciiTreeImpl implements Tree<String> {

	private Node<String> root;

	public AsciiTreeImpl(String name, String[] paths, boolean printable) {
		this.root = new NodeImpl(name, null, printable);
		Node<String> current = this.root;
		String root = StringUtils.EMPTY;
		for(String path: paths) {
			if(path.startsWith("#ROOT")) {
				root = path.split("=")[1];
				continue;
			}
			boolean isAbsolute = path.indexOf(0) == '/';
			if(!isAbsolute) {
				path = root + "/" + path;
			}
			Node<String> node = current;
			StringTokenizer tokenizer = new StringTokenizer(path.trim(), "/");
			while(tokenizer.hasMoreTokens()) {
				String nodeName = tokenizer.nextToken();
				if(nodeName.charAt(0) == '-') { // is it a property?
					while(tokenizer.hasMoreTokens()) { // read all of the rest data
						nodeName += "/" + tokenizer.nextToken();
					}
				}
				current = current.visit(nodeName);
			}
			current = node;
		}
	}

	/* (non-Javadoc)
	 * @see net.demonsteam.tools.ascii.tree.Tree#print()
	 */
	@Override
	public void print() {
		if(this.root.getChildren().size() > 1) {
			this.root.setPrintable(true);
		}
		this.root.accept(new PrintIndentedAsciiTreeVisitorImpl(0));
	}

}
