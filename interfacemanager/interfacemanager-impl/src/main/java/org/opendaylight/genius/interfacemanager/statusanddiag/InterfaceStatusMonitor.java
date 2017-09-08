/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.statusanddiag;

import java.lang.management.ManagementFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusPoller;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OsgiServiceProvider(classes = ServiceStatusPoller.class)
public class InterfaceStatusMonitor implements InterfaceStatusMonitorMBean, ServiceStatusPoller {

    private String serviceName;
    // TODO retaining this for backword compatibility till the whole funcionality is migrated to use the new diagstatus
    private String serviceStatus;
    private ServiceDescriptor serviceDescriptor;
    private static final String JMX_INTERFACE_OBJ_NAME = "com.ericsson.sdncp.services.status:type=SvcInterfaceService";
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStatusMonitor.class);
    private final DiagStatusService diagStatusService;

    @Inject
    private InterfaceStatusMonitor(InterfacemgrProvider interfaceMgrProvider,
                                   final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(IfmConstants.INTERFACE_SERVICE_NAME);
    }

    @PostConstruct
    private void start() {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, ServiceState.OPERATIONAL,"Service started");
        diagStatusService.report(serviceDescriptor);
    }

    @PreDestroy
    private void close() {
        serviceDescriptor = new ServiceDescriptor(IfmConstants.INTERFACE_SERVICE_NAME, ServiceState.UNREGISTERED, "Service Closed");
        diagStatusService.report(serviceDescriptor);
    }

    public void registerMbean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objName = new ObjectName(JMX_INTERFACE_OBJ_NAME);
            LOG.debug("MXBean Object-Name framed");
            mbs.registerMBean(this, objName);
            LOG.info("MXBean registration SUCCESSFUL!!! {}", JMX_INTERFACE_OBJ_NAME);
        } catch (InstanceAlreadyExistsException iaeEx) {
            LOG.error("MXBean registration FAILED with InstanceAlreadyExistsException", iaeEx);
        } catch (MBeanRegistrationException mbrEx) {
            LOG.error("MXBean registration FAILED with MBeanRegistrationException", mbrEx);
        } catch (NotCompliantMBeanException ncmbEx) {
            LOG.error("MXBean registration FAILED with NotCompliantMBeanException", ncmbEx);
        } catch (MalformedObjectNameException monEx) {
            LOG.error("MXBean registration FAILED with MalformedObjectNameException", monEx);
        }
    }

    public void unregisterMbean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objName = new ObjectName(JMX_INTERFACE_OBJ_NAME);
            mbs.unregisterMBean(objName);
            LOG.info("MXBean un-registration SUCCESSFUL!!! {}", JMX_INTERFACE_OBJ_NAME);
        } catch (MBeanRegistrationException mbrEx) {
            LOG.error("MXBean un-registration FAILED with MBeanRegistrationException", mbrEx);
        } catch (MalformedObjectNameException monEx) {
            LOG.error("MXBean un-registration FAILED with MalformedObjectNameException", monEx);
        } catch (InstanceNotFoundException e) {
            LOG.debug("MXBean un-registration FAILED with InstanceNotFoundException", e);
        }
    }

    @Override
    public String acquireServiceStatus() {
        return serviceStatus;
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Add logic here to derive the dynamic service state.
        // Currently this is just returning the initial state.
        return serviceDescriptor;
    }

    public void reportStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }
}
