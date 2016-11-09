/*
 * Copyright (c) 2016 RedHat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

/**
 *  This interface helps in creating the openflow matches.
 */

public interface MatchInfoBase {

    /**
     * Creater the inner match object
     *
     * @param mapMatchBuilder the map which holds the matches.
     */
    void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder);

    /**
     * Set the match to the match builder.
     * @param matchBuilder the matchbuilder to set the match
     * @param mapMatchBuilder the map containing the matches
     */
    void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder);

}
