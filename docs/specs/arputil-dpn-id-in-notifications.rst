.. contents:: Table of Contents
      :depth: 3

===========================
DPN ID In ARP Notifications
===========================

https://git.opendaylight.org/gerrit/#/c/75248/

The Genius arputil component provides a notification service for ARP packets forwarded from switches via OpenFlow packet-in events. This change adds the switch's datapath ID to the notifications.


Problem description
===================

This change resolves the fact that the switch datapath ID is not copied from the OpenFlow packet-in event to the ARP notification sent by Genius arputil.

Use Cases
---------

This change is primarily introduced to correctly support assigning a FIP to an Octavia VIP:

https://jira.opendaylight.org/browse/NETVIRT-1402

An Octavia VIP is a Neutron port that is not bound to any VM and is therefor not added to br-int. The VM containing the active HaProxy sends gratuitous ARPs for the VIP's IP and ODL intercepts those and programs flows to forward VM traffic to the VMs port.  

The ODL code responsible for configuring the FIP association flows on OVS currently relies on a southbound openflow port that corresponds to the neutron FIP port. The only real reason this is required is so that ODL can decide which switch should get the flows. In the case of the VIP port, there is no corresponding southbound port so the flows never get configured.

To resolve this, ODL can know which switch to program the flows on from the gratuitous ARP packet-in event which will come from the right switch (we already listen for those.) So, basically we just respond to the gratuitous ARP by correlating it with the Neutron port, checking that the port is an Octavia VIP (the owner field), and programming the flows.

Proposed change
===============

* Add dpn-id fields to the the arp-request-received and arp-response-received yang notifications
* Extract the datapath ID from the PacketReceived's ingress field and set it in the notification

Pipeline changes
----------------
N/A

Yang changes
------------
In arputil-api, add dpn-id fields to the the arp-request-received and arp-response-received yang notifications

Configuration impact
---------------------
N/A

Clustering considerations
-------------------------
N/A

Other Infra considerations
--------------------------
N/A

Security considerations
-----------------------
N/A

Scale and Performance Impact
----------------------------
N/A

Targeted Release
-----------------
Nitrogen and preferably backported to Oxygen

Alternatives
------------
N/A

Usage
=====
Consumers of the ARP notifications may call getDpnId() to retrieve the datapath ID of the switch that forwarded the ARP packed to the controller.

Features to Install
-------------------
odl-genius

REST API
--------
This change simply adds a field to an existing yang notification and therefor does not change any APIs.

CLI
---
N/A


Implementation
==============

Assignee(s)
-----------
Josh Hershberg, jhershbe@redhat.com

Work Items
----------
Simple change, see the gerrit patch above.


Dependencies
============
Although ARP notifications are currently consumed by netvirt vpnmanager, this feature is backwards compatible.
A new notification listener that consumes the datapath ID will be added to natmanager to resolve the issue with Octavia mentioned above.

Testing
=======
This feature will be tested as part of the fix to the above mentioned bug. 

Unit Tests
----------
N/A

Integration Tests
-----------------
N/A

CSIT
----
TBD

Documentation Impact
====================
N/A

References
==========
N/A
