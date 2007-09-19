/*
 * TreeIter.java
 *
 * Copyright (c) 2006 Operational Dynamics Consulting Pty Ltd
 * 
 * The code in this file, and the library it is a part of, are made available
 * to you by the authors under the terms of the "GNU General Public Licence,
 * version 2" plus the "Classpath Exception" (you may link to this code as a
 * library into other programs provided you don't make a derivation of it).
 * See the LICENCE file for the terms governing usage and redistribution.
 */
package org.gnome.gtk;

import org.gnome.glib.Boxed;

/**
 * A temporary pointer to a row in a TreeModel. TreeIters are used to indicate
 * a row in a TreeModel, either the "current" row if you are iterating over
 * the data, or as an indication of which row a given event occurred on.
 * 
 * <p>
 * To obtain a new TreeIter, use one of the following:
 * <ul>
 * <li>TreeModel's
 * {@link org.gnome.gtk.TreeModel#getIterFirst() getIterFirst()}.
 * <li>TreeSelection's ...
 * </ul>
 * 
 * <p>
 * Like other iterators in Java, a TreeIter becomes invalid the moment the
 * underlying model changes. If you need a persistent pointer to a particular
 * row, create a {@link TreeRowReference} with FIXME a utility method here?
 * 
 * @author Andrew Cowie
 */
public class TreeIter extends Boxed
{
    protected TreeIter(long pointer) {
        super(pointer);
    }

    /**
     * Allocate a blank TreeIter structure. This is done by declaring one
     * locally, copying it, and returning the pointer to the copy.
     * 
     * <p>
     * <b>For use by bindings hackers only!</b>
     */
    TreeIter() {
        super(GtkTreeIterOverride.createTreeIter());
    }

    protected void release() {
        GtkTreeIter.free(this);
    }
}
