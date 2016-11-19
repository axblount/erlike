package erlike;

import erlike.functions.Lambda;

/**
 * This is a {@link NodeRef} that represents a {@link Node} running
 * on this JVM.
 */
class LocalNodeRef implements NodeRef {
  private final Node node;

  public LocalNodeRef(Node node) {
    this.node = node;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof LocalNodeRef) {
      return this.node.equals(((LocalNodeRef) other).node);
    }
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
  public ProcRef spawn(Class<? extends Proc> procType, Object... args) {
    return node.spawn(procType, args);
  }

  @Override
  public ProcRef spawn(Class<? extends Proc> procType) {
    return node.spawn(procType);
  }

  @Override
  public ProcRef spawn(Lambda.Zero zero) {
    return node.spawn(zero);
  }

  @Override
  public <A> ProcRef spawn(Lambda.One<A> one, A a) {
    return node.spawn(one, a);
  }

  @Override
  public <T> ProcRef spawnRecursive(Lambda.Recursive<T> rec, T t) {
    return node.spawnRecursive(rec, t);
  }

  @Override
  public <A, B> ProcRef spawn(Lambda.Two<A, B> two, A a, B b) {
    return node.spawn(two, a, b);
  }

  @Override
  public <A, B, C> ProcRef spawn(Lambda.Three<A, B, C> three, A a, B b, C c) {
    return node.spawn(three, a, b, c);
  }

  @Override
  public <A, B, C, D> ProcRef spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d) {
    return node.spawn(four, a, b, c, d);
  }
}
