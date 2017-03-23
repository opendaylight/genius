/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A sorting {@link Comparator} for {@link List}s. The contents of the lists being
 * compared are sorted by the natural order of the element before the
 * comparison.
 *
 * @author Michael Vorburger.ch
 */
public final class SortingListComparator extends ListComparator {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int compare(List lefts, List rights) {
        List/*<T>*/ sortedLefts = Lists.newArrayList(lefts);
        Collections.sort(sortedLefts);
        List/*<T>*/ sortedRights = Lists.newArrayList(rights);
        Collections.sort(sortedRights);
        return super.compare(sortedLefts, sortedRights);
    }
}
