/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import static org.immutables.value.Value.Style.ImplementationVisibility.PRIVATE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

/**
 * <a href="http://immutables.org">Immutables.org</a> style meta annotation.
 *
 * @author Michael Vorburger.ch
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Value.Style(
        // generate *Builder as public top level type and the *Immutable impl. as private inner class
        visibility = PRIVATE,
        // we use setter-like convention instead of Immutables.org default (no set* prefix)
        // because this is makes it look closer to the YANG binding gen. code, and also
        // because the Xtend literals can use the nicer to read = instead of set*(..);
        // the down-side is that in Java chained Builder initialization it's more ugly.
        init = "set*",
        // It would be neat to able to use strictBuilder = true, BUT:
        // a) that prevents generation of from() copy methods, which are quite handy
        //    (unless https://github.com/immutables/immutables/issues/595 gets implemented)
        // b) that prevents generation of init/setters for collections, which (as above)
        //    makes it look closer to the YANG binding gen. code which has this (but not addAll),
        //    because the Xtend literals can use the nicer to read = instead of addAll*(..)
        //    (unless https://github.com/immutables/immutables/issues/596 gets implemented)
        strictBuilder = false)
public @interface OpenDaylightImmutableStyle { }
// Beware: Changes made here are not active without a restart in Eclipse (would need separate project)
