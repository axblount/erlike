package erlike;

/**
 * All references to {@link Node}s are handled transparently through this class.
 * It does not matter if it's the current Node or one running on another server.
 */
public interface NodeId {
    ProcId spawn(Class<? extends Proc> procType, Object... args);
    ProcId spawn(Class<? extends Proc> procType);
    ProcId spawn(Lambda.Zero zero);
    <A> ProcId spawn(Lambda.One<A> one, A a);
    <T> ProcId spawnRecursive(Lambda.Recursive<T> rec, T t);
    <A, B> ProcId spawn(Lambda.Two<A, B> two, A a, B b);
    <A, B, C> ProcId spawn(Lambda.Three<A, B, C> three, A a, B b, C c);
    <A, B, C, D> ProcId spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d);
}
