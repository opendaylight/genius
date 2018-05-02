
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
  the time of configuration.
* Use case 3: Create single OVS Tunnel Interface if flow based tunnels are
  configured and this is the first tunnel on this device/tep.
* Use case 4: Flow based and non flow based tunnels should be able to exist
  in a given transport zone.
* Use case 5: If BFD monitoring is required between two end-points, point-to-point
  tunnels should be created between them, overriding the default of-tunnel. This
  point-to-point tunnel will be used only for BFD, and the actual traffic will
  still take of-tunnels.
* Use case 6: On tep delete, if this is the last tunnel interface on this
  tep/device and it is flow based tunnel, delete the OVS Tunnel Interface.

Following use cases will not be supported:

* BFD based tunnel monitoring will not be supported on OF-tunnels. However, if BFD monitoring
  is required between two end-points, point to point tunnels can be still created between them
  for monitoring purpose.
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

Proposed change
===============
``OVS 2.0.0`` onwards allows configuration of flow based tunnels through
interface ``option:remote_ip=flow``. Currently this field is set to
IP address of the destination endpoint.

``remote_ip=flow`` means tunnel destination IP will be set by an OpenFlow
action. This allows us to add different actions for different destinations
using the single OVS/OF port.

This change will add optional parameters to ITM and IFM YANG files to allow
OF Tunnels. Based on this option, ITM will configure IFM which in turn will
create tunnel ports in OVSDB.

Using OVSDB Plugin
------------------
OVSDB Plugin provides following field in Interface to configure options:

.. code-block:: none
   :caption: ovsdb.yang

    list options {
        description "Port/Interface related optional input values";
        key "option";
        leaf option {
            description "Option name";
            type string;
        }
        leaf value {
            description "Option value";
            type string;
        }

For flow based tunnels we will set option name ``remote_ip`` to
value ``flow``.

MDSALUtil changes
-----------------
MDSALUtil changes are already covered by the previous OFTunnels implementation through interface-manager.

Pipeline changes
----------------
This change adds a new match in **Table0**. Today we match in ``in_port``
to determine which tunnel interface this pkt came in on. Since currently
each tunnel maps to a source-destination pair it tells us about source device.
For interfaces configured to use flow based tunnels this will add an
additional match for ``tun_src_ip``. So, ``in_port+tunnel_src_ip`` will
give us which tunnel interface this pkt belongs to.

When services call ``getEgressActions(), they will get one additional action,
``set_tunnel_dest_ip`` before the ``output:ofport`` action.

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

A new container will be added in odl-item-meta.yang to maintain a mapping of parent-child interfaces.


.. code-block:: none
   :caption: odl-item-meta.yang
   :emphasize-lines: 12-15

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

The key for dpn-teps-state yang will have to be made composite, to include ``monitoring-enabled``
flag too, as this will be needed if bfd-monitoring is enabled on an of-tunnel enabled DPN.

.. code-block:: none
   :caption: odl-item-meta.yang
   :emphasize-lines: 12-15

   container dpn-teps-state {
       list dpns-teps {
           key "source-dpn-id";
           leaf source-dpn-id {
               type uint64;
               mandatory true;
           }

           ..........

           /* Remote DPNs to which this DPN-Tep has a tunnel */
           list remote-dpns {
                key "destination-dpn-id monitoring-enabled";
                leaf destination-dpn-id {
                    type uint64;
                    mandatory true;
                }

                leaf monitoring-enabled {
                     type boolean;
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
   :caption: odl-item-meta.yang
   :emphasize-lines: 12-15

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

#. User: Enables itm-scalability by setting itm-direct-tunnels flag to true.
#. User: While adding tep user gives ``option-of-tunnel:true`` for tep being
   added.
#. ITM: When creating tunnel interfaces for this tep, if
   ``option-of-tunnel:true``, set ``tunnel-remote-ip:true`` for the tunnel
   interface.
#. ITM: If ``option-of-tunnel:true`` and this is first tunnle on this device,
   set ``option:remote_ip=flow`` when creating tunnel interface in OVSDB. Else,
   set ``option:remote_ip=<destination-ip>``.
#. ITM: Receives notification when the of-port is added on the switch.
#. ITM: Checks for the northbound configured tunnel interfaces on top of this flow based tunnel,
   and creates tunnels-state for all of them.

Deleting TEP
^^^^^^^^^^^^

#. If ``tunnel-remote-ip:true`` and this is *last* tunnel on this device,
   delete tunnel port in OVSDB. Else, do nothing.
#. If ``tunnel-remote-ip:false``, follow existing logic.

Enable BFD between two TEPs
^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. If BFD monitoring is enabled through the RPC, additional point to point tunnels will be
   created on the specified source, destination DPNs.
#. These tunnel end points won't be added to the usual tunnel-state which applications listen to.
#. The state of the point to point tunnels will be still available via RPC for applications who want to
   use them in their datapath for aliveness.

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
Any clustering requirements are already addressed in ITM and IFM, no new
requirements added as part of this feature.

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

Targeted Release(s)
-------------------
Fluorine.

Known Limitations
-----------------
BFD monitoring will not work when OF Tunnels are used. Today BFD monitoring in
OVS relies on destination_ip configured in remote_ip when creating tunnel port
to determine target IP for BFD packets. If we use ``flow`` it won't know where
to send BFD packets. Unless OVS allows adding destination IP for BFD monitoring
on such tunnels, monitoring cannot be enabled.

However, the solution allows to create additional point to point tunnels between the same end points
which can be solely used for BFD monitoring purpose.

Alternatives
------------
LLDP/ARP based monitoring was considered for OF tunnels to overcome lack of BFD
monitoring but was rejected because LLDP/ARP based monitoring doesn't scale
well. Since driving requirement for this feature is scale setups, it didn't
make sense to use an unscalable solution for monitoring.

Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature.

REST API
--------

Adding TEPs to transport zone
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

For most users TEP Addition is the only configuration they need to do to create
tunnels using genius. The REST API to add TEPs with OF Tunnels is same as earlier
with one small addition.

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
#. Add ``set_tunnel_dest_ip`` action to actions returned in
   ``getEgressActionsForTunnel()`` for OF Tunnels.
#. Add match on ``tun_src_ip`` in **Table0** for OF Tunnels.
#. Add CLI.
#. Add UTs.
#. Add ITs.
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
#. Create a TZ with mix of OF and non OF Tunnels and test datapath.
#. Delete a TEP using OF Tunnels and add it again with non OF tunnels.
#. Delete a TEP using non OF Tunnels and add it again with OF Tunnels.
#. Enable BFD monitoring on an OF Tunnel enabled src, dest DPN pair.

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
