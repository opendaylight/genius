
.. contents:: Table of Contents
:depth: 3

==================================
OF-Tunnels with ITM Direct Tunnels
==================================

This document serves as the test plan for the OF Based tunnels.This document comprises of test cases pertaining to all the use case covered by the Functional Spec.

.. note::

  Name of suite and test cases should map exactly to as they appear in Robot reports.

Test Setup
==========

Brief description of test setup.

Testbed Topologies
------------------
Topology device software and inter node communication details -

#. **ODL Node** – 1 or 3 Node ODL Environment should be used
#. **Switch Node** - 3 Nodes with OVS 2.6

Test Topology
^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------

#. 1 controller with 3 OVS for functional testing
#. 3 controller with 3 OVS for functional testing

Software Requirements
---------------------

N.A.

Test Suite Requirements
=======================

Test Suite Bringup
------------------

* itm-direct-tunnel flag should be true.

In test suit bringup build the topology as described in the Test Topology. Bring all the tunnels UP.

Test Suite Cleanup
------------------

Final steps after all tests in suite are done. This should include any cleanup, sanity checks,
configuration etc. that needs to be done once all test cases in suite are done.

Debugging
---------

Capture any debugging information that is captured at start of suite and end of suite.


Test Cases
==========

Create a TZ with TEPs set to use OF Tunnels
--------------------------------------------
Change the config parameter to enable OF tunnel of ITM provisioning and Verify Tunnel Creation is successful.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
   #. Verify the parent-child interface mapping is established.
   #. Verify ingress and egress flows.

Delete the TEPs set to use OF Tunnels
--------------------------------------
Delete the TEPs and verify Deletion is successful and no stale(flows,config) is left.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Delete TEP local ip address from OVS
   #. Verify the deletion is successful and no stale entries left.
   #. Verify the parent-child interface mapping is removed.
   #. Verify ingress and egress flows.


Enable BFD Monitoring for OF Tunnels
-------------------------------------
Verify BFD monitoring can be enabled for OF tunnels.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
   #. Verify whether the BFD monitoring is enabled.
   #. If it is the first entry in the datastore create a P2P tunnel.
   #. Increase the reference count by one.

Disable BFD Monitoring for OF Tunnels
--------------------------------------
Verify BFD monitoring can be disabled for OF tunnels

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
   #. Disable BFD monitoring.
   #. Verify whether the BFD monitoring is disabled
   #. Decrease the reference count by one.
   #. When reference count reaches zero delete the P2P tunnel.


Implementation
==============
N.A.

Assignee(s)
-----------
Who is contributing test cases? In case of multiple authors, designate a
primary assignee and other contributors. Primary assignee is also expected to
be maintainer once test code is in.

Primary assignee:
  Faseela K

Other contributors:
  R P Karthika


Work Items
----------
N.A.

Links
-----

* Link to implementation patche(s) in CSIT

References
==========
https://docs.opendaylight.org/projects/genius/en/latest/specs/of-tunnels.html
