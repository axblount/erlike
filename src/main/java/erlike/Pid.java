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

public final class Pid implements Serializable {
    final Nid nid;
    final long procId;

    Pid(final Nid nid, final long procId) {
        this.nid = nid;
        this.procId = procId;
    }

    public Nid getNid() {
        return nid;
    }

    public void send(Object msg) {
        Library.node().sendById(nid, procId, msg);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pid) {
            Pid pid = (Pid)other;
            return this.procId == pid.procId && this.nid == pid.nid;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s->%d", nid, procId);
    }
}
