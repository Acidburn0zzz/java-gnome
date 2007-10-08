/*
 * Surface.java
 *
 * Copyright (c) 2007 Operational Dynamics Consulting Pty Ltd
 *
 * The code in this file, and the library it is a part of, are made available
 * to you by the authors under the terms of the "GNU General Public Licence,
 * version 2" plus the "Classpath Exception" (you may link to this code as a
 * library into other programs provided you don't make a derivation of it).
 * See the LICENCE file for the terms governing usage and redistribution.
 */
package org.freedesktop.cairo;

import org.freedesktop.bindings.Proxy;

/**
 * The thing that Cairo will draw on/to. This is the base class for several
 * concrete back ends.
 * 
 * @author Andrew Cowie
 */
public abstract class Surface extends Proxy
{
    protected Surface(long pointer) {
        super(pointer);
    }

    protected void release() {
        CairoSurface.destroy(this);
    }
    
    public void finish() {
        CairoSurface.finish(this);
    }
    
    public void writeToPNG(String filename) {
        CairoSurface.writeToPng(this, filename);
    }
}
