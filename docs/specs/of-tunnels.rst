==========
OF Tunnels
==========

https://git.opendaylight.org/gerrit/#/q/topic:of-tunnels

OF Tunnels feature adds support for flow based tunnels to allow
scalable overlay tunnels.

Problem description
===================

Today when tunnel interfaces are created, InterFaceManager [IFM] creates one
OVS port for each tunnel interface i.e. source-destination pair. For N devices
in a TransportZone this translates to N*(N-1) tunnel ports created across all
devices and N-1 ports in each device. This has obvious scale limitations.

Use Cases
---------
This feature will support following use cases:

* Use case 1: Allow user to specify if they want to use flow based tunnels at
  the time of configuration.
* Use case 2: Create single OVS Tunnel Interface if flow based tunnels are
  configured and this is the first tunnel on this device/tep.
* Use case 3: Flow based and non flow based tunnels should be able to exist
  in a given transport zone.
* Use case 4: On tep delete, if this is the last tunnel interface on this
  tep/device and it is flow based tunnel, delete the OVS Tunnel Interface.

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
* Configuration of Source IP for tunnel through flow. It will still be fixed. Though we're
  adding option in IFM YANG for this, implementation for it won't be done till we get
  use case(s) for it.

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
Following new actions will be added to ``mdsalutil/ActionType.java``

* set_tunnel_src_ip
* set_tunnel_dest_ip

Following new matches will be added to ``mdsalutil/NxMatchFieldType.java``

* tun_src_ip
* tun_dest_ip

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
Changes will be needed in ``itm.yang`` and ``odl-interface.yang`` to allow
configuring a tunnel as flow based or not.

ITM YANG changes
^^^^^^^^^^^^^^^^
A new parameter ``option-of-tunnel`` will be added to ``list-vteps``

.. code-block:: none
   :caption: itm.yang
   :emphasize-lines: 12-15

    list vteps {
        key "dpn-id portname";
        leaf dpn-id {
            type uint64;
        }
        leaf portname {
            type string;
        }
        leaf ip-address {
            type inet:ip-address;
        }
        leaf option-of-tunnel {
            type boolean;
            default false;
        }
    }

Same parameter will also be added to ``tunnel-end-points`` in ``itm-state.yang``.
This will help eliminate need to retrieve information from TransportZones when configuring
tunnel interfaces.

.. code-block:: none
   :caption: itm-state.yang
   :emphasize-lines: 11-14

    list tunnel-end-points {
        ordered-by user;
        key "portname VLAN-ID ip-address tunnel-type";
        /* Multiple tunnels on the same physical port but on different VLAN can be supported */

        leaf portname {
            type string;
        }
        ...
        ...
        leaf option-of-tunnel {
            type boolean;
            default false;
        }
    }


This will allow to set OF Tunnels on per VTEP basis. So in a transport-zone
we can have some VTEPs (devices) that use OF Tunnels and others that don't.
Default of false means it will not impact existing behavior and will need to
be explicitly configured. Going forward we can choose to set default true.

IFM YANG changes
^^^^^^^^^^^^^^^^
We'll add a new ``tunnel-optional-params`` and add them to ``iftunnel``

.. code-block:: none
   :caption: odl-interface.yang
   :emphasize-lines: 1-23

    grouping tunnel-optional-params {
        leaf tunnel-source-ip-flow {
            type boolean;
            default false;
        }

        leaf tunnel-remote-ip-flow {
            type boolean;
            default false;
        }

        list tunnel-options {
            key "tunnel-option";
            leaf tunnel-option {
                description "Tunnel Option name";
                type string;
            }
            leaf value {
                description "Option value";
                type string;
            }
        }
    }

The ``list tunnel-options`` is a list of key-value pairs of strings, similar to
options in OVSDB Plugin. These are not needed for OF Tunnels but is being added
to allow user to configure any other Interface options that OVS supports. Aim is to
enable developers and users try out newer options supported by OVS without needing to
add explicit support for it. Note that there is no counterpart for this option in
``itm.yang``. Any options that we want to explicitly support will be added as a separate
option. This will allow us to do better validations for options that are needed for
our specific use cases.


.. code-block:: none
   :emphasize-lines: 6

    augment "/if:interfaces/if:interface" {
        ext:augment-identifier "if-tunnel";
        when "if:type = 'ianaift:tunnel'";
        ...
        ...
        uses tunnel-optional-params;
        uses monitor-params;
    }

Workflow
--------

Adding tep
^^^^^^^^^^

#. User: While adding tep user gives ``option-of-tunnel:true`` for tep being
   added.
#. ITM: When creating tunnel interfaces for this tep, if
   ``option-of-tunnel:true``, set ``tunnel-remote-ip:true`` for the tunnel
   interface.
#. IFM: If ``option-of-tunnel:true`` and this is first tunne on this device,
   set ``option:remote_ip=flow`` when creating tunnel interface in OVSDB. Else,
   set ``option:remote_ip=<destination-ip>``.

Deleting tep
^^^^^^^^^^^^

#. If ``tunnel-remote-ip:true`` and this is *last* tunnel on this device,
   delete tunnel port in OVSDB. Else, do nothing.
#. If ``tunnel-remote-ip:false``, follow existing logic.

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
Carbon.
Boron-SR3.

Known Limitations
-----------------
BFD monitoring will not work when OF Tunnels are used. Today BFD monitoring in
OVS relies on destination_ip configured in remote_ip when creating tunnel port
to determine target IP for BFD packets. If we use ``flow`` it won't know where
to send BFD packets. Unless OVS allows adding destination IP for BFD monitoring
on such tunnels, monitoring cannot be enabled.

Alternatives
------------
LLDP/ARP based monitoring was considered for OF tunnels to overcome lack of BFD
monitoring but was rejected because LLDP/ARP based monitoring doesn't scale
well. Since driving requirement for this feature is scale setups, it didn't
make sense to use an unscalable solution for monitoring.

XML/CFG file based global knob to enable OF tunnels for all tunnel interfaces
was rejected due to inflexible nature of such a solution. Current solution
allows a more fine grained and device based configuration at runtime. Also,
wanted to avoid adding yet another global configuration knob.

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


Creating tunnel-interface directly in IFM
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This use case is mainly for those who want to write applications using Genius and/or
want to create individual tunnel interfaces. Note that this is a simpler easy way to
create tunnels without needing to delve into how OVSDB Plugin creates tunnels.

Refer `Genius User Guide <http://docs.opendaylight.org/en/latest/user-guide/genius-user-guide.html#creating-overlay-tunnel-interfaces>`__
for more details on this.

**URL:** restconf/config/ietf-interfaces:interfaces

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 10

   {
    "interfaces": {
    "interface": [
        {
            "name": "vxlan_tunnel",
            "type": "iana-if-type:tunnel",
            "odl-interface:tunnel-interface-type": "odl-interface:tunnel-type-vxlan",
            "odl-interface:datapath-node-identifier": "1",
            "odl-interface:tunnel-source": "192.168.56.101",
            "odl-interface:tunnel-destination": "192.168.56.102",
            "odl-interface:tunnel-remote-ip-flow": "true",
            "odl-interface:monitor-enabled": false,
            "odl-interface:monitor-interval": 10000,
            "enabled": true
        }
     ]
    }
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
  <Vishal Thapar>

Other contributors:
  <Vacancies available>


Work Items
----------
#. YANG changes
#. Add relevant match and actions to MDSALUtil
#. Add ``set_tunnel_dest_ip`` action to actions returned in
   ``getEgressActions()`` for OF Tunnels.
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
Integration tests will be added once IT framework for ITM and IFM is ready.

CSIT
----
CSIT already has test cases for tunnels which test with non OF Tunnels. Similar test
cases will be added for OF Tunnels. Alternatively, some of the existing test cases
that use multiple teps can be tweaked to use OF Tunnels for one of them.

Following test cases will need to be added/expanded in Genius CSIT:

#. Create a TZ with more than one TEPs set to use OF Tunnels and test datapath.
#. Create a TZ with mix of OF and non OF Tunnels and test datapath.
#. Delete a TEP using OF Tunnels and add it again with non OF tunnels and test
   the datapath.
#. Delete a TEP using non OF Tunnels and add it again with OF Tunnels and test
   datapath.

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information on how to add TEPs with flow based
tunnels.

Developer Guide will need to capture how to use changes in IFM to create
individual tunnel interfaces.

References
==========

* https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan
