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

/*package-local*/
final class PidImpl implements Pid {
    private final Node contextNode;
    private final int nodeId;
    private final int procId;

    PidImpl(final Node contextNode, final int nodeId, final int procId) {
        this.contextNode = contextNode;
        this.nodeId = nodeId;
        this.procId = procId;
    }

    @Override
    public void send(Object msg) {
        contextNode.sendById(nodeId, procId, msg);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PidImpl) {
            PidImpl pid = (PidImpl)other;
            return this.procId == pid.procId && this.nodeId == pid.nodeId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return nodeId ^ procId;
    }

    @Override
    public String toString() {
        if (nodeId == Node.SELF_NODE_ID)
            return String.format("%s->%d", contextNode.getName(), procId);
        return String.format("%s->%d->%d", contextNode.getName(), nodeId, procId);
    }

    @Override
    public String getName() { return toString(); }
}
