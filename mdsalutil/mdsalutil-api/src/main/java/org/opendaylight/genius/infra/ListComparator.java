/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link Comparator} for {@link List}s.
 * The elements of the list all have to implement Comparable.
 * @author Michael Vorburger.ch
 */
//TODO ListComparator<T extends Comparable<T>> implements Comparator<List<T>> {
public class ListComparator implements Comparator<List<?>> {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    // TODO public int compare(List<T> lefts, List<T> rights) {
    public int compare(List lefts, List rights) {
        if (lefts.size() != rights.size()) {
            return lefts.size() < rights.size() ? -1 : 1;
        } else {
            for (Iterator/*<T>*/<Comparable> leftIterator = lefts.iterator(),
                             rightIterator = rights.iterator(); leftIterator.hasNext();) {
                Comparable/*T*/ left = leftIterator.next();
                Comparable/*T*/ right = rightIterator.next();
                int compare = left.compareTo(right);
                if (compare != 0) {
                    return compare;
                }
            }
        }
        return 0;
    }

}
