package net.demonsteam.tools.ascii.tree;

public interface Visitable<T> {

    void accept(Visitor<T> visitor);
}
