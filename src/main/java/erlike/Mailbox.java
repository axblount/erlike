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

import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Unbounded MPSC lock-free queue with blocking calls. Hopefully this works.
 * <p>
 * Based on: http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue
 */
// AtomicReferenceFieldUpdater requires lots of rawtypes and unchecked casts.
// Suppress those warnings for the entire class.
@SuppressWarnings({"unchecked", "rawtypes"})
public class Mailbox<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    /**
     * Nodes used to store waiting mail.
     *
     * @param <E> The type of mail held.
     */
    private static class Node<E> {
        public E item;

        @SuppressWarnings("unused")
        private volatile Node<E> volatileNext;

        private static final AtomicReferenceFieldUpdater<Node, Node> nextAccess =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "volatileNext");

        public Node(E item, Node<E> next) {
            this.item = item;
            this.volatileNext = next;
        }

        public Node(E item) {
            this(item, null);
        }

        public Node<E> getNext() {
            return nextAccess.get(this);
        }

        public void setNext(final Node<E> newNext) {
            nextAccess.set(this, newNext);
        }

        public boolean casNext(final Node<E> expect, final Node<E> newNext) {
            return nextAccess.compareAndSet(this, expect, newNext);
        }
    }

    @SuppressWarnings("unused")
    private volatile Node<E> volatileHead, volatileTail;

    private static final AtomicReferenceFieldUpdater<Mailbox, Node> headAccess =
            AtomicReferenceFieldUpdater.newUpdater(Mailbox.class, Node.class, "volatileHead");
    private static final AtomicReferenceFieldUpdater<Mailbox, Node> tailAccess =
            AtomicReferenceFieldUpdater.newUpdater(Mailbox.class, Node.class, "volatileTail");

    /** Used to block for and signal an insertion. */
    private final SignalBarrier insertion;

    /** Creates an empty mailbox. */
    public Mailbox() {
        volatileHead = volatileTail = new Node<E>(null);
        insertion = new SignalBarrier();
    }

    @Override
    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        Node<E> newTail = new Node<E>(e);
        Node<E> oldTail = tailAccess.getAndSet(this, newTail);

        // oldTail is now out of reach for producers.
        // we can set it normally.
        oldTail.setNext(newTail);
        insertion.signal();
        return true;
    }

    @Override
    public boolean add(E e) {
        if (offer(e))
            return true;
        throw new IllegalStateException();
    }

    @Override
    public E poll() {
        Node<E> next = headAccess.get(this).getNext();
        if (next != null) {
            headAccess.set(this, next);
            E result = next.item;
            next.item = null;
            return result;
        }
        return null;
    }

    @Override
    public E remove() {
        E result = poll();
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    @Override
    public boolean remove(Object e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E element() {
        E result = peek();
        if (result == null)
            throw new NoSuchElementException();
        return result;
    }

    @Override
    public E peek() {
        Node<E> first = headAccess.get(this).getNext();
        if (first != null)
            return first.item;
        return null;
    }

    @Override
    public void put(E e) throws InterruptedException {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        E result = poll();
        while (result == null) {
            insertion.await();
            result = poll();
        }
        return result;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E result = poll();
        long nanos = unit.toNanos(timeout);
        while (result == null && nanos > 0) {
            nanos = insertion.awaitNanos(nanos);
            result = poll();
        }
        return result;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int size() {
        int count = 0;
        Node<E> node = headAccess.get(this);
        while (node.getNext() != null) {
            node = node.getNext();
            count++;
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return headAccess.get(this).getNext() == null;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException("a queue cannot drain to itself");
        if (maxElements <= 0)
            return 0;

        int count = 0;
        E current = poll();
        while (current != null && count < maxElements) {
            c.add(current);
            current = poll();
            count++;
        }
        return count;
    }

    /**
     * Remove a node from the queue and return its item.
     * This method moves the head and tail pointers
     * as necessary.
     *
     * @param prev The node previous to the one being removed.
     * @param node The node to be removed.
     * @return The item stored in the removed node.
     */
    private E removeNode(Node<E> prev, Node<E> node) {
        if (prev == null || node == null)
            throw new NullPointerException();
        if (prev == node)
            throw new IllegalArgumentException("Corrupt mailbox");

        E result = node.item;

        if (tailAccess.compareAndSet(this, node, prev)) {
            // node was our tail, but now prev is the tail
            //
            // even though offer will strip off the dead tail,
            // we should set prev.next=null so that (take|poll)Match
            // don't descent into a dead part of the queue.
            // This shouldn't cause a conflict if only one thread is
            // reading from the queue.
            //
            // We use cas instead of set to make sure that we don't
            // overwrite an insert that happened just beforehand.
            prev.casNext(node, null);
        } else if (headAccess.compareAndSet(this, prev, node)) {
            // prev was the sentinel, now node is the sentinel
            // there's nothing more to do.
        } else {
            // prev and node are just two regular nodes in the queue.
            // skip over next.
            // This isn't atomic, but I think that's okay, because
            // only the consumer should be messing around in the middle
            // of the queue.
            prev.setNext(node.getNext());
        }

        node.item = null;
        return result;
    }

    public E pollMatch(Predicate<Object> pred) {
        Node<E> prev = headAccess.get(this),
                node = prev.getNext();

        while (node != null) {
            // Check if the current node satisfies
            // our predicate.
            if (pred.test(node.item)) {
                return removeNode(prev, node);
            }

            prev = node;
            node = prev.getNext();
        }

        return null;
    }

    public E pollMatch(Predicate<Object> pred, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        Node<E> prev = headAccess.get(this),
                node = prev.getNext();

        for (;;) {
            while (node != null) {
                // Check if the current node satisfies
                // our predicate.
                if (pred.test(node.item)) {
                    return removeNode(prev, node);
                }

                prev = node;
                node = prev.getNext();
            }

            while (node == null) {
                if (nanos <= 0) return null;
                nanos = insertion.awaitNanos(nanos);
                node = prev.getNext();
            }
        }
    }

    public E takeMatch(Predicate<Object> pred) throws InterruptedException {
        Node<E> prev = headAccess.get(this),
                node = prev.getNext();

        for (;;) {
            while (node != null) {
                // Check if the current node satisfies
                // our predicate.
                if (pred.test(node.item)) {
                    return removeNode(prev, node);
                }

                prev = node;
                node = prev.getNext();
            }

            while (node == null) {
                insertion.await();
                node = prev.getNext();
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<E> {
        private Node<E> current;

        public Iter() {
            current = headAccess.get(Mailbox.this);
        }

        @Override
        public boolean hasNext() {
            return current.getNext() != null;
        }

        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException();

            current = current.getNext();
            return current.item;
        }
    }
}
