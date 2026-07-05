package logic;

import model.Node;

import java.util.ArrayList;
import java.util.List;

/*
 * 
 * This is a custom minimum heap priority queue
 * It is used by Dijkstra's algorithm to always remove
 * the node with the smallest current distance first
 *
 * Each entry stores:
 * - a node
 * - its priority value (distance)
 */
public class MinHeapPriorityQueue {

    /*
     * 
     * Represents one element in the heap:
     * a node and its priority value
     */
    public static class Entry {
        private final Node node;
        private final int priority;

        public Entry(Node node, int priority) {
            this.node = node;
            this.priority = priority;
        }

        public Node getNode() {
            return node;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Internal heap storage
    private final List<Entry> heap = new ArrayList<>();

    /*
     * @return true if the heap has no elements
     */
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /*
     * 
     * Adds a new entry into the heap
     * and restores heap order upward
     * 
     */
    public void insert(Node node, int priority) {
        heap.add(new Entry(node, priority));
        siftUp(heap.size() - 1);
    }

    /*
     * 
     * Removes and returns the entry
     * with the smallest priority value
     * 
     */
    public Entry extractMin() {
        if (heap.isEmpty()) {
            return null;
        }

        Entry min = heap.get(0);
        Entry last = heap.remove(heap.size() - 1);

        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }

        return min;
    }

    /*
     *
     * Restores heap order after insertion
     * by moving an entry upward
     * 
     */
    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;

            if (heap.get(index).getPriority() >= heap.get(parent).getPriority()) {
                break;
            }

            swap(index, parent);
            index = parent;
        }
    }

    /*
     * 
     * Restores heap order after removal
     * by moving an entry downward
     * 
     */
    private void siftDown(int index) {
        int size = heap.size();

        while (true) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int smallest = index;

            if (left < size && heap.get(left).getPriority() < heap.get(smallest).getPriority()) {
                smallest = left;
            }

            if (right < size && heap.get(right).getPriority() < heap.get(smallest).getPriority()) {
                smallest = right;
            }

            if (smallest == index) {
                break;
            }

            swap(index, smallest);
            index = smallest;
        }
    }

    /*
     * Swaps two entries in the heap
     */
    private void swap(int i, int j) {
        Entry temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}