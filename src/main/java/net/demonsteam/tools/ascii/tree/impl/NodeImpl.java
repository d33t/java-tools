package net.demonsteam.tools.ascii.tree.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import net.demonsteam.tools.ascii.tree.Node;
import net.demonsteam.tools.ascii.tree.Visitor;

import org.apache.commons.lang3.ArrayUtils;

import lombok.Getter;
import lombok.Setter;

public class NodeImpl implements Node<String> {

	@Getter
	private final String name;

	@Getter
	private String[] properties;

	@Setter
	@Getter
	private boolean printable;

	@Getter
	private Node<String> parent;

	@Getter
	private final Set<Node<String>> children = new LinkedHashSet<>();

	public NodeImpl(String name, Node<String> parent) {
		this(name, parent, true);
	}

	public NodeImpl(String name, Node<String> parent, boolean printable) {
		this.name = name;
		this.properties = new String[5];
		this.parent = parent;
		this.printable = printable;
    }

	@Override
	public void accept(Visitor<String> visitor) {
		visitor.visitData(this);

		for(Node<String> child: this.children) {
			Visitor<String> childVisitor = visitor.visitTree(child);
			child.accept(childVisitor);
		}
	}

	@Override
	public Node<String> visit(String name) {
		if(name.charAt(0) == '-') {
			String propKeyAndValue = name.substring(1);
			if(!ArrayUtils.contains(this.properties, propKeyAndValue)) { // no duplicates
				this.properties = ArrayUtils.add(this.properties, name.substring(1));
			}
			return this;
		}
		for(Node<String> child: this.children) {
			if(child.getName().equals(name)) {
				return child;
			}
		}
		return visit(new NodeImpl(name, this));
    }

	@Override
	public Node<String> visit(Node<String> child) {
        this.children.add(child);
        return child;
    }
}
