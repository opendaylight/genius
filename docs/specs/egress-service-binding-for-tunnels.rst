
.. contents:: Table of Contents
:depth: 3

==================================
Egress Service Binding On Tunnels
==================================

https://git.opendaylight.org/gerrit/#/q/topic:egress-tunnel-service-binding

Egress Service Binding On Tunnels Feature enables applications to bind multiple services on
an egress tunnel.


Problem description
===================

Currently GENIUS does not provide a mechanism to support binding egress services on tunnels.
Egress Service Binding is only supported for l2vlan interfaces.



Use Cases
---------

This feature will support following use cases:

Use case 1: interface-manager should support binding multiple egress services on tunnels
Use case 2: interface-manager should support priority based egress service handling for tunnels
Use case 3: all application traffic egressing on a tunnel should go through the egress 
dispatcher table
Use case 4: datapath should be intact even if there is no egress service bound on the tunnel
Use case 5: interface-manager should support unbinding egress services on tunnels
Use case 5: interface-manager should support handling of lower layer interface deletions gracefully

Following use cases will not be supported:

Use case 1 : Update of egress service binding on tunnels

Proposed change
===============

The proposed change extends the current egress service binding functionality to tunnel
interfaces. With this feature, multiple applications can bind their services on the same
tunnel interface, and traffic will be processed on an application priority basis.
Applications are given the flexibility to provide service specific actions while they
bind their services. Normally service binding actions include
<go-to-service-pipeline-entry-table>. Packets will enter a particular egress service based
on the service priority, and if the packet is not consumed by the service,
it is the application's responsibility to resubmit the packet back to the egress
dispatcher table for further processing by next priority service. Egress Dispatcher
Table will have a default service priority entry per tunnel interface to egress the
packet on the tunnel port.So, if there are no egress services bound on a tunnel interface,
this default entry will take care of taking the packet out of the switch.


Pipeline changes
----------------
There is a pipeline change introduced as part of this feature for tunnel egress,
and is captured in genius pipeline document already:

http://docs.opendaylight.org/en/latest/submodules/genius/docs/pipeline.html

After the ingress service processing, packets which are identified to be egressed on
tunnel interfaces, currently directly go to the tunnel port. With this feature,
these packets will goto Egress Dispatcher Table first, where the packet will be processed
by Egress Services on the tunnel interface one by one, and finally will egress the switch.

Register6 will be used to indicate service priority as well as interface tag for the tunnel
interface, in Egress Dispatcher Table, and when there are N services bound on a tunnel
interface, there will be N+1 entries in Egress Dispatcher Table,
the additional one for the default tunnel entry. The first 4 bits of Register6 will be
used to indicate the service priority and the next 20 bits for interface Tag, and this will
be the match criteria for packet redirection to service pipeline in Egress Dispatcher Table.
Before sending the packet to the service, Egress Dispatcher Table will set the service index
to the next service' priority.

RPC Changes
-----------

GetEgressACtionsForInterface RPC in interface-manager currently returns the output:port action
for tunnel interfaces. This will be changed to return
set_field_reg6(default-service-index + interface-tag) and
resubmit(egress_dispatcher_table).

Yang changes
------------

N/A (Already covered as part of the basic egress service binding support for l2vlan interfaces)

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
The feature adds one extra transaction during tunnel port creation, since the default Egress Dispatcher Table entry has to be programmed for each tunnel.

Targeted Release
-----------------
Carbon.

Alternatives
------------
N/A

Usage
=====
How will end user use this feature? Primary focus here is how this feature
will be used in an actual deployment.

For most Genius features users will be other projects but this
should still capture any user visible CLI/API etc. e.g. ITM configuration.

This section will be primary input for Test and Documentation teams.
Along with above this should also capture REST API and CLI.

Features to Install
-------------------
This feature doesn't add any new karaf feature.Installing any of the below features can enable the service:

odl-genius-ui
odl-genius-rest
odl-genius

REST API
--------
Sample JSONS/URIs.

**URL:** http://localhost:8181/restconf/config/interface-service-bindings:service-bindings

**Sample JSON data**

{
  "services-info": [
    {
      "interface-name": "<tunnel-interface-name>",
      “service-mode” : “service-mode-egress”,
      "bound-services": [
        {
          "service-name": "RT5",
          "flow-priority": "5",
          "service-type": "service-type-flow-based",
          "instruction": [
           {
            "order": 1,
            "go-to-table": {
               "table_id": <table_id>
             }
           }],
          "service-priority": "2",
          "flow-cookie": "1"
        }
      ]
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
Who is implementing this feature? In case of multiple authors, designate a
primary assignee and other contributors.

Primary assignee:
  Faseela K


Work Items
----------
Break up work into individual items. This should be a checklist on
Trello card for this feature. Give link to trello card or duplicate it.


Dependencies
============
Genius, Netvirt


Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
New junits will be added to InterfaceManagerConfigurationTest to cover the following :

* bind single egress service on tunnel
* unbind single egress service on tunnel
* bind multiple egress services on tunnel in priority order
* unbind multiple egress services on tunnel in priority order
* bind multiple egress services out of priority order
* unbind multiple egress services out of priority order
* delete tunnel port to check if egress dispatcher flows for bound services get deleted
* add tunnel port back to check if egress dispatcher flows for bound services get added back

Integration Tests
-----------------

CSIT
----
The following TCs should be added to CSIT to cover this feature:

* bind single egress service on tunnel to see the corresponding table entries
  are created in switch
* unbind single egress service on tunnel to see the corresponding table entries
  are deleted in switch
* bind multiple egress services on tunnel in priority order to see if metadata
  changes are proper on the flow table
* unbind multiple egress services on tunnel in priority order to see if metadata
  changes are proper on the flow table on each unbind
* bind multiple egress services out of priority order to see if metadata
  changes are proper on the flow table
* unbind multiple egress services out of priority order
* delete tunnel port to check if egress dispatcher flows for bound services get deleted
* add tunnel port back to check if egress dispatcher flows for bound services get added back


Documentation Impact
====================
There is a pipeline change for tunnel datapath intriduced due to this change.
The proposed pipeline change is available in the below patch:


References
==========
Add any useful references. Some examples:

* Links to Summit presentation, discussion etc.
* Links to mail list discussions
* Links to patches in other projects
* Links to external documentation

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__

[2] https://specs.openstack.org/openstack/nova-specs/specs/kilo/template.html

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
