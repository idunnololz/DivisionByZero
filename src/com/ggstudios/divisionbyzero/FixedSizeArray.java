/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ggstudios.divisionbyzero;

import java.util.Comparator;

import com.ggstudios.utils.DebugLog;

/**
 * FixedSizeArray is an alternative to a standard Java collection like ArrayList.  It is designed
 * to provide a contiguous array of fixed length which can be accessed, sorted, and searched without
 * requiring any runtime allocation.  This implementation makes a distinction between the "capacity"
 * of an array (the maximum number of objects it can contain) and the "count" of an array
 * (the current number of objects inserted into the array).  Operations such as set() and remove()
 * can only operate on objects that have been explicitly add()-ed to the array; that is, indexes
 * larger than getCount() but smaller than getCapacity() can't be used on their own.
 * @param <T> The type of object that this array contains.
 */
public class FixedSizeArray<T> {
	private static final String TAG = "FixedSizeArray";

	private final T[] contents;
	private int size;

	public FixedSizeArray(int capacity) {
		super();
		// Ugh!  No generic array construction in Java.
		contents = (T[])new Object[capacity];
		this.size = 0; 
	}

	public FixedSizeArray(int capacity, Comparator<T> comparator) {
		super();
		contents = (T[])new Object[capacity];
		size = 0;
	}

	/** 
	 * Inserts a new object into the array.  If the array is full, an assert is thrown and the
	 * object is ignored.
	 */
	public final void add(T object) {
		if(size >= contents.length) {
			try {
				throw new Exception();
			} catch (Exception e) {
				DebugLog.e(TAG, "Array exhausted!", e);
			}
		}
		contents[size] = object;
		size++;
	}

	public final void add(int index, T object) {		
		if(size >= contents.length) {
			try {
				throw new Exception();
			} catch (Exception e) {
				DebugLog.e(TAG, "Array exhausted!", e);
			}
		}
		System.arraycopy(contents, index, contents, index + 1, size - index);
		contents[index] = object;
		size++;
	}

	/** 
	 * Searches for an object and removes it from the array if it is found.  Other indexes in the
	 * array are shifted up to fill the space left by the removed object.  Note that if
	 * ignoreComparator is set to true, a linear search of object references will be performed.
	 * Otherwise, the comparator set on this array (if any) will be used to find the object.
	 */
	public void remove(T object) {
		final int index = indexOf(object);
		if (index != -1) {
			remove(index);
		}
	}

	/** 
	 * Removes the specified index from the array.  Subsequent entries in the array are shifted up
	 * to fill the space.
	 */
	public void remove(int index) {
		// ugh
		if (index < size) {
			for (int x = index; x < size; x++) {
				if (x + 1 < contents.length && x + 1 < size) {
					contents[x] = contents[x + 1];
				} else {
					contents[x]  = null;
				}
			}
			size--;
		}
	}

	/** 
	 * Adds the specified object to the front of the list.
	 */
	public void addToFront(T obj) {
		// shift all elements down...
		for (int x = 0; x < size; x++) {
			contents[x + 1] = contents[x];
		}
		contents[0] = obj;
		size++;
	}

	/**
	 * Removes the last element in the array and returns it.  This method is faster than calling
	 * remove(count -1);
	 * @return The contents of the last element in the array.
	 */
	public T removeLast() {
		T object = null;
		if (size > 0) {
			object = contents[size - 1];
			contents[size - 1] = null;
			size--;
		}
		return object;
	}

	/**
	 * Swaps the element at the passed index with the element at the end of the array.  When
	 * followed by removeLast(), this is useful for quickly removing array elements.
	 */
	public void swapWithLast(int index) {
		T object = contents[size - 1];
		contents[size - 1] = contents[index];
		contents[index] = object;
	}
	
	public void swap(int i1, int i2) {
		T object = contents[i2];
		contents[i2] = contents[i1];
		contents[i1] = object;
	}

	/**
	 * Sets the value of a specific index in the array.  An object must have already been added to
	 * the array at that index for this command to complete.
	 */
	public void set(int index, T object) {
		if (index < size) {
			contents[index] = object; 
		}
	}

	/**
	 * Clears the contents of the array, releasing all references to objects it contains and 
	 * setting its count to zero.
	 */
	public void clear() {
		for (int x = 0; x < size; x++) {
			contents[x] = null;
		}
		size = 0;
	}

	/**
	 * Returns an entry from the array at the specified index.
	 */
	public T get(int index) {
		return contents[index];
	}

	/** 
	 * Returns the raw internal array.  Exposed here so that tight loops can cache this array
	 * and walk it without the overhead of repeated function calls.  Beware that changing this array
	 * can leave FixedSizeArray in an undefined state, so this function is potentially dangerous
	 * and should be used in read-only cases.
	 * @return The internal storage array.
	 */
	public final Object[] getArray() {
		return contents;
	}

	/** Returns the maximum number of objects that can be inserted inot this array. */
	public int getCapacity() {
		return contents.length;
	}

	public int size() {
		return size;
	}

	public int indexOf(Object o) {
		if (o == null) {
			for (int i = 0; i < size; i++)
				if (contents[i]==null)
					return i;
		} else {
			for (int i = 0; i < size; i++)
				if (o.equals(contents[i]))
					return i;
		}
		return -1;
	}
}
