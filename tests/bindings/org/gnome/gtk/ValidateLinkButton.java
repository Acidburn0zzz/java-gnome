/*
 * ValidateLinkButton.java
 *
 * Copyright (c) 2009 Operational Dynamics Consulting Pty Ltd, and Others
 * 
 * The code in this file, and the suite it is a part of, are made available
 * to you by the authors under the terms of the "GNU General Public Licence,
 * version 2" See the LICENCE file for the terms governing usage and
 * redistribution.
 */
package org.gnome.gtk;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test LinkButton's visited and uri properties.
 * 
 * @author Serkan Kaba
 */
public class ValidateLinkButton extends TestCaseGtk
{
    public final void testLinkButton() {
        URI java_gnome = null;
        final LinkButton link;

        try {
            java_gnome = new URI("http://java-gnome.sourceforge.net");
        } catch (URISyntaxException e) {
            fail("The URI should be valid");
        }

        link = new LinkButton(java_gnome);

        link.setUriHook(new LinkButton.UriHook() {

            public void onUriClicked(LinkButton source, URI uri) {
            // No-op
            }
        });

        assertFalse(link.getVisited());
        link.emitClicked();
        assertTrue(link.getVisited());
        link.setVisited(false);
        assertFalse(link.getVisited());
        assertEquals(java_gnome, link.getUri());
    }
}
