/*
 * Plumbing.java
 *
 * Copyright (c) 2006 Operational Dynamics Consulting Pty Ltd
 * 
 * The code in this file, and the library it is a part of, are made available
 * to you by the authors under the terms of the "GNU General Public Licence,
 * version 2" plus the "Classpath Exception" (you may link to this code as a
 * library into other programs provided you don't make a derivation of it).
 * See the LICENCE file for the terms governing usage and redistribution.
 */
package org.freedesktop.bindings;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Parent of all classes in the translation layer of a bindings library. This
 * class provides the infrastructure (or, "plumbing") that the generated code
 * can access the native values held within Proxy, Constant, etc.
 * 
 * <p>
 * <i><b>No one using developing applications which happen to use bindings
 * based on this package should ever need to see, use, or subclass, this.</b>
 * People hacking on the bindings themselves will end up call generated or
 * crafted methods in the translation layer, but they too will never have to
 * use these mechanisms directly. Note that individual library families will
 * likely subclass this to extend its instance lookup behaviour in a manner
 * appropriate to the type systems in use within those libraries.</i>
 * 
 * @author Andrew Cowie
 * @since 4.0.0
 */
public abstract class Plumbing
{
    protected Plumbing() {}

    /**
     * Every Proxy that gets created gets added to this Map so that if a call
     * down to the native layer returns an object that has already been
     * created Java side that instance is returned rather than creating a new
     * one.
     */
    static final HashMap knownProxies;

    /**
     * When Enums get created, we add them to this Map so we can find an
     * appropriate instance for a given ordinal. The table is two tier:
     * 
     * Class => ArrayList[Constant+]
     */
    /*
     * Neither keys nor values are to be weak references here; we are not
     * unloading Constant classes so the Class keys will stay strongly
     * reachable; the Constant instances themselves will always be present by
     * virtue of their being in this two tier table.
     */
    static final HashMap knownConstants;

    static {
        /*
         * TODO: any particular reason to pick a starting array size?
         * 
         * FIXME! Early on we used WeakHashMap, but that is weak on _keys_
         * only, and in fact we definitely do _not_ want that. We need to
         * switch to weak _values_; we're going to need to wrap and unwrap
         * WeakReference around the Proxies we put as values to achieve that.
         */
        knownProxies = new HashMap();
        knownConstants = new HashMap();
    }

    /*
     * Proxy handling ------------------------------------
     */

    /**
     * When a Proxy is created, it must call this method so that other methods
     * that need that to return that particular Proxy (but, coming from the
     * native side, only know the pointer address) can do so by doing a
     * lookup.
     */
    static final void registerProxy(Proxy obj) {
        knownProxies.put(new Long(obj.pointer), new WeakReference(obj));
    }

    /**
     * Called by the release() function of a Proxy object, this method cleans
     * up any entries of the Proxy in the Plumbings internals.
     */
    static final void unregisterProxy(Proxy obj) {
        knownProxies.remove(new Long(obj.pointer));
    }

    /**
     * Get the memory address which is the location of the Object or Structure
     * that a given Proxy represents. That doesn't mean anything on the Java
     * side so don't try to interpret it - it's for use by the translation
     * layer as they marshal objects through to the native layer.
     * 
     * @return opaque data to be passed to native methods only.
     */
    /*
     * We go to considerable effort to keep this method out of the visibility
     * of public users which is why translation layer code subclass this
     * org.freedesktop.bindings.Pluming which has package visibility of Proxy
     * and Constant. Even more, there's nothing we can do about this being
     * protected, so we choose a method name other than getPointer() to keep
     * it totally of out of view from get<COMPLETE>.
     */
    protected static final long pointerOf(Proxy reference) {
        return reference == null ? 0L : reference.pointer;
    }

    /**
     * Like {@link #pointerOf(Proxy)}, but acts over an array of Proxys.
     * 
     * @return opaque data to be passed to native methods only.
     */
    protected static final long[] pointersOf(Proxy[] references) {
        long[] pointers = new long[references.length];
        for (int i = 0; i < references.length; ++i) {
            pointers[i] = (references[i] == null ? 0L : references[i].pointer);
        }
        return pointers;
    }

    /**
     * Given a pointer, find out if we already have a Proxy for it Java side.
     * 
     * <p>
     * This will return the Proxy instance already created by a concrete
     * constructor if one was created Java side. This is sufficient if you are
     * only using this for Proxies that were created as a result of objects
     * being constructed in Java. If, on the other hand, you are calling this
     * from a native to Java code path, then you need to account for the fact
     * that it is likely that a returned pointer will not yet have a Proxy
     * here. You don't need to override this method with your own
     * <code>instanceFor()</code> implementation so much as implement custom
     * variants: call this method to find out if there is a Proxy already;
     * then if not take appropriate action to create (and in so doing,
     * register) a new Proxy object.
     * 
     * <p>
     * Note that under this architecture, denaturation should <b>not</b>
     * occur because if we created the type, then we will already and always
     * have a reference to it. Regardless if our type is a much derived
     * subclass of whatever the native library's equivalent is, any look up of
     * that pointer will be routed to our Proxy subtype.
     * 
     * <p>
     * <i><b>This must be overridden by any library using these bindings, or
     * you will only be able to get instances for objects created Java side.</b></i>.
     * 
     * @param pointer
     *            opaque memory reference as passed from the C side.
     * @return the instance corresponding to a given pointer, or null if that
     *         pointer isn't registered.
     */
    /*
     * This is a skeleton implementation with the necessary functionality of
     * looking up existing Proxies but nevertheless this is, in effect, an
     * "abstract" method; if you are using java-gnome to wrap a non GLib based
     * library, you will need to implement an instanceFor() that knows how to
     * create appropriate Proxy instances based on something that can be
     * looked up.
     */
    protected static Proxy instanceFor(long pointer) {
        WeakReference ref = (WeakReference) knownProxies.get(new Long(pointer));

        if (ref == null) {
            return null;
        } else {
            return (Proxy) ref.get();
        }
    }

    /**
     * By design, the protected <init>(long) constructors in the various Proxy
     * subclasses are not publicly visible. Unfortunately, that means that
     * Plumbing subclasses in other packages cannot see them. So, we use the
     * trick of calling JNI (where visibility rules are ignored) to create
     * Proxy instances.
     */
    protected static native Proxy createProxy(Class type, long pointer);

    /*
     * Constant handling ----------------------------------
     */

    /**
     * When a Constant (ie, our Enum wrapper) is created, this must be called
     * to ensure the appropriate constant object can be retrieved on request
     * when all that is known is type and ordinal.
     * 
     * <p>
     * Note that there is no need for an <code>unregisterConstant()</code>;
     * once loaded Constant objects are expected to be around for the lifetime
     * of the VM.
     */
    /*
     * TODO It would be cool if we had a way of sizing the map perfectly on
     * allocation.
     */
    static final void registerConstant(Constant obj) {
        final Class type;
        HashMap map;

        type = obj.getClass();

        map = (HashMap) knownConstants.get(type);

        if (map == null) {
            // TODO is 8 a good initial capacity?
            map = new HashMap(8);
            knownConstants.put(type, map);
        }

        map.put(new Integer(obj.ordinal), obj);
    }

    /**
     * Get the ordinal number corresponding to the enum that a given Constant
     * represents. That value is deliberately obscured on the Java side
     * because by itself it doesn't mean anything without an enclosing
     * Constant class and the machinery to handle it. This method is for use
     * by the translation layer only as it marshals objects through to the
     * native layer.
     * 
     * @return opaque data to be passed to native methods only
     */
    /*
     * Like pointerOf(), this is carefully shielded from hackers writing the
     * public API layer of the bindings.
     */
    protected static final int numOf(Constant reference) {
        return reference.ordinal;
    }

    /**
     * Like {@link #numOf(Constant) numOf()} but acts over an array of
     * Constants.
     * 
     * @return opaque data to be passed to native methods only.
     */
    protected static final int[] numsOf(Constant[] references) {
        int[] ordinals = new int[references.length];
        for (int i = 0; i < references.length; ++i) {
            /*
             * Here we need to check for null, as in output parameters
             * we don't want to initialize the array!
             */
            ordinals[i] = references[i] == null ? 0 : references[i].ordinal;
        }
        return ordinals;
    }

    /**
     * Given a Class and an ordinal number, lookup the Constant object that
     * corresponds to that native enum.
     * 
     * @throws IllegalArgumentException
     *             if it can't find a Constant object corresponding to the
     *             Class, ordinal combination you've requested.
     */
    protected static Constant enumFor(Class type, int ordinal) {
        Constant obj;

        obj = getRegisteredConstant(type, ordinal);
        if (obj == null) {
            throw new IllegalArgumentException("You asked for ordinal " + ordinal
                    + " which is not known for the requested Constant type " + type.getName());
        }

        return obj;
    }

    /**
     * Retrieve the ordinals corresponding to several constants, and fill a
     * Constant array with them.
     * 
     * @see #enumFor(Class, int)
     */
    protected static void fillEnumArray(Class type, Constant[] enums, int[] ordinals) {
        for (int i = 0; i < enums.length; ++i) {
            enums[i] = enumFor(type, ordinals[i]);
        }
    }

    /**
     * Given a Class and an ordinal number, try to lookup the Constant object
     * that corresponds to that flag. If there's no registered constant that
     * matches the given ordinal, then it corresponds to a OR'd flag, so a new
     * Constant object is created and registered.
     */
    /*
     * TODO the result of toString should match the ordered
     */
    protected static Flag flagFor(Class type, int ordinal) {
        Flag obj;

        obj = (Flag) getRegisteredConstant(type, ordinal);
        if (obj == null) {
            String name = null;
            for (int i = 1; i != 0; i <<= 1) {
                if ((ordinal & i) != 0) {
                    Constant c = enumFor(type, i);
                    name = (name == null ? "" : name + "|") + c.nickname;
                }
            }
            obj = createFlag(type, ordinal, name);
        }

        return obj;
    }

    /**
     * Retrieve the ordinals corresponding to several flags, and fill a Flag
     * array with them.
     * 
     * @see #flagFor(Class, int)
     */
    protected static void fillFlagArray(Class type, Flag[] flags, int[] ordinals) {
        for (int i = 0; i < flags.length; ++i) {
            flags[i] = flagFor(type, ordinals[i]);
        }
    }

    /**
     * Lookup a Constant that corresponds to the given type and ordinal.
     */
    private static Constant getRegisteredConstant(Class type, int ordinal) {
        final HashMap map;
        Constant obj;

        assert (type != null);

        map = (HashMap) knownConstants.get(type);
        if (map == null) {
            throw new IllegalArgumentException("No Constants of type " + type.getName()
                    + " are registered");
        }

        obj = (Constant) map.get(new Integer(ordinal));
        return obj;
    }

    /**
     * The Constants (both enums and flags), are instantiated and registered
     * at class load time. However, the flags can be combined (OR'ed)
     * together, thus creating new flags at runtime. This method add a new
     * flag, invocating the protected constructor on the given flag class. To
     * prevent visibility problems, we use the trick of calling JNI (where
     * visibility rules are ignored) to create Proxy instances.
     */
    private static native Flag createFlag(Class type, int ordinal, String nickname);
}
