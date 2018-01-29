
.. contents:: Table of Contents
      :depth: 3

===============
ITM Scalability
===============

This document serves as the test plan for the ITM Scalability – OF Based tunnels.  This document comprises of
test cases pertaining to all the use case covered by the Functional Spec.

.. note::

  Name of suite and test cases should map exactly to as they appear in Robot reports.


Test Setup
==========

Brief description of test setup.

Testbed Topologies
------------------
Topology device software and inter node communication details -

#. **ODL Node** – 1 or 3 Node ODL Environment should be used
#. **Switch Node** - 2 or 3 Nodes with OVS 2.6

Test Topology
^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------

#. 1 controller with 2 OVS for functional testing
#. 3 controller with 2 OVS for functional testing

Software Requirements
---------------------
N.A.

Test Suite Requirements
=======================

Test Suite Bringup
------------------

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

Verify Tunnel Creation with enabled IFM Bypass
----------------------------------------------
Change the config parameter to enable IFM Bypass of ITM provisioning and Verify Tunnel Creation is successful.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
   #. Change the configuration parameter as per the new way of ITM provisioning.
   #. Verify the tunnel creation is successful.


Verify Tunnel Creation with disabled IFM Bypass
-----------------------------------------------
Change the config parameter to enable without IFM Bypass of ITM provisioning and Verify Tunnel Creation is successful.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
   #. Change the configuration parameter as per the old way of ITM provisioning.
   #. Verify the tunnel creation is successful.


Change ITM provisioning parameter to enable IFM Bypass
------------------------------------------------------
Clean up existing ITM config, change ITM provisioning parameter to provide IFM Bypass, Verify ITM creation succeeds.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Check for any existing ITM configuration in the system.
   #. Do a clean up of all the existing ITM configuration.
   #. Configure the ITM as per the new way of provisioning.
   #. Verify the tunnel creation is successful.

Change ITM provisioning parameter to disable IFM Bypass
-------------------------------------------------------
Clean up existing ITM config, change ITM provisioning parameter to disable IFM Bypass, Verify ITM creation succeeds.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Check for any existing ITM configuration in the system.
   #. Do a clean up of all the existing ITM configuration.
   #. Configure the ITM as per the old way of provisioning
   #. Verify the tunnel creation is successful


Bring DOWN the datapath
-----------------------
Configure ITM tunnel Mesh, Bring DOWN the datapath and Verify Tunnel status is updated in ODL.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
   #. Configure the ITM tunnel mesh.
   #. Verify the tunnel creation is successful.
   #. Bring down the datapath on the system.
   #. Verify the tunnel status is updated in ODL.


Bring UP the datapath
------------------------
Configure ITM tunnel Mesh, Bring UP the datapath and Verify Tunnel status is updated in ODL.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Configure the ITM tunnel mesh.
   #. Verify the tunnel creation is successful.
   #. Bring UP the datapath on the system.
   #. Verify the tunnel status is updated in ODL.


Enable BFD Monitoring for ITM Tunnels
-------------------------------------
Change ITM config parameters to enable IFM Bypass and Verify BFD monitoring can be enabled for ITM tunnels.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. And configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Verify whether the BFD monitoring is enabled.


Disable BFD Monitoring for ITM Tunnels
--------------------------------------
Change ITM config parameters to enable IFM Bypass and Verify BFD monitoring can be disabled for ITM tunnels

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Disable BFD monitoring.
   #. Verify whether the BFD monitoring is disabled


Enable/Disable BFD to verify tunnel status alarm
------------------------------------------------
Enable BFD and check for the data path alarm and as well as control path alarms.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Verify whether the BFD monitoring is enabled.
   #. Bring down the tunnel and check for the Alarms.
   #. Disable alarm support and verify whether alarm is not reporting.


Verify Tunnel down alarm is reported
------------------------------------
Enable Tunnel status alarm and Bring down the Tunnel port, and verify Tunnel down alarm is reported.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Verify whether the BFD monitoring is enabled.
   #. Enable the alarms for the tunnel UP/DOWN notification.
   #. Bring down the tunnel and check for the Alarms.


Verify Tunnel status for the Disconnected DPN
---------------------------------------------
Disconnect DPN from ODL and verify Tunnel status is shown as UNKNOWN for the Disconnected DPN.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. And configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Disconnect the DPN from the ODL.
   #. Verify tunnel status is shown as ‘UNKNOWN’  for the disconnected DPN.


Verify Tunnel down alarm is cleared
-----------------------------------
Enable Tunnel status alarm and Bring up the Tunnel port which is down, and verify Tunnel down alarm is cleared.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. And configure the tunnel monitoring to BFD.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Verify the tunnel creation is successful.
   #. Enable the alarms for the tunnel UP/DOWN notification.
   #. Bring ‘DOWN’ the tunnel and check for the alarm notification.


Perform ODL reboot
------------------
Create ITM with provisioning config parameter set to true, Perform ODL reboot and Verify dataplane is intact.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Do a ODL Reboot.
   #. Verify the dataplane is intact.


Verify Re-sync is successful once connection is up
--------------------------------------------------
Create ITM with provisioning config parameter set to true for IFM Bypass, bring down control plane
connection(between ODL--OVS), modify ODL config, Verify Re-sync is successful once connection is up.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Bring up the ITM config as per the new way of provisioning.
   #. Bring down the control plane connection between ODL – OVS.
   #. Modify ODL configuration.
   #. Check whether the Re-sync is successful once the connection is UP.


Verify ITM creation with 2 DPNs
-------------------------------

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Check for any existing ITM configuration in the system.
   #. Do a clean up of all the existing ITM configuration.
   #. Configure the ITM as per the old way of provisioning
   #. Verify the tunnel creation is successful


Verify TEP Creation
-------------------
Add new TEP's and verify Creation is successful

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
   #. Add new TEP’s to the existing configuration.
   #. Monitor the time taken for tunnel addition and flow programming.
   #. Verify the tunnel creation is successful.


Verify TEP Deletion
-------------------
Delete few TEP's and verify Deletion is successful and no stale(flows,config) is left.

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
   #. Delete the newly added TEP configuration.
   #. Monitor the time taken for tunnel deletion and flow re-programming.
   #. Verify the deletion is successful and no stale entries left.


Verify ITM creation by Re-adding TEPs
-------------------------------------
Re-add deleted TEP's and Verify ITM creation is successful

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points  with VxLan Encapsulation.
   #. Re-add the deleted TEP entries
   #. Monitor the time taken for tunnel re-addition and flow programming
   #. Verify the tunnel creation is successful.


Verify Deletion of All TEPs
---------------------------
Delete all TEP's and verify Deletion is successful and no stale(flows,config) is left

Test Steps and Pass Criteria
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Create the VxLAN Tunnels between the OVS to OVS.

   #. Verify that the tunnels are built properly between all the End Points with VxLan Encapsulation.
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
  Nidhi Adhvaryu,
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
