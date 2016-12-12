/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.MonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.monitor.params.MonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import java.util.List;

public final class TunnelParameter implements MonitorParams{


    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public Class<? extends TunnelTypeBase> getTunnelType() {
        return tunnelType;
    }

    public List<DPNTEPsInfo> getCfgdDpnList() {
        return cfgdDpnList;
    }

    public List<HwVtep> getCfgdHwVteps() {
        return cfgdHwVteps;
    }

    public IpAddress getDestinationIP() {
        return destinationIP;
    }

    @Override
    public Boolean isMonitorEnabled() {
        return isMonitorEnabled;
    }

    @Override
    public Integer getMonitorInterval() {
        return monitorInterval;
    }

    @Override
    public Class<? extends TunnelMonitoringTypeBase> getMonitorProtocol() {
        return monitorProtocol;
    }

    @Override
    public List<MonitorConfig> getMonitorConfig() {
        return monitorConfigList;
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return  TunnelParameter.class;
    }

    public IdManagerService getIdManagerService() {
        return idManagerService;
    }

    private final IdManagerService idManagerService;
    private final DataBroker dataBroker;
    private final Class<? extends TunnelTypeBase> tunnelType;
    private final List<DPNTEPsInfo> cfgdDpnList;
    private final List<HwVtep> cfgdHwVteps;
    private final IpAddress destinationIP;
    private final Integer monitorInterval;
    private final boolean isMonitorEnabled;
    private final Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
    private final List<MonitorConfig> monitorConfigList;


    private TunnelParameter(Builder builder){
        this.idManagerService = builder.idManagerService;
        this.dataBroker = builder.dataBroker;
        this.tunnelType = builder.tunnelType;
        this.cfgdDpnList = builder.cfgdDpnList;
        this.cfgdHwVteps = builder.cfgdHwVteps;
        this.destinationIP = builder.destinationIP;
        this.monitorInterval = builder.monitorInterval;
        this.isMonitorEnabled = builder.isMonitorEnabled;
        this.monitorProtocol = builder.monitorProtocol;
        this.monitorConfigList = builder.monitorConfigList;

    }

    public static class Builder {

        public Builder setIdManagerService(IdManagerService idManagerService) {
            this.idManagerService = idManagerService;
            return this;
        }

        public Builder setDataBroker(DataBroker dataBroker) {
            this.dataBroker = dataBroker;
            return this;
        }

        public Builder setTunnelType(Class<? extends TunnelTypeBase> tunnelType) {
            this.tunnelType = tunnelType;
            return this;
        }

        public Builder setCfgdDpnList(List<DPNTEPsInfo> cfgdDpnList) {
            this.cfgdDpnList = cfgdDpnList;
            return this;
        }

        public Builder setCfgdHwVteps(List<HwVtep> cfgdHwVteps) {
            this.cfgdHwVteps = cfgdHwVteps;
            return this;
        }

        public Builder setDestinationIP(IpAddress destinationIP) {
            this.destinationIP = destinationIP;
            return this;
        }

        public Builder setMonitorInterval(Integer monitorInterval) {
            this.monitorInterval = monitorInterval;
            return this;
        }

        public Builder setMonitorEnabled(boolean monitorEnabled) {
            this.isMonitorEnabled = monitorEnabled;
            return this;
        }

        public Builder setMonitorProtocol(Class<? extends TunnelMonitoringTypeBase> monitorProtocol) {
            this.monitorProtocol = monitorProtocol;
            return this;
        }

        public Builder setMonitorConfig(List<MonitorConfig> monitorConfigList){
            this.monitorConfigList = monitorConfigList;
            return this;
        }

        public TunnelParameter build(){
            return new TunnelParameter(this);
        }

        private IdManagerService idManagerService;
        private DataBroker dataBroker;
        private Class<? extends TunnelTypeBase> tunnelType;
        private List<DPNTEPsInfo> cfgdDpnList;
        private List<HwVtep> cfgdHwVteps;
        private IpAddress destinationIP;
        private Integer monitorInterval;
        private boolean isMonitorEnabled;
        private Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
        private List<MonitorConfig> monitorConfigList;
    }
}