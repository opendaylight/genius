/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.INTERFACE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.arputil.internal.ArpUtilImpl;
import org.opendaylight.infrautils.inject.guice.testutils.GuiceRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class ArpUtilTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule MethodRule guice = new GuiceRule(new ArpUtilTestModule());
    public List<String> lowerLayerIf = new ArrayList<>(Arrays.asList("openflow:1:2"));

    @Inject ArpUtilImpl arpUtil;
    @Inject DataBroker dataBroker;
    @Inject OdlArputilService odlArputilService;

    @Before
    public void start() throws InterruptedException {
        // Create the bridge and make sure it is ready
        configureInterfaces();
    }

    @After
    public void end() throws InterruptedException {
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    private void configureInterfaces() {
        ParentRefs parentRefs = new ParentRefsBuilder().setDatapathNodeIdentifier(ArpUtilTestUtil.DPN_ID).build();
        try {
            ArpUtilTestUtil.putInterfaceConfig(dataBroker, INTERFACE_NAME, parentRefs, L2vlan.class);
            Interface itfaceState = new InterfaceBuilder().setPhysAddress(new PhysAddress("1F:1F:1F:1F:1F:1F"))
                    .setLowerLayerIf(lowerLayerIf).build();
            ArpUtilTestUtil.addStateEntry(INTERFACE_NAME, dataBroker, itfaceState);
        } catch (TransactionCommitFailedException e) {
            System.out.println("transaction failed on configuring interfaces");
        }
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "checkstyle:RegexpSinglelineJava"})
    @Test
    public void testGetMac() {

        final InterfaceAddress interfaceAddress = new InterfaceAddressBuilder().setInterface(INTERFACE_NAME)
                .setIpAddress(new IpAddress(new Ipv4Address("192.169.0.1")))
                .setMacaddress(new PhysAddress("1F:1F:1F:1F:1F:1F"))
                .build();
        final List<InterfaceAddress> itf = Arrays.asList(interfaceAddress);
        callOnPacketReceived(5000);
        final GetMacInput getMacInput = new GetMacInputBuilder()
                .setIpaddress(new IpAddress(new Ipv4Address("192.169.0.1")))
                .setInterfaceAddress(itf).build();
        Future<RpcResult<GetMacOutput>> output = odlArputilService.getMac(getMacInput);
        try {
            Assert.assertEquals("1F:1F:1F:1F:1F:1F", output.get().getResult().getMacaddress().getValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    //calls onPacketReceived with a delay to avoid getMac from getting stuck
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void callOnPacketReceived(long delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                final PacketReceived packetReceived = ArpUtilTestUtil.createPayload();
                try {
                    arpUtil.onPacketReceived(packetReceived);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, delay);
    }
}
