package erlike;

/**
 * This is a {@link NodeId} that represents a {@link Node} running
 * on this JVM.
 */
class LocalNodeId implements NodeId {
    private final Node node;

    public LocalNodeId(Node node) {
        this.node = node;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LocalNodeId)
            return this.node.equals(((LocalNodeId)other).node);
        return false;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String toString() {
        return node.getName();
    }

    @Override
    public ProcId spawn(Class<? extends Proc> procType, Object... args) {
        return node.spawn(procType, args);
    }

    @Override
    public ProcId spawn(Class<? extends Proc> procType) {
        return node.spawn(procType);
    }

    @Override
    public ProcId spawn(Lambda.Zero zero) {
        return node.spawn(zero);
    }

    @Override
    public <A> ProcId spawn(Lambda.One<A> one, A a) {
        return node.spawn(one, a);
    }

    @Override
    public <T> ProcId spawnRecursive(Lambda.Recursive<T> rec, T t) {
        return node.spawnRecursive(rec, t);
    }

    @Override
    public <A, B> ProcId spawn(Lambda.Two<A, B> two, A a, B b) {
        return node.spawn(two, a, b);
    }

    @Override
    public <A, B, C> ProcId spawn(Lambda.Three<A, B, C> three, A a, B b, C c) {
        return node.spawn(three, a, b, c);
    }

    @Override
    public <A, B, C, D> ProcId spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d) {
        return node.spawn(four, a, b, c, d);
    }
}
