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

/**
 * This interface represents the id of a process
 * somewhere within the given Erlike system.
 * The process may be local or remote.
 */
public interface Pid {
    /**
     * Send a message to the process this points to.
     * @param msg The message to send.
     */
    public void send(Object msg);

    /**
     * Get a string representing the process this Pid represents.
     * Two Pids for the same process may have two different names
     * depending on where they are printed.
     *
     * @return A String representing this process.
     */
    public String getName();
}

