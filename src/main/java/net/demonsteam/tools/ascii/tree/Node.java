/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.ascii.tree;

import java.util.Set;

/**
 * TODO documentation is missing
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 13 Nov 2019
 */
public interface Node<T> extends Visitable<T> {

	public T getName();

	public T[] getProperties();

	public Node<String> getParent();

	public Set<Node<T>> getChildren();

	public boolean isPrintable();

	public void setPrintable(boolean printable);

	public Node<T> visit(T name);

	public Node<T> visit(Node<T> child);
}
