
.. contents:: Table of Contents
      :depth: 3

===============
ITM Scalability
===============

Test suit for testing service recovery manager functions.

Test Setup
==========
Test setup consists of ODL with `odl-genius-rest` installed and two switches (DPNs) connected
to ODL over OVSDB and OpenflowPlugin.

Testbed Topologies
------------------
This suit uses the default Genius topology.

Default Topology
^^^^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------
N.A

Software Requirements
---------------------
OVS 2.6+

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

Capture any debugging information that is captured at start of suite and end of suite.


Test Cases
==========

ITM TEP Recovery
----------------
Verify SRM by recovering TEP instance by using transportzone name and TEP's ip address.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Create the setup as per the default-topology.
   #. Create ITM tunnel using REST API.
   #. Verify  the ITM is created on both controller and ovs.
   #. Delete the ITM tunnel via REST.
   #. Check ITM is deleted on both controller and ovs
   #. Login to karaf and issue instance recovery command using transportzone name and TEP's ip address.
   #. Aboved deleted ITM is recovered
   #. Verify in controller and ovs.


ITM Transportzone Recovery
--------------------------
Verify SRM by recovering TZ instance using transportzone name.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Create the setup as per the default-topology.
   #. Create ITM tunnel using REST API.
   #. Verify  the ITM is created on both controller and ovs.
   #. Delete the ITM tunnel via REST.
   #. Check ITM is deleted on both controller and ovs
   #. Login to karaf and issue instance recovery command using transportzone name.
   #. Aboved deleted ITM is recovered
   #. Verify in controller and ovs.


ITM Service Recovery
--------------------
Verify SRM by recovering service ITM.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Create the setup as per the default-topology.
   #. Create ITM tunnel using REST API.
   #. Verify  the ITM is created on both controller and ovs.
   #. Delete the ITM tunnel via REST.
   #. Check ITM is deleted on both controller and ovs
   #. Login to karaf and issue service recovery command using service name.
   #. Aboved deleted ITM is recovered
   #. Verify in controller and ovs.

IFM Instance Recovery
---------------------
Verify SRM instance recovery using interface port name.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Create the setup as per the default-topology.
   #. Create ITM tunnel using REST API.
   #. Verify  the ITM is created on both controller and ovs.
   #. Delete the ITM tunnel via REST.
   #. Check ITM is deleted on both controller and ovs
   #. Login to karaf and issue instance recovery command using interface port name.
   #. Aboved deleted ITM is recovered
   #. Verify in controller and ovs.


IFM Service Recovery
--------------------
Verify SRM by recovering service IFM.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Create the setup as per the default-topology.
   #. Create ITM tunnel using REST API.
   #. Verify  the ITM is created on both controller and ovs.
   #. Delete the ITM tunnel via REST.
   #. Check ITM is deleted on both controller and ovs
   #. Login to karaf and issue service recovery command using service name.
   #. Aboved deleted ITM is recovered
   #. Verify in controller and ovs.


Implementation
==============
N.A.

Assignee(s)
-----------
Who is contributing test cases? In case of multiple authors, designate a
primary assignee and other contributors. Primary assignee is also expected to
be maintainer once test code is in.

Primary assignee:
  Nidhi Adhvaryu

Other contributors:
  N.A

Work Items
----------
N.A.

Links
-----

* Link to implementation patche(s) in CSIT

References
==========

[1] `OpenDaylight Genius SRM user Guide
<http://docs.opendaylight.org/en/latest/submodules/genius/docs/specs/service-recovery.html#srm-operations>`__

