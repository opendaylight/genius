
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

TC01 : Create a TZ with TEPs set to use OF Tunnels
--------------------------------------------------
Change the config parameter to enable OF tunnel of ITM provisioning and Verify Tunnel Creation is successful.

**Test Steps and Pass Criteria**

Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify the parent-child interface mapping is established from dpn-teps-state DS.
   #. Verify ingress and egress flows.

TC02 : Delete the TEPs set to use OF Tunnels
---------------------------------------------
Delete the TEPs and verify Deletion is successful and no stale(flows,config) is left.

**Test Steps and Pass Criteria**

Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Delete TEP local ip address from OVS
   #. Verify the deletion is successful and no stale entries left.
   #. Verify the parent-child interface mapping is removed from dpn-teps-state DS.
   #. Verify ingress and egress flows.

TC03 : Create OFPort/TEP on a DPN
---------------------------------
Change the config parameter to enable OF tunnel of ITM provisioning and Verify Tunnel Creation is
successful on a single tep.

**Test Steps and Pass Criteria**

Create the VxLAN Tunnels between the switch_1 and switch_2.

   #. Verify that the tunnels are built properly between the two switches.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels

Create the VxLAN Tunnels by adding in switch_3.

   #. Verify that the tunnels are built properly between the switches.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch even after
         addition of third switch.
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify the parent-child interface mapping is established from dpn-teps-state DS.
   #. Verify ingress and egress flows.

TC04 : Delete OFPort/TEP on a DPN
---------------------------------
Delete a single TEP and verify Deletion is successful and no stale(flows,config) is left.

**Test Steps and Pass Criteria**

Create the VxLAN Tunnels between the switches.

   #. Verify that the tunnels are built properly between all the End Points.
       * tunnels have to be built with VxLAN encapsulation
       * remote_ip should be *flow* instead of destination ip
   #. Delete TEP local ip address from switch_1.
   #. Verify the parent interface of switch_1 is deleted
   #. Verify the parent interface of switch_2 and switch_3 doesnot contain corresponding child
      interface of switch_1.
   #. Verify the ingress and egress flows.

TC05 : Verify OFT supports only selective BFD
---------------------------------------------
Verify only selective BFD monitoring can be enabled for OF tunnels.

**Test Steps and Pass Criteria**

   #. Enable global BFD flag using REST call.
   #. Check for the error message from ITM.

TC06 : Verify reference count with selective BFD
------------------------------------------------
Verify reference count is increased accordingly for selective BFD monitoring.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Invoke BFD enable RPC between required dpns(selective BFD).
   #. Verify reference count increases by one.
   #. Verify P2P tunnel is created between the dpns when count goes from 0 to 1.

TC07 : Verify tunnel state with BFD Monitoring enabled and interface state down
-------------------------------------------------------------------------------
Verify tunnel status with BFd enabled and interface state as down.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the selective BFD monitoring is enabled.
   #. Bring interface state down on dpn for which selective BFD monitoring is enabled.
   #. Verify tunnel state is DOWN in tep-show state for all tunnels related to that dpn.
   #. Verify tunnel status for remaining dpns is unaffected.

TC08 : Verify tunnel state with BFD Monitoring enabled and interface state up
-----------------------------------------------------------------------------
Verify tunnel status with BFd enabled and interface state as up.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the selective BFD monitoring is enabled.
   #. Bring interface state up on dpn for which selective BFD monitoring is enabled.
   #. Verify tunnel state is UP in tep-show state for all tunnels related to that dpn.
   #. Verify tunnel status for remaining dpns is unaffected.

TC09 : Delete VTEP with non-zero reference count
------------------------------------------------
Verify VTEP can be deleted even if the reference count is non-zero.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify whether the BFD monitoring is enabled.
   #. Verify reference count is non-zero.
   #. Delete TEP local ip address from switch_1.
       * verify OF tunnel is deleted
       * verify the P2P tunnel is deleted even if reference count is non-zero
       * verify the parent-child interface mapping is removed from dpn-teps-state DS.
       * verify tunnel-state operational datastore is cleaned up
       * verify flows are cleaned up

TC10 : Verify reference count with BFD disable RPC
--------------------------------------------------
Verify reference count decreases accordingly when BFD disable RPC is called.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify BFD is enabled.
   #. Invoke BFD disable RPC.
   #. Verify reference count decreases by one.
   #. When the reference count reaches zero delete the P2P tunnel.
       * verify the parent-child interface mapping is removed from dpn-teps-state DS.
       * verify tunnel-state operational datastore is cleaned up
       * verify flows are cleaned up

TC11 : Verify interface state with BFD disabled
-----------------------------------------------
Verify that tunnel state remains same even after toggling of interface state once BFD is disabled.

**Test Steps and Pass Criteria**

Configure the tunnel monitoring to BFD.

   #. Verify the tunnel creation is successful.
       * only one tunnel port has to be created per switch
       * tep:show-state should display all the N*N-1 tunnels
   #. Verify BFD is disabled.
   #. Verify reference count is zero.
   #. Bring interface state down.
   #. Verify tunnel state is still UP in tep-show state.
   #. Bring interface state up.
   #. Verify tunnel state is UP in tep-show state.

 Note : When BFD is disabled toggling of interface state should not impact in tep-show state output.

Implementation
==============
N.A.

Assignee(s)
-----------

Primary assignee:
  Faseela K

Other contributors:
  R P Karthika


Work Items
----------
N.A.

Links
-----

https://git.opendaylight.org/gerrit/#/c/79295/

References
==========
https://docs.opendaylight.org/projects/genius/en/latest/specs/of-tunnels.html
