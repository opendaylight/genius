/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.MoreObjects;
import com.google.common.testing.EqualsTester;
import java.util.Objects;
import org.junit.Test;
import org.opendaylight.genius.utils.MoreObjects2;

public class MoreObjects2Test {

    @Test
    public void test() {
        new EqualsTester()
                .addEqualityGroup(new Thing("hello", 123), new Thing("hello", 123))
                .addEqualityGroup(new Thing("hoi", 123), new Thing("hoi", 123))
                .addEqualityGroup(new Thing("hoi", null))
                .addEqualityGroup(new Thing(null, null))
                .testEquals();
    }

    static class Thing {

        String name;
        Integer age;

        @Override
        public boolean equals(Object obj) {
            return MoreObjects2.equalsHelper(this, obj,
                    (a, b) -> Objects.equals(a.name, b.name) && Objects.equals(a.age, b.age));
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("name", name).add("age", age).toString();
        }

        Thing(String name, Integer age) {
            super();
            this.name = name;
            this.age = age;
        }

    }

}
