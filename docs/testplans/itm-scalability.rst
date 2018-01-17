
.. contents:: Table of Contents
      :depth: 3

===============
ITM Scalability
===============

This document serves as the test plan for the SF419: ITM Scalability – OF Based tunnels.  This document comprises of
test cases pertaining to all the use case covered by the Functional Spec.

.. note::

  Name of suite and test cases should map exactly to as they appear in Robot reports.


Test Setup
==========

Brief description of test setup.

Testbed Topologies
------------------
Topology device software and inter node communication details -

#. **SDN-C Node** – 3 Node CBA Environment should be used with CSS 3.0 and OVS 2.6
#. **OpenStack/DevStack Node** - Openstack mitaka release should be used for Testing
#. **Switch Node** - The Compute Nodes can Run OVS 2.6 or the CSS 3.0
#. **DC Gateway(SER) node** - SER running latest SEOS and with the reference configuration provided

Test Topology
^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------

#. 3 controller with 3 DPN’s for functional testing
#. 3 Controller with 20 DPN’s for scale testing

Software Requirements
---------------------

#. CSS will be installed on Ubuntu 14.04 operating system
#. OVS 2.6  will be installed on a VM to Emulate the ToR switch


Test Suite Requirements
=======================

Test Suite Bringup
------------------

Initial steps before any tests are run. This should include any cleanup, sanity checks,
configuration etc. that needs to be done before test cases in suite are run. This should
also capture if this suite depends on another suite to do bringup for it.

Test Suite Cleanup
------------------

Final steps after all tests in suite are done. This should include any cleanup, sanity checks,
configuration etc. that needs to be done once all test cases in suite are done.

Debugging
---------

Capture any debugging information that is captured at start of suite and end of suite.


Test Cases
==========

This section should capture high level details about all the test cases part of the suite.Individual test cases should
be subsections in the order they should be executed.

Test Steps and Pass Criteria
----------------------------
Step by step procedure of what is done as part of this test.

#. Change the config parameter to enable new way(IFM Bypass) of ITM provisioning and Verify Tunnel Creation is successful

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
      #. Change the configuration parameter as per the new way of ITM provisioning.
      #. Verify the tunnel creation is successful.

#. Change the config parameter to enable old way of ITM provisioning and Verify Tunnel Creation is successful

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
      #. Change the configuration parameter as per the old way of ITM provisioning.
      #. Verify the tunnel creation is successful.

#. Verify error is thrown, if ITM provisioning config parameter is changed from old-new or vice versa when Tunnel config exists

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
      #. Verify the tunnel creation is successful.
      #. Change the configuration parameter from old to new way of ITM provisioning and vice-versa.
      #. Check for any ‘ERROR’ messages since the tunnel config already exists.

#. Clean up existing ITM config, change ITM provisioning parameter to new way, Verify ITM creation succeeds

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Check for any existing ITM configuration in the system.
      #. Do a clean up of all the existing ITM configuration.
      #. Configure the ITM as per the new way of provisioning.
      #. Verify the tunnel creation is successful.

#. Clean up existing ITM config, change ITM provisioning parameter to old way, Verify ITM creation succeeds.

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Check for any existing ITM configuration in the system.
      #. Do a clean up of all the existing ITM configuration.
      #. Configure the ITM as per the old way of provisioning
      #. Verify the tunnel creation is successful

#. Configure ITM tunnel Mesh, Bring DOWN the datapath and Verify Tunnel status is updated in ODL

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
      #. Configure the ITM tunnel mesh.
      #. Verify the tunnel creation is successful.
      #. Bring down the datapath on the system.
      #. Verify the tunnel status is updated in ODL.

#. Configure ITM tunnel Mesh, Bring UP the datapath and Verify Tunnel status is updated in ODL.

   #. **Test Procedure**

      #. Build the topology as described in the Test Topology.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Configure the ITM tunnel mesh.
      #. Verify the tunnel creation is successful.
      #. Bring UP the datapath on the system.
      #. Verify the tunnel status is updated in ODL.


#. Change ITM config parameters to new way of provisioning and Verify BFD monitoring can be enabled for ITM tunnels

   #. **Test Procedure**

      #. Bring up the topology and bring all the tunnels UP.
      #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Verify whether the BFD monitoring is enabled.


#. Change ITM config parameters to new way of provisioning and Verify BFD monitoring can be disabled for ITM tunnels

   #. **Test Procedure**

      #. Bring up the topology and bring all the tunnels UP.
      #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Disable BFD monitoring.
      #. Verify whether the BFD monitoring is disabled


#. Verify support to enable/disable tunnel status alarm

   #. **Test Procedure**

      #. Bring up the topology and bring all the tunnels UP.
      #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Verify whether the BFD monitoring is enabled.
      #. Bring down the tunnel and check for the Alarms.
      #. Disable alarm support and verify whether alarm is not reporting.


#. Enable Tunnel status alarm and Bring down the Tunnel port, and verify Tunnel down alarm is reported.

   #. **Test Procedure**

     #. Bring up the topology and bring all the tunnels UP.
     #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Verify whether the BFD monitoring is enabled.
      #. Enable the alarms for the tunnel UP/DOWN notification.
      #. Bring down the tunnel and check for the Alarms.

#. Disconnect DPN from SDNC and verify Tunnel status is shown as UNKNOWN for the Disconnected DPN

   #. **Test Procedure**

      #. Bring up the topology and bring all the tunnels UP.
      #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Disconnect the DPN from the SDNC.
      #. Verify tunnel status is shown as ‘UNKNOWN’  for the disconnected DPN.


#. Enable Tunnel status alarm and Bring up the Tunnel port which is down, and verify Tunnel down alarm is cleared

   #. **Test Procedure**

      #. Bring up the topology and bring all the tunnels UP.
      #. And configure the tunnel monitoring to BFD.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Verify the tunnel creation is successful.
      #. Enable the alarms for the tunnel UP/DOWN notification.
      #. Bring ‘DOWN’ the tunnel and check for the alarm notification.


#. Enable new way of Tunnel provisioning , create Tunnel mesh and perform upgrade , verify Tunnel configuration
   parameters are retained

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Create tunnel mesh .
      #. Perform Release Upgrade on the system.
      #. Verify the tunnel configuration parameters are retained.
      #. Verify the tunnel status.


#. Configure ITM without SF419 changes, upgrade to build with SF419.1 support, Verify upgrade succeeds and Tunnels
   status persists

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the old way of provisioning.
      #. Perform Release Upgrade to the build with SF419.1 support.
      #. Verify the tunnel configuration parameters exists.
      #. Verify the tunnel status.


#. Create ITM with new way of provisioning set to true, Perform Cluster reboot and Verify Traffic and dataplane is intact.

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Do a cluster Reboot.
      #. Verify the traffic and dataplane is intact.


#. Create ITM with new way of provisioning set to true, Perform controller(CIC) reboot and Verify Traffic and dataplane is intact

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Do a CIC Reboot.
      #. Verify the traffic.
      #. Verify whether the dataplane is intact and no traffic loss found.

#. Create ITM with new way of provisioning set to true, bring down control plane connection(between SDNC--CSS), modify
   SDNC config, Verify Re-sync is successful once connection is up

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Bring up the ITM config as per the new way of provisioning.
      #. Bring down the control plane connection between SDNC – CSS.
      #. Modify SDNC configuration.
      #. Check whether the Re-sync is successful once the connection is UP.


#. Verify ITM creation with 20/30 DPNs

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology1.
      #. Create the VxLAN Tunnels between the CSS to CSS

   #. **Pass Criteria**

      #. Check for any existing ITM configuration in the system.
      #. Do a clean up of all the existing ITM configuration.
      #. Configure the ITM as per the old way of provisioning
      #. Verify the tunnel creation is successful

#. Add 5 new TEP's and verify Creation is successful

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology2.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
      #. Add 5 new TEP’s to the existing configuration.
      #. Monitor the time taken for tunnel addition and flow programming.
      #. Verify the tunnel creation is successful.


#. Delete few TEP's and verify Deletion is successful and no stale(flows,config) is left.

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology2.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
      #. Delete the 5 newly added TEP configuration.
      #. Monitor the time taken for tunnel deletion and flow re-programming.
      #. Verify the deletion is successful and no stale entries left.


#. Re-add deleted TEP's and Verify ITM creation is successful.

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology2.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
      #. Re-add the deleted TEP entries
      #. Monitor the time taken for tunnel re-addition and flow programming
      #. Verify the tunnel creation is successful.


#. Delete all TEP's and verify Deletion is successful and no stale(flows,config) is left

   #. **Test Procedure**

      #. Build the topology as described in the Test_Topology2.
      #. Create the VxLAN Tunnels between the OVS to OVS.

   #. **Pass Criteria**

      #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
      #. Delete all the TEP entries.
      #. Monitor the time taken for tunnel deletion and flow re-programming
      #. Verify the deletion is successful and no stale entries left.


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
  Nidhi Adhvaryu
  Sathwik Boggarapu


Work Items
----------
N.A.

Links
-----

* Link to implementation patche(s) in CSIT

References
==========
N.A.
