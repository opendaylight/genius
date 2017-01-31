
.. contents:: Table of Contents
      :depth: 3

==========================
Service Binding On Tunnels
==========================

https://git.opendaylight.org/gerrit/#/q/topic:service-binding-on-tunnels

Service Binding On Tunnels Feature enables applications to bind multiple services on
an ingress/egress tunnel.


Problem description
===================

Currently GENIUS does not provide a generic mechanism to support binding services on all
interfaces.Ingress service binding pipeline is different for l2vlan interfaces and tunnel
interfaces.Similarly, egress Service Binding is only supported for l2vlan interfaces.

Today when ingress services are bound on a tunnel, the highest priority service gets
bound in ``INTERFACE INGRESS TABLE(0)`` itself, and remaining service entries get
populated in ``LPORT DISPATCHER TABLE(17)``, which is not in alignment with the service
binding logic for VM ports. As part of this feature, we enable ingress/egress service
binding support for tunnels in the same way as for VM interfaces. This feature also enables
service-binding based on a tunnel-type which is basically meant for optimizing the number
of flow entries in dispatcher tables.

Use Cases
---------

This feature will support following use cases:

* Use case 1: IFM should support binding services based on tunnel type.
* Use case 2: All application traffic ingressing on a tunnel should go through the ``LPORT
  DISPATCHER TABLE(17)``.
* Use case 3: IFM should support binding multiple ingress services on tunnels.
* Use case 4: IFM should support priority based ingress service handling for tunnels.
* Use case 5: IFM should support unbinding ingress services on tunnels.
* Use case 6: IFM should support binding multiple egress services on tunnels.
* Use case 7: IFM should support priority based egress service handling for tunnels.
* Use case 8: All application traffic egressing on a tunnel should go through the egress
  dispatcher table(220).
* Use case 9: Datapath should be intact even if there is no egress service bound on the tunnel.
* Use case 10: IFM should support unbinding egress services on tunnels.
* Use case 11: IFM should support handling of lower layer interface deletions gracefully.
* Use case 12: IFM should support binding services based on tunnel type and
  lport-tag on the same tunnel interface on a priority basis.
* Use case 13: Applications should bind on specific tunnel types on module startup
* Use case 13: IFM should take care of programming the tunnel type based binding flows
  on each DPN.

Following use cases will not be supported:

* Use case 1 : Update of service binding on tunnels. Any update should be done as
  delete and re-create

Proposed change
===============

The proposed change extends the current l2vlan service binding functionality to tunnel
interfaces. With this feature, multiple applications can bind their services on the same
tunnel interface, and traffic will be processed on an application priority basis.
Applications are given the flexibility to provide service specific actions while they
bind their services. Normally service binding actions include
*go-to-service-pipeline-entry-table*. Packets will enter a particular service based
on the service priority, and if the packet is not consumed by the service,
it is the application's responsibility to resubmit the packet back to the ``egress/ingress
dispatcher table`` for further processing by next priority service. Egress Dispatcher
Table will have a default service priority entry per tunnel interface to egress the
packet on the tunnel port.So, if there are no egress services bound on a tunnel interface,
this default entry will take care of taking the packet out of the switch.

The feature also enables service binding based on tunnel type. This way number of entries in
Dispatcher Tables can be optimized if all the packets entering on tunnel of a particular type
needs to be handled in the same way.


Pipeline changes
----------------
There is a pipeline change introduced as part of this feature for tunnel egress as well
as ingress, and is captured in genius pipeline document patch [2]_.

With this feature, all traffic from INTERFACE_INGRESS_TABLE(0) will be dispatched to
LPORT_DISPATCHER_TABLE(17), from where the packets will be dispatched to the respective
applications on a priority basis.

Register6 will be used to set the ingress tunnel-type in Table0, and this can be used to
match in Table17 to identify the respective applications bound on the tunnel-type.
Remaining logic of ingress service binding will remain as is, and service-priority and
interface-tag will be set in metadata as usual. The bits from 25-28 of Register6 will be
used to indicate tunnel-type.

After the ingress service processing, packets which are identified to be egressed on
tunnel interfaces, currently directly go to the tunnel port. With this feature,
these packets will goto Egress Dispatcher Table[Table 220] first, where the packet will be
processed by Egress Services on the tunnel interface one by one, and finally will egress the switch.

Register6 will be used to indicate service priority as well as interface tag for the egress tunnel
interface, in Egress Dispatcher Table, and when there are N services bound on a tunnel
interface, there will be N+1 entries in Egress Dispatcher Table,
the additional one for the default tunnel entry. The first 4 bits of Register6 will be
used to indicate the service priority and the next 20 bits for interface Tag, and this will
be the match criteria for packet redirection to service pipeline in Egress Dispatcher Table.
Before sending the packet to the service, Egress Dispatcher Table will set the service index
to the next service' priority. Same as ingress, Register6 will be used for egress tunnel-type
matching, if there are services bound on tunnel-type.

+-------------------------+---------------------------+----------------------------------+
| TABLE                   | MATCH                     |            ACTION                |
+=========================+===========================+==================================+
|                         |  in_port                  |  SI=0,reg6=interface_type,       |
| INTERFACE_INGRESS_TABLE |                           |  metadata=lport tag,             |
|                         |                           |  goto table 17                   |
+-------------------------+---------------------------+----------------------------------+
| LPORT_DISPATCHER_TABLE  | metadata=service priority |  increment SI,                   |
|                         | && lport-tag(priority=10) |  apply service specific actions, |
|                         |                           |  goto ingress service            |
|                         +---------------------------+----------------------------------+
|                         | reg6=tunnel-type          |  increment SI,                   |
|                         | priority=5                |  apply service specific actions, |
|                         |                           |  goto ingress service            |
+-------------------------+---------------------------+----------------------------------+
| EGRESS_DISPATCHER_TABLE | Reg6==service Priority    |  increment SI,                   |
|                         | && lport-tag(priority=10) |  apply service specific actions, |
|                         |                           |  goto egress service             |
|                         +---------------------------+----------------------------------+
|                         | reg6=tunnel-type          |  increment SI,                   |
|                         | priority=5                |  apply service specific actions, |
|                         |                           |  goto egress service             |
+-------------------------+---------------------------+----------------------------------+

RPC Changes
-----------

``GetEgressActionsForInterface`` RPC in interface-manager currently returns the output:port
action for tunnel interfaces. This will be changed to return
set_field_reg6(default-service-index + interface-tag) and resubmit(egress_dispatcher_table).

Yang changes
------------

No yang changes are needed, as binding on tunnel-type is enabled by having reserved keywords for
interface-names

Workflow
--------

Create Tunnel
^^^^^^^^^^^^^
#. User: User created a tunnel end point
#. IFM:  When tunnel port is created on OVS, and the respective OpenFlow port Notification
   comes, IFM binds a default service in Egress Dispatcher Table for the tunnel interface,
   which will be the least priority service, and the action will be to take
   the packet out on the tunnel port.

Bind Service on Tunnel Interface
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. User: While binding service on tunnels user gives ``service-priority``, ``service-mode``
   and ``instructions`` for service being bound on the tunnel interface.
#. IFM: When binding the service for the tunnel, if this is the first service
   being bound, program flow rules in Dispatcher Table(ingress/egress based on service mode)
   to match on ``service-priority`` and ``interface-tag`` value with actions
   pointing to the service specific actions supplied by the application.
#. IFM: When binding a second service, based on the service priority one more flow will
   be created in Dispatcher Table with matches specific to the new service
   priority.

Unbind Service on Tunnel Interface
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. User: While unbinding service on tunnels user gives ``service-priority`` and
   ``service-mode`` for service being unbound on the tunnel interface.
#. IFM: When unbinding the service for the tunnel, IFM removes the entry in
   Dispatcher Tables for the service. IFM also rearranges the remaining flows for the
   same tunnel interface to adjust the missing service priority

Bind Service on Tunnel Type
^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Application: While binding service on tunnel type user gives a reserved keyword
   indicating the tunnel-type apart from``service-priority``, ``service-mode``
   and ``instructions`` for service being bound. The reserved keywords will be
   ``ALL_VXLAN_INTERNAL``, ``ALL_VXLAN_EXTERNAL``, and ``ALL_MPLS_OVER_GRE``.
#. IFM: When binding the service for the tunnel-type,program flow rules in Dispatcher
   Table(ingress/egress based on service mode) to match on ``service-priority`` and
   ``tunnel-type`` value with actions pointing to the service specific actions
   supplied by the application will be created on each DPN.
#. IFM: When binding a second service, based on the service priority one more flow will
   be created in Dispatcher Table with matches specific to the new service
   priority will be created on each DPN..

Unbind Service on Tunnel Type
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. User: While unbinding service on tunnels user gives a reserved keyword
   indicating the tunnel-type ,``service-priority`` and ``service-mode`` for service being
   unbound on all connected DPNs.
#. IFM: When unbinding the service for the tunnel-type, IFM removes the entry in
   Dispatcher Tables for the service. IFM also rearranges the remaining flows for the
   same tunnel type to adjust the missing service priority

Delete Tunnel
^^^^^^^^^^^^^
#. User: User deleted a tunnel end point
#. IFM:  When tunnel port is deleted on OVS, and the respective OpenFlow Port Notification
   comes, IFM unbinds the default service in Egress Dispatcher Table for the tunnel interface.
#. IFM:  If there are any outstanding services bound on the tunnel interface, all the Dispatcher
   Table Entries for this Tunnel will be deleted by IFM.

Application Module Startup
^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Applications:  When Application bundle comes up, they can bind respective applications
   on the tunnel types they are interested in, with their respective service priorities.

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
The solution is supported on a 3-node cluster.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
* The feature adds one extra transaction during tunnel port creation, since the default
  Egress Dispatcher Table entry has to be programmed for each tunnel.
* The feature provides support for service-binding on tunnel type with the primary purpose
  of minimizing the number of flow entries in ingress/egress dispatcher tables.

Targeted Release
-----------------
Carbon.

Alternatives
------------
N/A

Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature.Installing any of the below features
can enable the service:

odl-genius-ui
odl-genius-rest
odl-genius

REST API
--------

Creating tunnel-interface directly in IFM
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This use case is mainly for those who want to write applications using Genius and/or
want to create individual tunnel interfaces. Note that this is a simpler easy way to
create tunnels without needing to delve into how OVSDB Plugin creates tunnels.

Refer `Genius User Guide [4]_`
for more details on this.

**URL:** restconf/config/ietf-interfaces:interfaces

**Sample JSON data**

.. code-block:: json

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
            "odl-interface:monitor-enabled": false,
            "odl-interface:monitor-interval": 10000,
            "enabled": true
        }
     ]
    }
   }

Binding Egress Service On Tunnels
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**URL:** http://localhost:8181/restconf/config/interface-service-bindings:service-bindings/services-info/{tunnel-interface-name}/interface-service-bindings:service-mode-egress

**Sample JSON data**

.. code-block:: json

   {
      "bound-services": [
        {
          "service-name": "service1",
          "flow-priority": "5",
          "service-type": "service-type-flow-based",
          "instruction": [
           {
            "order": 1,
            "go-to-table": {
               "table_id": 88
             }
           }],
          "service-priority": "2",
          "flow-cookie": "1"
        }
      ]
   }


CLI
---
N.A.


Implementation
==============

Assignee(s)
-----------
Primary assignee:
  Faseela K


Work Items
----------

#. Create Table 0 tunnel entries to set tunnel-type and lport_tag and
   point to ``LPORT_DISPATCHER_TABLE``
#. Support of reserved keyword in interface-names for tunnel type based
   service binding.
#. Program tunnel-type based service binding flows on DPN connect events.
#. Program Lport Dispatcher Flows(17) on bind service
#. Remove Lport Dispatcher Flows(17) on unbind service
#. Handle multiple service bind/unbind on tunnel interface
#. Create default Egress Service for Tunnel on Tunnel Creation
#. Add ``set_field_reg_6`` and ``resubmit(220)`` action to actions returned in
   ``getEgressActionsForInterface()`` for Tunnels.
#. Program Egress Dispatcher Table(220) Flows on bind service
#. Remove Egress Dispatcher Table(220) Flows on unbind service
#. Handle multiple egress service bind/unbind on tunnel interface
#. Delete default Egress Service for Tunnel on Tunnel Deletion
#. Add UTs.
#. Add CSIT.
#. Add Documentation

#. Trello Card : https://trello.com/c/S8lNGd9S/6-service-binding-on-tunnel-interfaces

Dependencies
============
Genius, Netvirt

There will be several impacts on netvirt pipeline with this change. A brief overview
is given in the table below:

+-------------------------+-------------+-------------+--------------------------+
| RESERVED_INTERFACE_NAME | TUNNEL_TYPE | APPLICATION | DISPATCHER RULES         |
+=========================+=============+=============+==========================+
| ALL_VXLAN_INTERNAL      |  OVS - OVS  |  ELAN       |  SI=1, reg6=vxlan_int,   |                     |
|                         |             |             |  goto table 38           |
+-------------------------+-------------+-------------+--------------------------+
| ALL_VXLAN_EXTERNAL      |  OVS - TOR  |  DHCP       |  SI=0, reg6=vxlan_ext,   |
|                         |             |             |  goto table 18           |
|                         +-------------+-------------+--------------------------+
|                         |  OVS - DCGW |  EVPN       |  SI=1, reg6=vxlan_ext,   |
|                         |             |             |  goto table 38           |
|                         +-------------+-------------+--------------------------+
|                         |  OVS - TOR  |   ELAN      |  SI=2,reg6=vxlan_ext,    |
|                         |             |             |  goto table 38           |
+-------------------------+-------------+-------------+--------------------------+
| ALL_MPLS_OVER_GRE       |  OVS - DCGW |  L3VPN      |  SI=0, reg6=mpls_o_gre,  |
|                         |             |             |  goto table 20           |
+-------------------------+-------------+-------------+--------------------------+


Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
New junits will be added to InterfaceManagerConfigurationTest to cover the following :

#. Bind/Unbind single ingress service on tunnel-type
#. Bind/Unbind single egress service on tunnel-type
#. Bind single ingress service on tunnel-interface
#. Unbind single ingress service on tunnel-interface
#. Bind multiple ingress services on tunnel in priority order
#. Unbind multiple ingress services on tunnel in priority order
#. Bind multiple ingress services out of priority order
#. Unbind multiple ingress services out of priority order
#. Delete tunnel port to check if ingress dispatcher flows for bound services get deleted
#. Add tunnel port back to check if ingress dispatcher flows for bound services get added back
#. Bind single egress service on tunnel
#. Unbind single egress service on tunnel
#. Bind multiple egress services on tunnel in priority order
#. Unbind multiple egress services on tunnel in priority order
#. Bind multiple egress services out of priority order
#. Unbind multiple egress services out of priority order
#. Delete tunnel port to check if egress dispatcher flows for bound services get deleted
#. Add tunnel port back to check if egress dispatcher flows for bound services get added back

Integration Tests
-----------------

CSIT
----
The following TCs should be added to CSIT to cover this feature:

#. Bind/Unbind single ingress/egress service on tunnel-type to see the corresponding
   table entries are created in switch.
#. Bind single ingress service on tunnel to see the corresponding table entries
   are created in switch.
#. Unbind single ingress service on tunnel to see the corresponding table entries
   are deleted in switch.
#. Bind multiple ingress services on tunnel in priority order to see if metadata
   changes are proper on the flow table.
#. Unbind multiple ingress services on tunnel in priority order to see if metadata
   changes are proper on the flow table on each unbind.
#. Bind multiple ingress services out of priority order to see if metadata
   changes are proper on the flow table.
#. Unbind multiple ingress services out of priority order.
#. Delete tunnel port to check if ingress dispatcher flows for bound services get deleted.
#. Add tunnel port back to check if ingress dispatcher flows for bound services get added back.
#. Bind single egress service on tunnel to see the corresponding table entries
   are created in switch.
#. Unbind single egress service on tunnel to see the corresponding table entries
   are deleted in switch.
#. Bind multiple egress services on tunnel in priority order to see if metadata
   changes are proper on the flow table.
#. Unbind multiple egress services on tunnel in priority order to see if metadata
   changes are proper on the flow table on each unbind.
#. Bind multiple egress services out of priority order to see if metadata
   changes are proper on the flow table.
#. Unbind multiple egress services out of priority order.
#. Delete tunnel port to check if egress dispatcher flows for bound services get deleted.
#. Add tunnel port back to check if egress dispatcher flows for bound services get added back.


Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

There is a pipeline change for tunnel datapath introduced due to this change.
This should go in User Guide.

Developer Guide should capture how to configure egress service binding on tunnels.


References
==========
.. [#] Genius Carbon Release Plan https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan
.. [#] Netvirt Pipeline Diagram http://docs.opendaylight.org/en/latest/submodules/genius/docs/pipeline.html
.. [#] Genius Trello Card https://trello.com/c/S8lNGd9S/6-service-binding-on-tunnel-interfaces
.. [#] Genius User Guide http://docs.opendaylight.org/en/latest/user-guide/genius-user-guide.html#creating-overlay-tunnel-interfaces

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
