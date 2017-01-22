.. contents:: Table of Contents
      :depth: 3

================================================================
Load balancing and high availability of multiple VxLAN tunnels
================================================================

https://git.opendaylight.org/gerrit/#/q/topic:multiple-vxlan-tunnels

The purpose of this feature is to enable resiliency and load balancing of VxLAN encapsulated traffic
between pair of OVS nodes.

Additionally, the feature will provide infrastructure to support more complex use cases such as policy-based
path selection. The exact implementation of policy-based path selection is out of the scope of this document
and will be described in a different spec [2].


Problem description
===================

The current ITM implementation enables creation of a single VxLAN tunnel between each pair of hypervisors.

If the hypervisor is connected to the network using multiple links with different capacity or connected to different
L2 networks in different subnets, it is not possible to utilize all the available network resources to increase the
throughput of traffic to remote hypervisors.

In addition, link failure of the network card forwarding the VxLAN traffic will result in complete traffic loss
to/from the remote hypervisor if the network card is not part of a bonded interface.

Use Cases
---------

* Forwarding of VxLAN traffic between hypervisors with multiple network cards connected to L2 switches in
  different networks.
* Forwarding of VxLAN traffic between hypervisors with multiple network cards connected to the same L2 switch.

Proposed change
===============

* The ITM will continue to create tunnels based on transport-zone configuration similarly to the current implementation.
  When ITM detects that the same DPN is configured with different IPs in different zones, in addition to creating the
  actual tunnels, it will create logical tunnel interface for each pair of DPNs in the ``ietf-interface``
  config data-store representing the tunnel aggregation group between the DPNs.

  Creation of transport-zone with multiple IPs per DPN is out of the scope of this document and will be described in [2].

  The logical tunnel will reference all member tunnel interfaces in the group using the ``parent-refs`` augmentation.
  In addition, it would be possible to add weight to each member of the group to support unequal load-sharing of traffic.

* The proposed feature depends on egress tunnel service binding functionality detailed in [3].

  When the logical tunnel interface is created, a default egress service would be bound to it. The egress service will
  create an OF select group based on the actual list of tunnel members in the logical group.
  Each tunnel member can be assigned a weight field that will be applied on it's corresponding bucket in the OF select
  group. If weight was not defined, the bucket weight will be configured with a default value of 1 resulting
  in uniform distribution if weight was not configured for any of the buckets.
  Each bucket in the select group will route the egress traffic to one of the tunnel members in the group by
  loading the lport-tag of the tunnel member interface to NXM ``register6``.

  Logical tunnel egress service pipeline example:

::

 cookie=0x6900000, duration=0.802s, table=220, n_packets=0, n_bytes=0, priority=6,reg6=0x500
 actions=load:0xe000500->NXM_NX_REG6[],write_metadata:0xe000500000000000/0xfffffffffffffffe,group:80000
 cookie=0x8000007, duration=0.546s, table=220, n_packets=0, n_bytes=0, priority=7,reg6=0x600 actions=output:3
 cookie=0x8000007, duration=0.546s, table=220, n_packets=0, n_bytes=0, priority=7,reg6=0x700 actions=output:4
 cookie=0x8000007, duration=0.546s, table=220, n_packets=0, n_bytes=0, priority=7,reg6=0x800 actions=output:5
 group_id=800000,type=select,
 bucket=weight:50,watch_port=3,actions=load:0x600->NXM_NX_REG6[],resubmit(,220),
 bucket=weight:25,watch_port=4,actions=load:0x700->NXM_NX_REG6[],resubmit(,220),
 bucket=weight:25,watch_port=5,actions=load:0x800->NXM_NX_REG6[],resubmit(,220)

* BFD monitoring is required to probe the tunnel state and update the OF select group accordingly. Using OF tunnels [4]
  or turning off BFD monitoring will not allow the logical group service to respond to tunnel state changes.

* OF select group for logical tunnel can contain a mix of IPv4 and IPv6 tunnels, depending on the transport-zone
  configuration.

* ITM RPC calls to get internal tunnel interface between source and destination DPNs will return the logical tunnel
  interface group name if such exits, otherwise the lower layer tunnel will be returned.

* Since the logical tunnel group is an ``ietf-interface`` it has an allocated lport-tag.
  RPC call to ``getEgressActionsForInterface`` for the logical tunnel will load ``register6`` with its corresponding
  lport-tag and resubmit the traffic to the egress dispatcher table.

* The state of the logical tunnel group is affected by the states of the group members. If at least one of the
  tunnels is in ``oper-status`` UP, the logical group is considered UP.

  If the logical tunnel was set as ``admin-status`` DOWN, all the tunnel members will be set accordingly.

  Each bucket of the LB group will set the ``watch_port`` property to be the tunnel member OF port number.
  This will allow the OVS to monitor the bucket liveness and route egress traffic only to live buckets.

* Other applications would be able to utilize this infrastructure to introduce new services over logical tunnel group
  interface e.g. policy-based path selection. These services will take precedence over the default egress service for
  logical tunnel.


Pipeline changes
----------------

For the flows below it is assumed that a logical tunnel group was configured for both ingress and egress DPNs.
The logical tunnel group is composed of { ``tunnnel1``, ``tunnel2`` } and bound to the default logical tunnel
egress service.

Traffic between VMs on the same DPN
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
No pipeline changes required

L3 traffic between VMs on different DPNs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

VM originating the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""""
- Remote next hop group in the FIB table references the logical tunnel group.
- The default logical group service uses OF select group to load balance traffic between the tunnels.

  | Classifier table (0) =>
  | Dispatcher table (17) ``l3vpn service: set vpn-id=router-id`` =>
  | GW Mac table (19) ``match: vpn-id=router-id,dst-mac=router-interface-mac`` =>
  | FIB table (21) ``match: vpn-id=router-id,dst-ip=vm2-ip set dst-mac=vm2-mac tun-id=vm2-label reg6=logical-tun-lport-tag`` =>
  | Egress table (220) ``match: reg6=logical-tun-lport-tag`` =>
  | Logical tunnel LB select group ``set reg6=tun1-lport-tag`` =>
  | Egress table (220) ``match: reg6=tun1-lport-tag`` output to ``tunnel1``


VM receiving the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""
- No pipeline changes required

  | Classifier table (0) =>
  | Internal tunnel Table (36) ``match:tun-id=vm2-label`` =>
  | Local Next-Hop group: ``set dst-mac=vm2-mac,reg6=vm2-lport-tag`` =>
  | Egress table (220) ``match: reg6=vm2-lport-tag`` output to VM 2


SNAT traffic from non-NAPT switch
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

VM originating the traffic is non-NAPT switch:
"""""""""""""""""""""""""""""""""""""""""""""""
- NAPT group references the logical tunnel group.

  | Classifier table (0) =>
  | Dispatcher table (17) ``l3vpn service: set vpn-id=router-id`` =>
  | GW Mac table (19) ``match: vpn-id=router-id,dst-mac=router-interface-mac`` =>
  | FIB table (21) ``match: vpn-id=router-id`` =>
  | Pre SNAT table (26) ``match: vpn-id=router-id`` =>
  | NAPT Group ``set tun-id=router-id reg6=logical-tun-lport-tag`` =>
  | Egress table (220) ``match: reg6=logical-tun-lport-tag`` =>
  | Logical tunnel LB select group ``set reg6=tun1-lport-tag`` =>
  | Egress table (220) ``match: reg6=tun1-lport-tag`` output to ``tunnel1``

Traffic from NAPT switch punted to controller:
"""""""""""""""""""""""""""""""""""""""""""""""
- No explicit pipeline changes required

  | Classifier table (0) =>
  | Internal tunnel Table (36) ``match:tun-id=router-id`` =>
  | Outbound NAPT table (46) ``set vpn-id=router-id, punt-to-controller``

L2 unicast traffic between VMs in different DPNs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

VM originating the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""""
- ELAN DMAC table references the logical tunnel group

  | Classifier table (0) =>
  | Dispatcher table (17) ``l3vpn service: set vpn-id=router-id`` =>
  | GW Mac table (19) =>
  | Dispatcher table (17) ``l2vpn service: set elan-tag=vxlan-net-tag`` =>
  | ELAN base table (48) =>
  | ELAN SMAC table (50) ``match: elan-tag=vxlan-net-tag,src-mac=vm1-mac`` =>
  | ELAN DMAC table (51) ``match: elan-tag=vxlan-net-tag,dst-mac=vm2-mac set tun-id=vm2-lport-tag reg6=logical-tun-lport-tag`` =>
  | Egress table (220) ``match: reg6=logical-tun-lport-tag`` =>
  | Logical tunnel LB select group ``set reg6=tun2-lport-tag`` =>
  | Egress table (220) ``match: reg6=tun2-lport-tag`` output to ``tunnel2``

VM receiving the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""
- No explicit pipeline changes required

  | Classifier table (0) =>
  | Internal tunnel Table (36) ``match:tun-id=vm2-lport-tag set reg6=vm2-lport-tag`` =>
  | Egress table (220) ``match: reg6=vm2-lport-tag`` output to VM 2


L2 multicast traffic between VMs in different DPNs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

VM originating the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""""
- ELAN broadcast group references the logical tunnel group.

  | Classifier table (0) =>
  | Dispatcher table (17) ``l3vpn service: set vpn-id=router-id`` =>
  | GW Mac table (19) =>
  | Dispatcher table (17) ``l2vpn service: set elan-tag=vxlan-net-tag`` =>
  | ELAN base table (48) =>
  | ELAN SMAC table (50) ``match: elan-tag=vxlan-net-tag,src-mac=vm1-mac`` =>
  | ELAN DMAC table (51) =>
  | ELAN DMAC table (52) ``match: elan-tag=vxlan-net-tag`` =>
  | ELAN BC group ``goto_group=elan-local-group, set tun-id=vxlan-net-tag reg6=logical-tun-lport-tag`` =>
  | Egress table (220) ``match: reg6=logical-tun-lport-tag`` =>
  | Logical tunnel LB select group ``set reg6=tun1-lport-tag`` =>
  | Egress table (220) ``match: reg6=tun1-lport-tag`` output to ``tunnel1``

VM receiving the traffic (**Ingress DPN**):
"""""""""""""""""""""""""""""""""""""""""""
- No explicit pipeline changes required

  | Classifier table (0) =>
  | Internal tunnel Table (36) ``match:tun-id=vxlan-net-tag`` =>
  | ELAN local BC group ``set tun-id=vm2-lport-tag`` =>
  | ELAN filter equal table (55) ``match: tun-id=vm2-lport-tag set reg6=vm2-lport-tag`` =>
  | Egress table (220) ``match: reg6=vm2-lport-tag`` output to VM 2


Yang changes
------------
The following changes would be required to support configuration of logical tunnel group:

IFM Yang Changes
^^^^^^^^^^^^^^^^^
Add a new tunnel type to represent the logical group in ``odl-interface.yang``.
::

    identity tunnel-type-logical-group {
        description "Aggregation of multiple tunnel endpoints between two DPNs";
        base tunnel-type-base;
    }

Logical tunnel will reference multiple lower layer interfaces. ``odl-interface:parent-refs`` augment will
be enhanced to reflect this. Each ``parent-interface`` can have an assigned weight to support unequal load sharing.
::

   augment "/if:interfaces/if:interface" {
        ext:augment-identifier "parent-refs";
        leaf datapath-node-identifier {
            type uint64;
        }

        leaf-list parent-interface {
            type string;
        }

        ...
   }

Each tunnel member in the logical group can have an assigned weight as part of ``tunnel-optional-params``
in ``odl-interface:if-tunnel`` augment to support unequal load sharing.
::

    grouping tunnel-optional-params {
        leaf tunnel-source-ip-flow {
            type boolean;
            default false;
        }

        leaf tunnel-remote-ip-flow {
            type boolean;
            default false;
        }

        leaf weight {
           type uint16;
        }

        ...
    }


ITM Yang Changes
^^^^^^^^^^^^^^^^^^
Each tunnel endpoint in ``itm:transport-zones/transport-zone`` can be configured with optional weight parameter.
Weight configuration will be propagated to ``odl-interface:parent-refs``.
::

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

         leaf weight {
              type unit16;
         }

         leaf option-of-tunnel {
              type boolean;
              default false;
         }
    }

The RPC call ``itm-rpc:get-internal-or-external-interface-name`` will be enhanced to contain the destination dp-id
as an optional input parameter
::

    rpc get-internal-or-external-interface-name {
        input {
             leaf source-dpid {
                  type uint64;
             }

             leaf destination-dpid {
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
       }

       output {
            leaf interface-name {
                 type string;
            }
       }
    }


Configuration impact
---------------------
None

Clustering considerations
-------------------------
None

Other Infra considerations
--------------------------
None

Security considerations
-----------------------
None

Scale and Performance Impact
----------------------------
This feature is expected to increase the datapath throughput by utilizing all available network resources.

Targeted Release
-----------------
Carbon

Alternatives
------------
There are certain use cases where it would be possible to add the network cards to a separate bridge with
LACP enabled and patch it to br-int but this alternative was rejected since it imposes limitations on
the type of links and the overall capacity.

Usage
=====

Features to Install
-------------------
This feature doesn’t add any new karaf feature.

REST API
--------
Create multiple uplinks between pair of OVS nodes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
**URL:** restconf/config/itm:transport-zones/

**Sample JSON data**

The following REST will create 3 bi-directional tunnels between two OVS nodes.
::

  {
     "transport-zone": [
      {
          "zone-name": "underlay-net1",
          "subnets": [
          {
            "prefix": "0.0.0.0/0",
            "vteps": [
              {
                "dpn-id": 273348439543366,
                "portname": "tunnel_port",
                "ip-address": "20.2.1.2",
                "option-of-tunnel": false
              },
              {
                "dpn-id": 110400932149974,
                "portname": "tunnel_port",
                "ip-address": "20.2.1.3",
                "option-of-tunnel": false
              }
            ],
            "gateway-ip": "0.0.0.0",
            "vlan-id": 0
          }
         ],
        "tunnel-type": "odl-interface:tunnel-type-vxlan"
      },
      {
          "zone-name": "underlay-net2",
          "subnets": [
          {
            "prefix": "0.0.0.0/0",
            "vteps": [
              {
                "dpn-id": 273348439543366,
                "portname": "tunnel_port",
                "ip-address": "30.3.1.2",
                "option-of-tunnel": false
              },
              {
                "dpn-id": 110400932149974,
                "portname": "tunnel_port",
                "ip-address": "30.3.1.3",
                "option-of-tunnel": false
              }
            ],
            "gateway-ip": "0.0.0.0",
            "vlan-id": 0
          }
         ],
        "tunnel-type": "odl-interface:tunnel-type-vxlan"
      },
     {
          "zone-name": "underlay-net3",
          "subnets": [
          {
            "prefix": "0.0.0.0/0",
            "vteps": [
              {
                "dpn-id": 273348439543366,
                "portname": "tunnel_port",
                "ip-address": "40.4.1.2",
                "option-of-tunnel": false
              },
              {
                "dpn-id": 110400932149974,
                "portname": "tunnel_port",
                "ip-address": "40.4.1.3",
                "option-of-tunnel": false
              }
            ],
            "gateway-ip": "0.0.0.0",
            "vlan-id": 0
          }
         ],
        "tunnel-type": "odl-interface:tunnel-type-vxlan"
      }
    ]
   }


CLI
---
None


Implementation
==============

Assignee(s)
-----------

Primary assignee:
  Tali Ben-Meir <tali@hpe.com>

Other contributors:
  TBD


Work Items
----------
Trello card: https://trello.com/c/Q7LgiHH7/92-multiple-vxlan-endpoints-for-compute

* Add support to ITM for creation of multiple tunnels between pair of DPNs
* Enhance ``odl-interface:parent-refs`` model to support multiple parents
* Create logical tunnel group in ``ietf-interface`` if more than one tunnel exist between two DPNs.
  Update the ``parent-refs`` with the list of individual tunnel members
* Bind a default service for the logical tunnel interface to create OF select group based on the tunnel members
* Change ITM RPC calls to ``getTunnelInterfaceName`` and ``getInternalOrExternalInterfaceName`` to prefer
  the logical tunnel group over the tunnel members
* Support OF weighted select group


Dependencies
============
None

Testing
=======

Unit Tests
----------
* ITM unitests will be enhanced with test cases of multiple tunnels
* IFM unitests will be enhanced to handle CRUD operations on logical tunnel group

Integration Tests
-----------------

CSIT
----
Transport zone creation with multiple tunnels
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
* Verify tunnel endpoint creation
* Verify logical tunnel group creation
* Verify logical tunnel service binding flows/group

Transport zone removal with multiple tunnels
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
* Verify tunnel endpoint removal
* Verify logical tunnel group removal
* Verify logical tunnel service binding flows/group removal

Transport zone updates to single/multiple tunnels
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
* Verify tunnel endpoint creation/removal
* Verify logical tunnel group creation/removal
* Verify logical tunnel service binding flows/group creation/removal


Documentation Impact
====================
None

References
==========

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__

[2] `Policy based path selection <https://git.opendaylight.org/gerrit/#/q/topic:policy-based-path-selection>`__

[3] `Egress tunnel service binding <https://git.opendaylight.org/gerrit/#/q/topic:egress-tunnel-service-binding>`__

[4] `OF tunnels <http://docs.opendaylight.org/en/latest/submodules/genius/docs/specs/of-tunnels.html>`__
