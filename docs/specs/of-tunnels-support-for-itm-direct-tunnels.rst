
.. contents:: Table of Contents
      :depth: 3

=========================================
OF Tunnels Support For ITM Direct Tunnels
=========================================

https://git.opendaylight.org/gerrit/#/q/topic:of-tunnels

Genius already supports creation of OF-tunnels through interface-manager.
With itm-direct-tunnels, interface-manager is by-passed for all internal tunnel operations,
and in such scenarios, OF tunnels are not currently supported.
This feature adds support for flow based tunnels on top of itm-direct-tunnels
for better scalability. This feature additionally adds support for BFD monitoring on
top of OF-Tunnels enabled end-points on demand basis.
This feature also scopes in creation of ITM groups, as that will enhance the performance
of OF-tunnels.

Problem description
===================

Today when tunnel interfaces are created with itm-direct-tunnels enabled, ITM creates one
OVS port for each tunnel interface i.e. source-destination pair. For N devices
in a TransportZone this translates to N*(N-1) tunnel ports created across all
devices and N-1 ports in each device. This has obvious scale limitations.

Use Cases
---------
This feature will support following use cases:

* Use case 1: User should be able to create OF-tunnels with itm-direct-tunnels
  flag enabled.
* Use case 2: Allow user to specify if they want to use flow based tunnels at
  the time of configuration, at the vtep level.
* Use case 3: Create single OVS Tunnel Interface if flow based tunnels are
  configured for a VTEP.
* Use case 4: Create an ITM group per destination on all DPNs. For simplicity, the same
  group-id can be used across all DPNs to reach a destination.
* Use case 5: Flow based and non flow based tunnels should be able to exist
  in a given transport zone.
* Use case 6: If BFD monitoring is required between two end-points, point-to-point
  tunnels should be created between them, in addition to the default of-tunnel. This
  point-to-point tunnel will be used only for BFD, and the actual traffic will
  still take of-tunnels.
* Use case 7: ITM should maintain a reference count for the number of applications
  who have requested for monitoring, and the p2p tunnel should be deleted only when
  the reference count becomes zero on a disable monitoring RPC request.
* Use case 8: On tep delete of a flow-based vtep, delete the OVS Tunnel Interface.
* Use case 9: On tep delete of a flow-based vtep, where BFD monitoring is enabled,
  ITM has to delete the p2p tunnel created for BFD monitoring.
* Use case 10: On tep add of an already deleted flow-based vtep where monitoring was
  previously enabled, ITM should re-create the p2p tunnel for monitoring.
* Use case 11: On tep delete, update all groups on all DPNs with drop action.
* Use case 12: The ITM group will get deleted only when a scale-in happens with
  an explicit trigger to remove the DPN.
* Use case 13: Applications will get the ITM group-id to program their respective remote
  flows irrespective of the tunnel port being created or not, and hence applications
  can get rid of their current tunnel-state listeners for better performance.
  (Please note that the openflowplugin feature to enable ordered processing of flows pointing
   to groups is pre-requisite for the smooth functioning of the ITM groups)
* Use case 14: Add of-tunnels through CLI
* Use case 15: Delete of-tunnels through CLI
* Use case 16: Display of-tunnels through CLI

Following use cases will not be supported:

* Configuration of flow based and non-flow based tunnels of same type on the same device.
  OVS requires one of the following: ``remote_ip``, ``local_ip``, ``type`` and ``key`` to
  be unique. Currently we don't support multiple local_ip and key is always set to flow.
  So ``remote_ip`` and ``type`` are the only unique identifiers. ``remote_ip=flow``
  is a super set of ``remote_ip=<fixed-ip>`` and we can't have two interfaces with
  all other fields same except this.
* Changing tunnel from one flow based to non-flow based at runtime. Such a
  change will require deletion and addition of tep. This is inline with
  existing model where tunnel-type cannot be changed at runtime.
* Configuration of Source IP for tunnel through flow.
* Since data traffic will not be flowing over the point to point tunnel for BFD monitoring,
  the existing configuration of ``forwarding_if_rx`` which helps in avoiding tunnel flapping
  by making use of data traffic will not be supported.
* Hitless upgrade cannot be supported from a point to point tunnel deployment to of-tunnel
  deployment. The default configuration will remain as point to point tunnel, and user has to
  explicitly switch to of-tunnels after upgrade.
* Monitoring enable/disable is requested by services, which results in creating/deleting corresponding p2p
  tunnel for monitoring. There are scenarios where a service may fail to disable monitoring when it is not needed,
  for example case when a VM migrates from a BFD monitored source DPN to another one, while the controller cluster
  is down. In such scenarios the current solution will not be able to delete the unwanted p2p tunnel created
  on the previous source.

Proposed change
===============

The current OF Tunnels implmentation in genius has already taken care of the major yang model changes
in ITM and IFM yang files to allow flow based tunnels. This change will additionally enable any other
missing pieces for enabling OF-Tunnels through itm-direct-tunnels as well as supporting monitoring between
DPNs when OF-Tunnels is enabled.

Pipeline changes
----------------
Major pipeline changes for OF-Tunnels are alredy covered as part of the existing OFTunnels
implementation. However the same will not work with itm-direct-tunnels as the code
path is different.

ITM will program a group per source, destination DPN pair. This group will have actions
``set_tunnel_dest_ip`` before the ``output:ofport`` action.

When services call ``getEgressActionsForTunnel(), they will get the action to
goto the above programmed group-id.


OVSDB configuration changes
---------------------------
Whenever point to point tunnel is configured for BFD monitoring on a flow-based
source VTEP, an additional parameter of ``dst_port`` needs to be configured
for the tunnel port on the switch, so that OVS can distinguish between the actual
traffic coming over OF Tunnel against the BFD packets coming over the point to point
tunnel.

YANG changes
------------
Yang changes needed in ``itm.yang`` and ``itm-state.yang`` to allow
configuring a tunnel as flow based or not, is already convered by the previous
OF-Tunnels implementation. To support the same through itm-direct-tunnels, some
more yang changes will be needed in ITM as specified below :

ITM YANG changes
^^^^^^^^^^^^^^^^
A new parameter ``option-of-tunnel`` is already added to ``list-vteps`` in itm.yang and
``tunnel-end-points`` in ``itm-state.yang``.

A new container will be added in odl-itm-meta.yang to maintain a mapping of parent-child interfaces.


.. code-block:: none
   :caption: odl-item-meta.yang
   :emphasize-lines: 1-15

    container interface-child-info {
    description "The container of all child-interfaces for an interface.";
        list interface-parent-entry {
            key parent-interface;
            leaf parent-interface {
                type string;
            }

            list interface-child-entry {
                key child-interface;
                leaf child-interface {
                    type string;
                }
            }
        }
    }

A new container will be added to maintain the reference count for bfd monitoring requests from applications:

.. code-block:: none
   :caption: odl-item-meta.yang
   :emphasize-lines: 1-15

    container monitoring-ref-count {
    description "The container for maintaing the reference count for monitoring requests
                 between a src and dst DPN pair";
        config "false"
        list monitored-tunnels {
            key source-dpn destination-dpn;
            leaf source-dpn {
                type uint64;
            }
            leaf destination-dpn {
                type uint64;
            }
            leaf reference-count {
                type uint16;
            }
        }
    }

The key for dpn-teps-state yang will have to be made composite, to include ``monitoring-enabled``
flag too, as this will be needed if bfd-monitoring is enabled on an of-tunnel enabled DPN.

.. code-block:: none
   :caption: itm-state.yang
   :emphasize-lines: 14-24

   container dpn-teps-state {
       list dpns-teps {
           key "source-dpn-id";
           leaf source-dpn-id {
               type uint64;
               mandatory true;
           }

           leaf ip-address {
               type inet:ip-address;
           }
           ..........

           /* Remote DPNs to which this DPN-Tep has a tunnel */
           list remote-dpns {
                key "destination-dpn-id";
                leaf destination-dpn-id {
                    type uint64;
                    mandatory true;
                }

                leaf option-of-tunnel {
                    description "Use flow based tunnels for remote-ip";
                    type boolean;
                    default false;
                }

                leaf monitoring-enabled {
                     type boolean;
                     mandatory true;
                }

                leaf tunnel-name {
                    type string;
                    mandatory true;
                }

ITM RPC changes
^^^^^^^^^^^^^^^

A new RPC will be added to retrieve watch-port for the BFD enabled point-to-point tunnels.
By default all traffic will use the OF Tunnels between a source and destination DPN pair.
But applications like ECMP might want to use the BFD monitoring enabled point to point tunnel
in their pipeline as watch port for implementing liveness, and for such applications this RPC
will be useful.

.. code-block:: none
   :caption: itm-rpc.yang
   :emphasize-lines: 1-20

    rpc get-watch-port-for-tunnel {
        description "retrieve the watch port for the BFD enabled point to point tunnel";
        input {
            leaf source-node {
                type string;
            }

            leaf destination-node {
                type string;
            }

        }
        output {
            leaf port-no {
                type uint32;
            }
            leaf portname {
                type string;
            }
        }
    }

Workflow
--------

Adding TEP
^^^^^^^^^^

#. User: Enables itm-scalability by setting itm-direct-tunnels flag to true
   in genius-ifm-config.xml.
#. User: While adding tep user gives ``option-of-tunnel:true`` for tep being
   added.
#. ITM: If ``option-of-tunnel:true`` for vtep, set ``option:remote_ip=flow``
   when creating tunnel interface in OVSDB. Else, set ``option:remote_ip=<destination-ip>``.
#. ITM: OF Tunnel will be created with a separate destination udp port, so that the BFD traffic can be distinguished
   from the actual data traffic.
#. ITM: Receives notification when the of-port is added on the switch.
#. ITM: Checks for the northbound configured tunnel interfaces on top of this flow based tunnel,
   and creates group for each source-destination pair reachable over this of-tunnel.

Enable BFD between two TEPs
^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. If BFD monitoring is enabled through the ``setBfdParamOnTunnel()`` RPC, additional point to point tunnels will be
   created on the specified source, destination DPNs.
#. ITM increments monitored reference count for the particular source, destination pair.
#. These tunnel end points will be added to the tunnel-state which applications listen to.
#. The state of the point to point tunnels will be still available via ``get-watch-port-for-tunnel``RPC
   for applications who want to use them in their datapath for aliveness.
#. There won't be any flows that will be programmed on the OVS for these point to point tunnels,
   and they will serve the purpose of BFD monitoring alone.

Disable BFD between two TEPs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
#. ITM has to maintain a reference count for the number of applications who have requested for the monitoring.
#. If BFD monitoring is disabled through the ``setBfdParamOnTunnel()`` RPC, ITM will delete the p2p tunnel for
   monitoring only if this is the last service interested in monitoring.

Deleting TEP
^^^^^^^^^^^^

#. If ``tunnel-remote-ip:true`` for vtep, delete tunnel port in OVSDB.
   Also, delete relevant datastores which were populated in ITM.
#. If ``tunnel-remote-ip:false``, follow existing logic.
#. If BFD monitoring is enabled on a flow based VTEP, the point to point tunnel created for monitoring
   also needs to be deleted.
#. The BFD monitoring information will be still maintained in ITM, to enable smooth and transparent TEP re-creation.
#. All remote DPNs will be updated, to add drop action in their ITM group pointing to the deleted TEP.
#. The BFD monitoring information and the empty group corresponding to this TEP will be deleted only
   in a scale-in scenario, where the DPN is explicitly removed.



Configuration impact
---------------------
A configuration parameter will be added to genius-ifm-config.xml to set the value of dst_udp_port
for point to point tunnel for BFD monitoring.

Clustering considerations
-------------------------
Any clustering requirements are already addressed in ITM and IFM, no new
requirements added as part of this feature.

Upgrade Considerations
----------------------
An existing tunnel deployment should not automatically change after an upgrade.
If a deployment has pt-pt tunnels, then that’s what the upgrade will maintain.
The user would then have to set up of tunnels separately and remove the pt-pt tunnel mesh,
so it would amount to downtime.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
This solution will help improve scale numbers by reducing no. of interfaces
created on devices as well as no. of interfaces and ports present in
``inventory`` and ``network-topology``.
ITM will still be maintaining n*(n-1) tunnel-states in its datastore,
so that application logic won't be impacted.

Targeted Release(s)
-------------------
Fluorine.

Known Limitations
-----------------

#. Openflowplugin needs to ensure that the ITM group gets programmed on the switch first, before
   programming any flows that point to this group. This feature is currently not supported in ofp,
   but will be available as part of Fluorine.

#. Hitless upgrade cannot be supported from a point to point tunnel deployment to of-tunnel
   deployment. The default configuration will remain as point to point tunnel, and user has to
   explicitly switch to of-tunnels after upgrade.

#. Since data traffic will not be flowing over the point to point tunnel for BFD monitoring,
   the existing configuration of ``forwarding_if_rx`` which helps in avoiding tunnel flapping
   by making use of data traffic will not be supported.

Alternatives
------------
LLDP/ARP based monitoring was considered for OF tunnels to overcome lack of BFD
monitoring but was rejected because LLDP/ARP based monitoring doesn't scale
well. Since driving requirement for this feature is scale setups, it didn't
make sense to use an unscalable solution for monitoring.

Even BFD monitoring with point to point tunnel may not scale if all O(n**2).
Hence this whole proposal is about need based monitoring to reduce the monitored set of tunnels to reduce it to a small
subset of O(n**2) tunnels. LLDP & ARP might scale enough for the subset.

Using point to point tunnel itself for Data Traffic whenever BFD monitoring gets enabled
was discussed, however since all applications are currently using the destination port number in their flows,
it will add additional complexity of updating all application flows with the new port number, the moment
point to point tunnel is created to override OF-tunnels. Hence this option was discarded.


Usage
=====
Features to Install
-------------------
This feature doesn't add any new karaf feature.

User can use this feature via three options - REST, CLI or Auto-Tunnel Configuration.

REST API
--------

Adding TEPs to transport zone
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

For most users TEP Addition is the only configuration they need to do to create
tunnels using genius. The REST API to add TEPs with OF Tunnels is same as earlier.

**URL:** restconf/config/itm:transport-zones/

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 14

   {
    "transport-zone": [
        {
            "zone-name": "TZA",
            "subnets": [
                {
                    "prefix": "192.168.56.0/24",
                    "vlan-id": 0,
                    "vteps": [
                        {
                            "dpn-id": "1",
                            "portname": "eth2",
                            "ip-address": "192.168.56.101",
                            "option-of-tunnel":"true"
                        }
                    ],
                    "gateway-ip": "0.0.0.0"
                }
            ],
            "tunnel-type": "odl-interface:tunnel-type-vxlan"
        }
    ]
   }

CLI
---

A new boolean option, ``remoteIpFlow`` will be added to ``tep:add`` command.

.. code-block:: none
  :emphasize-lines: 7,24-25

  DESCRIPTION
    tep:add
    adding a tunnel end point

  SYNTAX
    tep:add [dpnId] [portNo] [vlanId] [ipAddress] [subnetMask] [gatewayIp] [transportZone]
    [remoteIpFlow]

  ARGUMENTS
    dpnId
            DPN-ID
    portNo
            port-name
    vlanId
            vlan-id
    ipAddress
            ip-address
    subnetMask
            subnet-Mask
    gatewayIp
            gateway-ip
    transportZone
            transport_zone
    remoteIpFlow
            Use flow for remote ip


ITM AUTO-TUNNELS
----------------

ITM already supports automatic configuration of of-tunnels.
Details on how to configure the same can be found under the references section.

Implementation
==============

Assignee(s)
-----------
Primary assignee:
  <Faseela K>

Other contributors:
  <Dimple Jain>
  <Nidhi Adhvaryu>
  <N Edwin Anthony>
  <B Sathwik>


Work Items
----------
#. YANG changes
#. Create ITM groups per destination
#. Update ITM groups per destination with drop action on TEP delete.
#. Delete ITM group only while scale-in.
#. Create OF-port on OVS only for the first tunnel getting configured, if ``of-tunnel`` is true.
#. Create point to point tunnel on OVS, when monitoring has to be enabled between two Flow Based DPNs.
#. Add option for configuring ``dst_port`` for point to point tunnels.
#. Add configuration option for dst_udp_port.
#. Skip flow configuration for point to point tunnels configured on top of flow-based VTEP.
#. Add ``goto_group`` action to actions returned in
   ``getEgressActionsForTunnel()`` for OF Tunnels.
#. Add match on ``tun_src_ip`` in **Table0** for OF Tunnels.
#. Maintain reference count for applications requesting for BFD monitoring.
#. Migrate ``setBfdParamOnTunnel()`` RPC as a routed RPC to ensure synchronized updation of reference count.
#. Transparently handle monitored p2p tunnel deletion, in case of flow based tunnel deletion.
#. Transparently handle monitored p2p tunnel addition, in case of flow based tunnel re-addition.
#. Add CLI.
#. Add UTs.
#. Add scale tests and compare the performance numbers against p2p tunnels.
#. Add CSIT.
#. Add Documentation

Dependencies
============
This doesn't add any new dependencies. This requires minimum of ``OVS 2.0.0``
which is already lower than required by some of other features.

This change is backwards compatible, so no impact on dependent projects.
Projects can choose to start using this when they want. However, there is a
known limitation with monitoring, refer Limitations section for details.

Following projects currently depend on Genius:

* Netvirt
* SFC

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in once framework is in place.

Integration Tests
-----------------
N/A

CSIT
----

Following test cases will need to be added/expanded in Genius CSIT:

#. Enhance Genius CSIT to support 3 switches
#. Create a TZ with more than one TEPs set to use OF Tunnels.
#. Delete a TZ with more than one TEPs set to use OF Tunnels.
#. Delete a TEP using OF Tunnels and add it again with non OF tunnels.
#. Delete a TEP using non OF Tunnels and add it again with OF Tunnels.
#. Enable BFD monitoring on an OF Tunnel enabled src, dest DPN pair.
#. Disable BFD monitoring on an OF Tunnel enabled src, dest DPN pair.
#. Enable auto-config and test the of-tunnels feature.

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information on how to add TEPs with flow based
tunnels.

Developer Guide will need to capture how to use changes in ITM to create
individual tunnel interfaces.

References
==========

* https://jira.opendaylight.org/browse/TSC-78
* http://docs.opendaylight.org/projects/genius/en/latest/specs/of-tunnels.html
* http://docs.opendaylight.org/projects/genius/en/latest/specs/itm-tunnel-auto-config.html
