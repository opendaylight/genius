/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.binding.testutils.AssertDataObjects.assertEqualBeans;

import java.math.BigInteger;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Component tests for interface manager.
 *
 * @author Michael Vorburger
 */
public class InterfaceManagerTest {

    public @Rule MethodRule guice = new GuiceRule(new InterfaceManagerTestModule());

    @Inject DataBroker dataBroker;
    @Inject TestIMdsalApiManager mdsalApiManager;

    @Test public void newVlanInterface() throws Exception {
        // 1. Given
        // TODO This is silly, because onSessionInitiated(), or later it's BP
        // equivalent, for clearer testability should just propagate the exception
        assertThat(InterfaceStatusMonitor.getInstance().acquireServiceStatus()).isEqualTo("OPERATIONAL");

        // 2. When
        // i) Vlan interface written to config/ietf-interfaces DS
        Interface vlanInterfaceEnabled = InterfaceManagerTestUtil.buildInterface(
                InterfaceManagerTestUtil.interfaceName, "Test VLAN if1", true, L2vlan.class, BigInteger.valueOf(1));
        InstanceIdentifier<Interface> vlanInterfaceEnabledInterfaceInstanceIdentifier = IfmUtil.buildId(
                InterfaceManagerTestUtil.interfaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, vlanInterfaceEnabledInterfaceInstanceIdentifier, vlanInterfaceEnabled, true);
        tx.submit();

        // TODO Must think about proper solution for better synchronization here instead of silly wait()...
        // TODO use TestDataStoreJobCoordinator.waitForAllJobs() when https://git.opendaylight.org/gerrit/#/c/48061/ is merged
        Thread.sleep(500);

        // 3. Then
        // a) check expected interface-child entry mapping in odl-interface-meta/config/interface-child-info was created
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryInstanceIdentifier = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(new InterfaceParentEntryKey("s1-eth1"),
                        new InterfaceChildEntryKey(vlanInterfaceEnabled.getName()));
        InterfaceChildEntry expectedInterfaceChildEntry = new InterfaceChildEntryBuilder()
                .setKey(new InterfaceChildEntryKey(vlanInterfaceEnabled.getName()))
                .setChildInterface(vlanInterfaceEnabled.getName()).build();
        // TODO Later use nicer abstraction for DB access here.. see ElanServiceTest
        assertEqualBeans(expectedInterfaceChildEntry, dataBroker.newReadOnlyTransaction()
                .read(CONFIGURATION, interfaceChildEntryInstanceIdentifier).checkedGet().get());
        // TODO b./c./d. assert other entries listed in points  listed in Faseela's "Re: Regarding genius junits" email
        // e) check expected flow entries were created
        mdsalApiManager.assertFlows(InterfaceManagerTestExpectedFlowEntries.newVlanInterface());
    }

}
