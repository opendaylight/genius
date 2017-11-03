
.. contents:: Table of Contents
      :depth: 3

=================
Interface Manager
=================
Test Suite for testing basic Interface Manager functions.

Test Setup
==========
Test setup consists of ODL with `odl-genius` installed and two switches (DPNs) connected
to ODL over OVSDB and OpenflowPlugin.

Testbed Topologies
------------------
This suit uses the default Genius topology.

Default Topology
^^^^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------
N.A.

Software Requirements
---------------------
OVS 2.6+
Mininet ???

Test Suite Requirements
=======================

Test Suite Bringup
------------------
Following steps are followed at beginning of test suite:

* Bring up ODL with `odl-genius` feature installed
* Add bridge to DPN
* Add `tap` interfaces to bridge created above
* Add OVSDB manager to DPN using `ovs-vsctl set-manager`
* Connect bridge to OpenFlow using `ovs-vsctl set-controller`
* Repeat above steps for other DPNs
* Create REST session to ODL


Test Suite Cleanup
------------------
Following steps are followed at beginning of test suite:

* Delete bridge DPN
* Delete OVSDB manager 'ovs-vsctl del-manager'
* Repeat above steps for other DPNs
* Delete REST session to ODL

Debugging
---------
Following DataStore models are captured at end of each test case:

* config/itm-config:tunnel-monitor-enabled
* config/itm-config:tunnel-monitor-interval
* config/itm-state:dpn-endpoints
* config/itm-state:external-tunnel-list
* config/itm:transport-zones
* config/network-topology:network-topology
* config/opendaylight-inventory:nodes
* operational/ietf-interfaces:interfaces
* operational/ietf-interfaces:interfaces-state
* operational/itm-config:tunnel-monitor-enabled
* operational/itm-config:tunnel-monitor-interval
* operational/itm-state:tunnels_state
* operational/network-topology:network-topology
* operational/odl-interface-meta:bridge-ref-info


Test Cases
==========

Create l2vlan Transparent Interface
-----------------------------------
This creates a transparent l2vlan interface between two dpns

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create transparent l2vlan interface through REST

   #. Interface shows up in config
   #. Interface state shows up in operational
   #. Flows are added to `Table0` on the bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Delete l2vlan Transparent Interface
-----------------------------------
This testcase deletes the l2vlan transparent interface created in previous test case.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Remove all interfaces in config

   #. Interface config is empty
   #. Interface states in operational is empty
   #. Flows are deleted from `Table0` on bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Create l2vlan Trunk Interface
-----------------------------
This testcase creates a l2vlan trunk interface between 2 DPNs.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create l2vlan trunk interface through REST

   #. Interface shows up in config
   #. Interface state shows up in operational
   #. Flows are added to `Table0` on the bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Create l2vlan Trunk Member Interface
------------------------------------
This testcase creates a l2vlan Trunk member interface for the l2vlan trunk interface
created in previous testcase.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create l2vlan trunk member interface through REST

   #. Interface shows up in config
   #. Interface state shows up in operational
   #. Flows are added to `Table0` on the bridge
   #. Flows match on `dl_vlan`
   #. Flows have `action=pop_vlan`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Bind service on Interface
-------------------------
This testcase binds service to the L2vlan Trunk Interface earlier.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Add service bindings for `elan` and `VPN` services on L2Vlan Trunk Interface using REST

   #. Check bindings for `VPN` and `elan` services exist on L2Vlan Trunk interface
   #. Flows are added to `Table17` on the bridge
   #. Flows have action `goto_table:21`
   #. Flows have action `goto_table:50`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Unbind service on Interface
---------------------------
This testcase Unbinds the services which were bound in previous testcase.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Delete service bindings for `elan` and `VPN` services on L2Vlan Trunk Interface using REST

   #. Check bindings for `VPN` and `elan` services on L2Vlan Trunk interface don't exist
   #. No flows on `Table0`
   #. No flows with action `goto_table:21`
   #. No flows with action `goto_table:50`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Delete L2vlan Trunk Interface
-----------------------------
Delete l2vlan trunk interface created and used in earlier test cases

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Remove all interfaces in config

   #. Interface config is empty
   #. Interface states in operational is empty
   #. Flows are deleted from `Table0` on bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.


Implementation
==============

Assignee(s)
-----------

Primary assignee:
  <developer-a>

Other contributors:
  <developer-b>
  <developer-c>


Work Items
----------
N.A.

Links
-----

* Link to implementation patche(s) in CSIT - TBD

References
==========

[1] `OpenDaylight Genius usrr Guide <http://docs.opendaylight.org/en/latest/user-guide/genius-user-guide.html#interface-manager-operations>`__
