/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Unit test for {@link ActionDrop}.
 *
 * @author Michael Vorburger.ch
 */
public class ActionDropTest {

    /**
     * ActionDrop's equals works, even though it's not overriden (because
     * ActionInfo's equals() has a getClass() != other.getClass()).
     */
    @Test
    public void testEqualsToAnotherClass() {
        assertThat(new ActionDrop()).isNotEqualTo(new ActionMoveShaToTha());
        assertThat(new ActionMoveShaToTha()).isNotEqualTo(new ActionDrop());
    }

    @Test
    public void testEqualsAndHashCode() {
        new EqualsTester()
            .addEqualityGroup(new ActionDrop(), new ActionDrop())
            .addEqualityGroup(new ActionDrop(123), new ActionDrop(123))
            .addEqualityGroup(new ActionMoveShaToTha(), new ActionMoveShaToTha())
            .testEquals();
    }

}
