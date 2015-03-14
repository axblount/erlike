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

final class LocalPid implements Pid {
    private final Node homeNode;
    private final long procId;

    LocalPid(final Node contextNode, final long procId) {
        this.homeNode = contextNode;
        this.procId = procId;
    }

    @Override
    public void send(Object msg) {
        homeNode.sendById(procId, msg);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LocalPid) {
            LocalPid pid = (LocalPid)other;
            return this.procId == pid.procId && this.homeNode == pid.homeNode;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s->%d", homeNode.getName(), procId);
    }
}
