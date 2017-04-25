package erlike;

import erlike.functions.Lambda;

/**
 * Created by alex on 11/21/16.
 */
class RemoteNodeRef implements NodeRef {
  private final RemoteNode remoteNode;

  RemoteNodeRef(RemoteNode remoteNode) {
    this.remoteNode = remoteNode;
  }

  @Override
  public ProcRef spawn(Class<? extends Proc> procType, Object... args) {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public ProcRef spawn(Lambda.Zero zero) {
    // FIXME: Implement me!
    remoteNode.sendObject("TODO");
    throw new RuntimeException("Not implemented.");
  }
}
