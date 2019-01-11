
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
#. **Switch Node** - 3 Nodes with OVS 2.6 or above

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

**Test Steps and Pass Criteria**

#. Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify the parent-child interface mapping is established.
   #. Verify ingress and egress flows.

Delete the TEPs set to use OF Tunnels
--------------------------------------
Delete the TEPs and verify Deletion is successful and no stale(flows,config) is left.

**Test Steps and Pass Criteria**

#. Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Delete TEP local ip address from OVS
   #. Verify the deletion is successful and no stale entries left.
   #. Verify the parent-child interface mapping is removed.
   #. Verify ingress and egress flows.

Create a single TEP set to use OF Tunnels
--------------------------------------------
Change the config parameter to enable OF tunnel of ITM provisioning and Verify Tunnel Creation is successful.

**Test Steps and Pass Criteria**

#. Create the VxLAN Tunnels between the switch_1 and switch_2.

   #. Verify that the tunnels are built properly between the two switches.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels

#. Create the VxLAN Tunnels by adding in switch_3.

   #. Verify that the tunnels are built properly between the switches.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   *It is important to verify the number of ports per switch still remains the same*
   #. Verify the parent-child interface mapping is established.
   #. Verify ingress and egress flows.

Delete a single TEP set to use OF Tunnels
-----------------------------------------
Delete the TEPs and verify Deletion is successful and no stale(flows,config) is left.

**Test Steps and Pass Criteria**

#. Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Delete TEP local ip address from switch_1.
   #. Verify the parent interface of switch_1 is deleted
   #. Verify the parent interface of switch_2 and switch_3 doesnot contain corresponding child
      interface of switch_1.

Verify tunnel state with BFD Monitoring enabled and interface state down
------------------------------------------------------------------------
Verify BFD monitoring can be enabled for OF tunnels.

**Test Steps and Pass Criteria**

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the BFD monitoring is enabled.
   #. Bring interface state down.
   #. Verify tunnel state is DOWN in tep-show state.

Verify tunnel state with BFD Monitoring enabled and interface state up
----------------------------------------------------------------------
Verify BFD monitoring can be enabled for OF tunnels.

**Test Steps and Pass Criteria**

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the BFD monitoring is enabled.
   #. Bring interface state up.
   #. Verify tunnel state is UP in tep-show state.

Verify reference count with BFD enable RPC
-------------------------------------------
Verify BFD monitoring can be enabled for OF tunnels.

**Test Steps and Pass Criteria**

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the BFD monitoring is enabled.
   #. Invoke BFD enable RPC.
   #. Verify reference count increases by one.
   #. Verify reference count increases accordingly by invoking multiple times.

Verify reference count with BFD disable RPC
-------------------------------------------
Verify BFD monitoring can be enabled for OF tunnels.

**Test Steps and Pass Criteria**

#. Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the BFD monitoring is enabled.
   #. Invoke BFD disable RPC.
   #. Verify reference count decreases by one.
   #. Verify reference count decreases accordingly by invoking multiple times.

Disable BFD Monitoring for OF Tunnels
--------------------------------------
Verify BFD monitoring can be disabled for OF tunnels

**Test Steps and Pass Criteria**

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
