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
 * All references to {@link Proc}s are handled transparently through this class.
 * It does not matter if the proc is running on the same node or in a different
 * hemisphere.
 * <p>
 * All you need to be able to do is send the Proc messages and get the
 * Proc's {@link Node}.
 */
public interface ProcRef {
  void send(Object message);

  // FIXME: this should not be here.
  // Instead I'll have to add instanceof checks to distinguish between local/remote ids.
  long id();

  NodeRef node();
}
