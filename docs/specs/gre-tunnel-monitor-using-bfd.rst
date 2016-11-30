===============================
GRE Tunnel Monitoring using BFD
===============================

https://git.opendaylight.org/gerrit/#/q/topic:bfd-for-gre

DC-GW tunnel detection is a key feature that is required for fast failover / rerouting in conjunction with multi-path ECMP forwarding in the dataplane. This feature implements BFD-based tunnel monitoring between OVS and the DC Gateway

Problem description
===================

This Feature is a key requirement to enable  load balancing of traffic in a cloud and also redundancy of paths for resiliency in cloud.

Use Cases
---------
-  Configure BFD tunnel monitoring during GRE Tunnel creation
-  Handle BFD tunnel status notification from switch
-  Notify the tunnel status to the subscriber e.g ECMP based load balancing
-  Raise alarm based on the tunnel status
-  Configure tunnel detection multiplier through restconf on the yang
-  Configure tunnel intervals and multiplier as part of external tunnel RPC calls
-  Configure tunnel detection multiplier and interval for all the GRE Tunnel in the controller


Proposed change
===============
Flow Changes
------------
BFD Tunnel Monitoring using OVSDB
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Operator will use the RPCs buildExternalTunnelFromDpns and addExternalTunnelEndpoint on ITM module to create L3GRE.Tunnel monitor intervals and multiplier can be passed as part of input.ITM module will consider the values passed by the operator through RPC input.If it is not sent then value configured in the itm-config datastore will be consider.ITM workers will configure monitor parameters and multipliers for the external tunnel with tunnel type equals to L3GRE on the tunnel interface. Interface Manager uses OVSDB plugin to create tunnels on OVS.  OVS supports BFD based tunnel monitoring which can be enabled using OVSDB southbound plugin. Whenever interface manager receives REST request to configure a tunnel interface, it retrieves the tunnel-monitoring-type and can populate the OVSDB Topology Config DataStore with the Tunnel monitoring details as follows:

.. code-block:: none
   :caption: code snippet
           :emphasize-lines: 1-10

           tpAugmentationBuilder.setName(portName);
           tpAugmentationBuilder.setInterfaceType(type);
           options.put("key", "flow");
           options.put("local_ip", localIp.getIpv4Address().getValue());
           options.put("remote_ip", remoteIp.getIpv4Address().getValue());
           List<InterfaceBfd> bfdParams = new ArrayList<>();
           bfdParams.add(getIfBfdObj(BFD_PARAM_ENABLE, Boolean.toString(ifTunnel.isMonitorEnabled())));
           bfdParams.add(getIfBfdObj(BFD_PARAM_INTERVAL,ifTunnel.getMonitorInterval().toString()))
           tpAugmentationBuilder.setInterfaceBfd(bfdParams);
           tpAugmentationBuilder.setOptions(options);


        OVSDB plugin acts upon this data change and configures the tunnel end points on the switch with the supplied information. BFD will be configured over L3-GRE tunnels using OVSDB bfd columns in interface table of OVSDB on the switch. switch sends BFD packets on the tunnel end point, and waits for response from the other end.  Switch notifies to CSC through ovsdb notification. Interface-manager updates the interface operational status based on the received event.CSC will discover tunnel failures by completely leveraging existing OVSDB Plugin, Interface manager and ITM event signaling work flow. JMX alarms are raised or cleared based on the tunnel interface state from ITM.


Configuring Tunnel multiplier on CSS for L3GRE tunnel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Itm-config yang file have tunnel-monitor-multiplier container. It is configured by the restconf ``/config/itm-config:tunnel-monitor-multiplier/`` or at the bootup by ITMManager . Listener on these yang changes will modify the multiplier information on all L3GRE tunnels.

Interface listener will configure the changes of multiplier on the switch using OVSDB plugin. Whenever interface manager receives REST request to configure a tunnel interface, it retrieves the tunnel-monitoring-type and can populate the OVSDB Topology Config Data Store. This new value will be transmitted with the next BFD Control packet.

Pipeline changes
----------------
NA

Yang changes
------------
ITM-config Yang Changes
^^^^^^^^^^^^^^^^^^^^^^^
Following container will be added to ``itm-config.yang`` for tunnel failure detection multiplier under the module itm-config.default value for the multiplier is configured to 3 and there is a range from 1 to 1000 for this parameter.

.. code-block:: none
   :caption: itm-config.yang
           :emphasize-lines: 1-8

           container tunnel-monitor-multiplier {
                leaf multiplier{
                    type uint8;
                    default 3;
                }
           }

ovsdb Yang Changes
^^^^^^^^^^^^^^^^^^
Tunnel detection multiplier parameter is added to the interface-bfd in ``ovsdb.yang``. bfd: bfd : bfd_multiplier: optional string, containing an integer, at least 1. The new value will be transmitted with the next BFD Control packet, and the use of a Poll Sequence is not necessary. The default is 3";

Itm RPC yang changes
^^^^^^^^^^^^^^^^^^^^
Tunnel monitoring intervals and multiplier will be optional input parameter for the add-external-tunnel-endpoint and build-external-tunnel-from-dpns.Here is the updated yang for these rpcs

.. code-block:: none
   :caption: itm.yang
           :emphasize-lines: 15-24,38-47

           rpc build-external-tunnel-from-dpns {
                    description "used for building tunnels between a Dpn and external node";
                    input {
                        leaf-list dpn-id {
                            type uint64;
                        }
                        leaf destination-ip {
                            type inet:ip-address;
                        }
                        leaf tunnel-type {
                            type identityref {
                                base odlif:tunnel-type-base;
                            }
                        }
                        leaf tunnel-monitor-interval {
                            type uint16;
                        }
                        leaf tunnel-monitor-multiplier{
                            type uint8;
                        }
                    }
           }
           rpc add-external-tunnel-endpoint {
                        description "used for building tunnels between teps on all Dpns and external node";
                        input {
                            leaf destination-ip {
                                 type inet:ip-address;
                            }
                            leaf tunnel-type {
                                type identityref {
                                    base odlif:tunnel-type-base;
                                }
                            }
                            leaf tunnel-monitor-interval {
                                type uint16;
                            }
                            leaf tunnel-monitor-multiplier{
                                type uint8;
                            }

                        }
           }


Configuration impact
--------------------
``bfd : bfd_multiplier`` added to the ovsdb yang file ``default value is 3``.This value can be configured from restconf and there is no impact on the existing deployment as switches support bfd based tunnel monitoring has hard coded value.

Clustering considerations
-------------------------
NA

Other Infra considerations
--------------------------
NA

Security considerations
-----------------------
NA

Scale and Performance Impact
----------------------------
As we are using BFD tunnel monitoring which is supported at switch side. since tunnel-monitoring is offloaded to the datapath nodes, the SDN control plane does not need to intervene with the benefit that the solution is scalable and robust.
Targeted Release
----------------
ODL-carbon

Alternatives
------------
There was an alternative to use stateless tunnel monitoring approach using GRE-KA, which required the control plane to get involved in sending and receiving each keep-alive packet, thereby resulting in scalability bottlenecks. This is particularly impactful since DCGW outage could affect external connectivity from all vswitches.This is similar to LLDP monitoring.

Usage
=====
- When a GRE tunnel is created /deleted due to addition/removal of gateway from the network.
- When a operator tried to fine tune bfd detection multiplier for the GRE tunnel.

Features to Install
-------------------
odl-genius ,odl-ovsdb-openstack


REST API
--------
Itm-config yang file have tunnel-monitor-multiplier container. It is configured by the restconf ``/config/itm-config:tunnel-monitor-multiplier/``


CLI
---
A new command is added to configure bfd multiplier i.e ``tep:bfd-monitor-multiplier`` for BFD based tunnel monitoring.

.. code-block:: none
  
  DESCRIPTION
    tep:bfd-monitor-multiplier
    configure tunnel multiplier

  SYNTAX
    tep:bfd-monitor-multiplier [multiplier]

  ARGUMENTS
    multiplier
    Tunnel multiplier used for BFD Monitoring

L3GRE tunnel information are displayed as part of ``tep:show`` command

Implementation
==============

Assignee(s)
-----------
Primary assignee:

<sathish kumar b t>

Other contributors:
NA


Work Items
----------
https://trello.com/c/jN8SdZPr/38-gre-tunnel-monitoring-using-bfd


Dependencies
============

This should also capture impacts on existing project that depend on Genius.
switch support is required for configuring bfd tunnel multiliper
DC gateway should support BFD based tunnel monitoring

Following projects currently depend on Genius:
Netvirt

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
Following Junit class is updated

- ItmExternalTunnelAddTest
    testBuildTunnelsFromDpnToExternalEndPoint and testBuildTunnelsToExternalEndPoint methods are updated to test against itm-state/external-tunnel-list/external-tunnel data.Monitor parameters like interval,enabling monitoring,multiplier paramers are updated in stubed interface and checked against the external tunnel data.
- ItmManagerRpcserviceTest
    Adding new test cases testTunnelBetweenCSSAndDCGW to test l3 gre tunnel flow .

Integration Tests
-----------------
TBD

CSIT
----
TBD

Documentation Impact
====================
NA

References
==========
None
