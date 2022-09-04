package superreferencer;

/*
 * @(#)DefaultListModel.java	1.37 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import pt.unl.fct.di.tsantos.util.collection.CollectionUtilities;


/**
 * This class loosely implements the <code>java.util.Vector</code>
 * API, in that it implements the 1.1.x version of
 * <code>java.util.Vector</code>, has no collection class support,
 * and notifies the <code>ListDataListener</code>s when changes occur.
 * Presently it delegates to a <code>Vector</code>,
 * in a future release it will be a real Collection implementation.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @version 1.37 03/23/10
 * @author Hans Muller
 */
public class EfficientListModel extends AbstractListModel
{
    private List delegate = new LinkedList();

    /**
     * Returns the number of components in this list.
     * <p>
     * This method is identical to <code>size</code>, which implements the
     * <code>List</code> interface defined in the 1.2 Collections framework.
     * This method exists in conjunction with <code>setSize</code> so that
     * <code>size</code> is identifiable as a JavaBean property.
     *
     * @return  the number of components in this list
     * @see #size()
     */
    public int getSize() {
	return delegate.size();
    }

    /**
     * Returns the component at the specified index.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>get(int)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @exception  ArrayIndexOutOfBoundsException  if the <code>index</code>
     *             is negative or greater than the current size of this
     *             list
     * @see #get(int)
     */
    public Object getElementAt(int index) {
	return delegate.get(index);
    }

    /**
     * Copies the components of this list into the specified array.
     * The array must be big enough to hold all the objects in this list,
     * else an <code>IndexOutOfBoundsException</code> is thrown.
     *
     * @param   anArray   the array into which the components get copied
     * @see Vector#copyInto(Object[])
     */
    public void copyInto(Object anArray[]) {
        CollectionUtilities.copyInto(delegate, anArray);
    }

    /**
     * Returns the number of components in this list.
     *
     * @return  the number of components in this list
     * @see Vector#size()
     */
    public int size() {
	return delegate.size();
    }

    /**
     * Tests whether this list has any components.
     *
     * @return  <code>true</code> if and only if this list has
     *          no components, that is, its size is zero;
     *          <code>false</code> otherwise
     * @see Vector#isEmpty()
     */
    public boolean isEmpty() {
	return delegate.isEmpty();
    }

    /**
     * Returns an enumeration of the components of this list.
     *
     * @return  an enumeration of the components of this list
     * @see Vector#elements()
     */
    public Enumeration<?> elements() {
        final Iterator<?> it = delegate.iterator();
        return new Enumeration<Object>() {
	    int count = 0;

	    public boolean hasMoreElements() {
		return it.hasNext();
	    }

	    public Object nextElement() {
		return it.next();
	    }
	};
    }

    /**
     * Tests whether the specified object is a component in this list.
     *
     * @param   elem   an object
     * @return  <code>true</code> if the specified object
     *          is the same as a component in this list
     * @see Vector#contains(Object)
     */
    public boolean contains(Object elem) {
	return delegate.contains(elem);
    }

    /**
     * Searches for the first occurrence of <code>elem</code>.
     *
     * @param   elem   an object
     * @return  the index of the first occurrence of the argument in this
     *          list; returns <code>-1</code> if the object is not found
     * @see Vector#indexOf(Object)
     */
    public int indexOf(Object elem) {
	return delegate.indexOf(elem);
    }

    /**
     * Searches for the first occurrence of <code>elem</code>, beginning
     * the search at <code>index</code>.
     *
     * @param   elem    an desired component
     * @param   index   the index from which to begin searching
     * @return  the index where the first occurrence of <code>elem</code>
     *          is found after <code>index</code>; returns <code>-1</code>
     *          if the <code>elem</code> is not found in the list
     * @see Vector#indexOf(Object,int)
     */
     /*public int indexOf(Object elem, int index) {
	return delegate.indexOf(elem, index);
    }*/

    /**
     * Returns the index of the last occurrence of <code>elem</code>.
     *
     * @param   elem   the desired component
     * @return  the index of the last occurrence of <code>elem</code>
     *          in the list; returns <code>-1</code> if the object is not found
     * @see Vector#lastIndexOf(Object)
     */
    public int lastIndexOf(Object elem) {
	return delegate.lastIndexOf(elem);
    }

    /**
     * Searches backwards for <code>elem</code>, starting from the
     * specified index, and returns an index to it.
     *
     * @param  elem    the desired component
     * @param  index   the index to start searching from
     * @return the index of the last occurrence of the <code>elem</code>
     *          in this list at position less than <code>index</code>;
     *          returns <code>-1</code> if the object is not found
     * @see Vector#lastIndexOf(Object,int)
     */
    /*public int lastIndexOf(Object elem, int index) {
	return delegate.lastIndexOf(elem, index);
    }*/

    /**
     * Returns the component at the specified index.
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
     * is negative or not less than the size of the list.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>get(int)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @see #get(int)
     * @see Vector#elementAt(int)
     */
    public Object elementAt(int index) {
	return delegate.get(index);
    }

    /**
     * Returns the first component of this list.
     * Throws a <code>NoSuchElementException</code> if this
     * vector has no components.
     * @return     the first component of this list
     * @see Vector#firstElement()
     */
    public Object firstElement() {
	return delegate.get(0);
    }

    /**
     * Returns the last component of the list.
     * Throws a <code>NoSuchElementException</code> if this vector
     * has no components.
     *
     * @return  the last component of the list
     * @see Vector#lastElement()
     */
    public Object lastElement() {
	return delegate.get(delegate.size() - 1);
    }

    /**
     * Sets the component at the specified <code>index</code> of this
     * list to be the specified object. The previous component at that
     * position is discarded.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
     * is invalid.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>set(int,Object)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      obj     what the component is to be set to
     * @param      index   the specified index
     * @see #set(int,Object)
     * @see Vector#setElementAt(Object,int)
     */
    public void setElementAt(Object obj, int index) {
        delegate.remove(index);
        delegate.add(index, obj);
	fireContentsChanged(this, index, index);
    }

    /**
     * Deletes the component at the specified index.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
     * is invalid.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>remove(int)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      index   the index of the object to remove
     * @see #remove(int)
     * @see Vector#removeElementAt(int)
     */
    public void removeElementAt(int index) {
	delegate.remove(index);
	fireIntervalRemoved(this, index, index);
    }

    /**
     * Inserts the specified object as a component in this list at the
     * specified <code>index</code>.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
     * is invalid.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>add(int,Object)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      obj     the component to insert
     * @param      index   where to insert the new component
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid
     * @see #add(int,Object)
     * @see Vector#insertElementAt(Object,int)
     */
    public void insertElementAt(Object obj, int index) {
	delegate.add(index, obj);
	fireIntervalAdded(this, index, index);
    }

    /**
     * Adds the specified component to the end of this list.
     *
     * @param   obj   the component to be added
     * @see Vector#addElement(Object)
     */
    public void addElement(Object obj) {
	int index = delegate.size();
	delegate.add(obj);
	fireIntervalAdded(this, index, index);
    }

    /**
     * Adds the specified components to the end of this list.
     *
     * @param   objs   the components to be added
     */
    public void addElements(Object[] objs) {
	int index = delegate.size();
        Collections.addAll(delegate, objs);
	//delegate.add(obj);
	fireIntervalAdded(this, index, index + objs.length);
    }

    /**
     * Adds the specified components to the end of this list.
     *
     * @param   objs   the components to be added
     */
    public void addElements(Collection objs) {
	int index = delegate.size();
        delegate.addAll(objs);
	//delegate.add(obj);
	fireIntervalAdded(this, index, index + objs.size());
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument
     * from this list.
     *
     * @param   obj   the component to be removed
     * @return  <code>true</code> if the argument was a component of this
     *          list; <code>false</code> otherwise
     * @see Vector#removeElement(Object)
     */
    public boolean removeElement(Object obj) {
	int index = indexOf(obj);
	boolean rv = delegate.remove(obj);
	if (index >= 0) {
	    fireIntervalRemoved(this, index, index);
	}
	return rv;
    }


    /**
     * Removes all components from this list and sets its size to zero.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>clear</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @see #clear()
     * @see Vector#removeAllElements()
     */
    public void removeAllElements() {
	int index1 = delegate.size() - 1;
	delegate.clear();
	if (index1 >= 0) {
	    fireIntervalRemoved(this, 0, index1);
	}
    }


    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
    @Override
   public String toString() {
	return delegate.toString();
    }


    /* The remaining methods are included for compatibility with the
     * Java 2 platform Vector class.
     */

    /**
     * Returns an array containing all of the elements in this list in the
     * correct order.
     *
     * @return an array containing the elements of the list
     * @see Vector#toArray()
     */
    public Object[] toArray() {
	return delegate.toArray();
    }

    /**
     * Returns the element at the specified position in this list.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code>
     * if the index is out of range
     * (<code>index &lt; 0 || index &gt;= size()</code>).
     *
     * @param index index of element to return
     */
    public Object get(int index) {
	return delegate.get(index);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code>
     * if the index is out of range
     * (<code>index &lt; 0 || index &gt;= size()</code>).
     *
     * @param index index of element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     */
    public Object set(int index, Object element) {
	Object rv = delegate.remove(index);
        delegate.add(index, element);
	fireContentsChanged(this, index, index);
	return rv;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the
     * index is out of range
     * (<code>index &lt; 0 || index &gt; size()</code>).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     */
    public void add(int index, Object element) {
	delegate.add(index, element);
	fireIntervalAdded(this, index, index);
    }

    /**
     * Removes the element at the specified position in this list.
     * Returns the element that was removed from the list.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code>
     * if the index is out of range
     * (<code>index &lt; 0 || index &gt;= size()</code>).
     *
     * @param index the index of the element to removed
     */
    public Object remove(int index) {
	Object rv = delegate.remove(index);
	fireIntervalRemoved(this, index, index);
	return rv;
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns (unless it throws an exception).
     */
    public void clear() {
	int index1 = delegate.size() - 1;
	delegate.clear();
	if (index1 >= 0) {
	    fireIntervalRemoved(this, 0, index1);
	}
    }

    /**
     * Deletes the components at the specified range of indexes.
     * The removal is inclusive, so specifying a range of (1,5)
     * removes the component at index 1 and the component at index 5,
     * as well as all components in between.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code>
     * if the index was invalid.
     * Throws an <code>IllegalArgumentException</code> if
     * <code>fromIndex &gt; toIndex</code>.
     *
     * @param      fromIndex the index of the lower end of the range
     * @param      toIndex   the index of the upper end of the range
     * @see	   #remove(int)
     */
    public void removeRange(int fromIndex, int toIndex) {
	if (fromIndex > toIndex) {
	    throw new IllegalArgumentException("fromIndex must be <= toIndex");
	}
	for(int i = toIndex; i >= fromIndex; i--) {
	    delegate.remove(i);
	}
	fireIntervalRemoved(this, fromIndex, toIndex);
    }

    /*
    public void addAll(Collection c) {
    }

    public void addAll(int index, Collection c) {
    }
    */
}
