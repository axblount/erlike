package erlike;

/**
 * This is a {@link ProcRef} that represents a {@link Proc} running
 * on this JVM.
 */
class LocalProcRef implements ProcRef {
  private final Proc proc;

  public LocalProcRef(Proc proc) {
    this.proc = proc;
  }

  @Override
  public long id() {
    return proc.getId();
  }

  @Override
  public void send(Object message) {
    proc.addMail(message);
  }

  @Override
  public NodeRef node() {
    return proc.node();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof LocalProcRef) {
      return this.proc.equals(((LocalProcRef) other).proc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return proc.hashCode();
  }

  @Override
  public String toString() {
    return String.format("%s->%s", node(), proc.getId());
  }
}
