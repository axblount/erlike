/*
 * Copyright (C) 2015 Alex Blount <axblount@email.arizona.edu>
 *
 * This file is part of Erlike, see <https://github.com/axblount/erlike>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package erlike;

import java.io.Serializable;

/**
 * Classes implementing SystemMail will be treated differently
 * when received by processes.
 */
interface SystemMail extends Serializable {
  /**
   * Apply the effect of this system mail to a process.
   * Subclasses should not rely on this being called inside
   * of the Proc thread.
   *
   * @param proc The proc to visit.
   */
  void visit(Proc proc);

  /**
   * This represents system mail that has a sender process.
   */
  abstract class SenderMail implements SystemMail {
    private final ProcRef sender;

    protected SenderMail(ProcRef sender) {
      this.sender = sender;
    }

    public ProcRef getSender() {
      return sender;
    }
  }

  /**
   * Sent to complete a link between two processes.
   */
  class Link extends SenderMail {
    private static final long serialVersionUID = 1L;

    public Link(ProcRef sender) {
      super(sender);
    }

    @Override
    public void visit(Proc proc) {
      proc.completeLink(getSender());
    }
  }

  /**
   * Sent to complete a link between two processes.
   */
  class Unlink extends SenderMail {
    private static final long serialVersionUID = 1L;

    public Unlink(ProcRef sender) {
      super(sender);
    }

    @Override
    public void visit(Proc proc) {
      proc.completeUnlink(getSender());
    }
  }

  /**
   * Sent to notify a proc that another has exited.
   */
  class LinkExit extends SenderMail {
    private static final long serialVersionUID = 1L;

    public LinkExit(ProcRef sender) {
      super(sender);
    }

    @Override
    public void visit(Proc proc) {
      // TODO: set interrupt reason
      proc.interrupt();
    }
  }
}
