/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.util.Comparator;

/**
 * A {@link Comparator} for arrays.
 * The elements of the array all have to implement Comparable.
 * @author Michael Vorburger.ch
 */
public class ArrayComparator<T extends Comparable<T>> implements Comparator<T[]> {

    // The code here is identical to the ListComparator,
    // just using length instead of size(), and index instead of iterator.

    @Override
    public int compare(T[] lefts, T[] rights) {
        if (lefts.length != rights.length) {
            return lefts.length < rights.length ? -1 : 1;
        } else {
            for (int i = 0; i < lefts.length; i++) {
                Comparable<T> left = lefts[i];
                Comparable<T> right = rights[i];
                @SuppressWarnings("unchecked")
                int compare = left.compareTo((T) right);
                if (compare != 0) {
                    return compare;
                }
            }
        }
        return 0;
    }

    // Here are some convenient adapters for primitive types

    public static final Comparator<long[]> LONG = (lefts, rights) -> {
        if (lefts.length != rights.length) {
            return lefts.length < rights.length ? -1 : 1;
        } else {
            for (int i = 0; i < lefts.length; i++) {
                long left = lefts[i];
                long right = rights[i];
                int compare = Long.compare(left,  right);
                if (compare != 0) {
                    return compare;
                }
            }
        }
        return 0;
    };

}
