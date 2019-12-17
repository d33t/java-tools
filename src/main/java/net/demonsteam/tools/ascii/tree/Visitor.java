package net.demonsteam.tools.ascii.tree;

public interface Visitor<T> {

	Visitor<T> visitTree(Node<T> tree);

	void visitData(Node<T> parent);
}
