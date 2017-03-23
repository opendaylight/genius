/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.infra.tests;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.genius.infra.ListComparator;
import org.opendaylight.genius.infra.SortingListComparator;

/**
 * Unit Test for {@link SortingListComparator}.
 * @author Michael Vorburger.ch
 */
public class ListComparatorTest {

    List<String> emptyList = ImmutableList.of();
    List<String> listA = ImmutableList.of("A");
    List<String> listB = ImmutableList.of("B");
    List<String> listAB = ImmutableList.of("A", "B");
    List<String> listBA = ImmutableList.of("B", "A");

    @Test
    public void testListComparator() {
        checkListComparator(new ListComparator());
    }

    @Test
    public void testSortingListComparator() {
        SortingListComparator sortingComparator = new SortingListComparator();
        checkListComparator(sortingComparator);
        assertThat(sortingComparator.compare(listAB, listBA)).isEqualTo(0);
    }

    void checkListComparator(ListComparator comparator) {
        assertThat(comparator.compare(emptyList, emptyList)).isEqualTo(0);
        assertThat(comparator.compare(listA, listA)).isEqualTo(0);
        assertThat(comparator.compare(listAB, listAB)).isEqualTo(0);

        assertThat(comparator.compare(listA, emptyList)).isEqualTo(1);
        assertThat(comparator.compare(listAB, listA)).isEqualTo(1);

        assertThat(comparator.compare(listA, listAB)).isEqualTo(-1);
        assertThat(comparator.compare(emptyList, listA)).isEqualTo(-1);
    }
}
