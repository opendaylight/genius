/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubnetObject {
    private IpAddress gatewayIp;
    private SubnetsKey key;
    private IpPrefix prefix;
    private java.lang.Integer vlanId;
    private static final Logger LOG = LoggerFactory.getLogger(SubnetObject.class);

    public SubnetObject(IpAddress gwIP, SubnetsKey key, IpPrefix mask, Integer vlanId) {
        gatewayIp = gwIP;
        this.key = key;
        this.prefix = mask;
        try {
            if (vlanId != null) {
                checkVlanIdRange(vlanId);
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid VlanID. expected: 0 to 4095");
        }
        this.vlanId = vlanId;
    }

    public IpAddress get_gatewayIp() {
        return gatewayIp;
    }

    public SubnetsKey get_key() {
        return key;
    }

    public IpPrefix get_prefix() {
        return prefix;
    }

    public java.lang.Integer get_vlanId() {
        return vlanId;
    }

    private int hash = 0;
    private volatile boolean hashValid = false;

    @Override
    public int hashCode() {
        if (hashValid) {
            return hash;
        }

        final int prime = 31;
        int result = 1;
        result = prime * result + ((gatewayIp == null) ? 0 : gatewayIp.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        result = prime * result + ((vlanId == null) ? 0 : vlanId.hashCode());
        hash = result;
        hashValid = true;
        return result;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubnetObject)) {
            return false;
        }
        SubnetObject other = (SubnetObject) obj;
        if (gatewayIp == null) {
            if (other.get_gatewayIp() != null) {
                return false;
            }
        } else if (!gatewayIp.equals(other.get_gatewayIp())) {
            return false;
        }
        if (key == null) {
            if (other.get_key() != null) {
                return false;
            }
        } else if (!key.equals(other.get_key())) {
            return false;
        }
        if (prefix == null) {
            if (other.get_prefix() != null) {
                return false;
            }
        } else if (!prefix.equals(other.get_prefix())) {
            return false;
        }
        if (vlanId == null) {
            if (other.get_vlanId() != null) {
                return false;
            }
        } else if (!vlanId.equals(other.get_vlanId())) {
            return false;
        }
        return true;
    }

    @Override
    public java.lang.String toString() {
        java.lang.StringBuilder builder = new java.lang.StringBuilder("Subnets [");
        boolean first = true;

        if (gatewayIp != null) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append("gatewayIp=");
            builder.append(gatewayIp);
        }
        if (key != null) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append("key=");
            builder.append(key);
        }
        if (prefix != null) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append("prefix=");
            builder.append(prefix);
        }
        if (vlanId != null) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append("vlanId=");
            builder.append(vlanId);
        }
        return builder.append(']').toString();
    }

    private static void checkVlanIdRange(final int value) {
        if (value >= 0 && value <= 4095) {
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid range: %s, expected: [[0?4095]].", value));
    }
}
