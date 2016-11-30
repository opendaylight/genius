=============================
ITM Tunnel Auto-Configuration
=============================

https://git.opendaylight.org/gerrit/#/q/topic:itm-auto-config

Internal Transport Manager (ITM) Tunnel Auto configuration feature  proposes a
solution to migrate from REST/CLI based Tunnel End Point (TEP) configuration to
automatic learning of Openvswitch (OVS) TEPs from the switches, thereby triggering
automatic configuration of tunnels.

Problem description
===================

* User has to use ITM REST APIs for addition/deletion of TEPs into/from Transport zone.
  But, OVS and other TOR switches that support OVSDB can be configured for TEP without
  requring TEP configuration through REST API, which leads to redundancy and makes the process
  cumbersome and error-prone.

Use Cases
---------
This feature will support following use cases:

* Use case 1: Add tep to existing transport-zone from southbound interface(SBI).
* Use case 2: Delete tep from SBI.
* Use case 3: Move the tep from one transport zone to another from SBI.
* Use case 4: User can specify the Datapath Node (DPN) bridge for tep other than ``br-int`` from SBI.
* Use case 5: Allow user to configure a tep from SBI if they want to use flow based tunnels.
* Use case 6: TEP-IP, Port, vlan, subnet, gateway IP are optional parameters
  for creating a transport zone from REST.
* Use case 7: User must configure Transport zone name and tunnel type parameters
  while creating a transport zone from REST, as both are mandatory parameters.
* Use case 8: Store teps received on OVS connect for transport-zone which is not yet created and also
  allow to move such teps into transport-zone when it gets created from northbound.
* Use case 9: Create default transport zone during bootup and
  tunnel-type for default transport zone should be configurable through config file.

Following use cases will not be supported:

* If a switch gets disconnected, the corresponding TEP entries will not get cleared
  off from the ITM config datastore (DS) and operator must explicitly clean it up.
* Operator is not supposed to delete ``default-transport-zone`` from REST, such
  scenario will be taken as incorrect configuration.
* Dynamic change in the bridge for tunnel creation via change in Openvswitch table’s
  external_ids parameter ``dpn-br-name`` is not supported.
* Dynamic change for ``of-tunnels`` tep configuration via change in Openvswitch table’s
  external_ids parameter ``of-tunnels`` is not supported.

Proposed change
===============
1. ITM will create a default transport zone upon initialization and configured with:

  * Default transport zone will be created with name ``default-transport-zone``.
  * Tunnel type: This would be configurable parameter via config file.
    ITM will take tunnel type value from config file for ``default-transport-zone``.
    Tunnel-type value cannot be changed dynamically. It will be taken from config
    file ``genius-itm-config.xml`` on startup.

2. When transport-zone is added from northbound i.e. REST interface.
   Few of the transport-zone parameters are mandatory, one is optional
   and fewer are deprecated now.

  * Mandatory transport zone Parameters: transport-zone name, tunnel type
  * Optional Parameters: ip-address
  * Deprecated parameters: subnet prefix, dpn-id, gateway-ip, vlan-id, portname
  * When a new transport zone is created check for any TEPs if present in
    ``tepsNotHostedInTransportZone`` for that transport zone. If present,
    remove from ``tepsNotHostedInTransportZone`` and add them under the
    transport zone and include the TEP in the tunnel mesh.

3. ITM will register listeners to the Node of network topology Operational DS
   to receive Data Tree Change Notification (DTCN) for add/update/delete notification
   in the OVSDB node so that such DTCN can be parsed and changes in the ``external_ids``
   for TEP parameters can be determined to perform TEP add/update/delete operations.

**URL:** restconf/operational/network-topology:network-topology/topology/ovsdb:1

**Sample JSON output**

.. code-block:: json
   :emphasize-lines: 16,17,20,21,24,25

    {
      "topology": [
        {
          "topology-id": "ovsdb:1",
          "node": [
          {
          "node-id": "ovsdb://uuid/83192e6c-488a-4f34-9197-d5a88676f04f",
          "ovsdb:db-version": "7.12.1",
          "ovsdb:ovs-version": "2.5.0",
          "ovsdb:openvswitch-external-ids": [
            {
              "external-id-key": "system-id",
              "external-id-value": "e93a266a-9399-4881-83ff-27094a648e2b"
            },
            {
              "external-id-key": "tep-ip",
              "external-id-value": "20.0.0.1"
            },
            {
              "external-id-key": "tzname",
              "external-id-value": "TZA"
            },
            {
              "external-id-key": "of-tunnels",
              "external-id-value": "true"
            }
          ],
          "ovsdb:datapath-type-entry": [
            {
              "datapath-type": "ovsdb:datapath-type-system"
            },
            {
              "datapath-type": "ovsdb:datapath-type-netdev"
            }
          ],
          "ovsdb:connection-info": {
            "remote-port": 45230,
            "local-ip": "10.111.222.10",
            "local-port": 6640,
            "remote-ip": "10.111.222.20"
          }

          ...
          ...

         }
        ]
       }
      ]
    }

OVSDB changes
-------------
Below table covers how ITM TEP parameter are mapped with OVSDB and which fields of
OVSDB would provide ITM TEP parameter values.

====================      ==================================================================
ITM TEP parameter         OVSDB field
====================      ==================================================================
DPN-ID                    ``ovsdb:datapath-id`` from bridge whose name is pre-configured
                          with openvswitch:external_ids:dpn-br-name:value

IP-Address                openvswitch:external_ids:tep-ip:value

Transport Zone Name       openvswitch:external_ids:tzname:value

of-tunnels                openvswitch:external_ids:of-tunnels:value
====================      ==================================================================

NOTE: If ``openvswitch:external_ids:dpn-br-name`` is not configured, then by default
``br-int`` will be considered to fetch DPN-ID which in turn would be used for
tunnel creation.

MDSALUtil changes
-----------------
``getDpnId()`` method is added into MDSALUtil.java.

.. code-block:: none
   :emphasize-lines: 9

    /**
     * This method will be utility method to convert bridge datapath ID from
     * string format to BigInteger format.
     *
     * @param datapathId datapath ID of bridge in string format
     *
     * @return the datapathId datapath ID of bridge in BigInteger format
     */
    public static BigInteger getDpnId(String datapathId);

Pipeline changes
----------------
N.A.

Yang changes
------------
Changes will be needed in ``itm.yang`` and ``itm-config.yang``.

itm.yang changes
^^^^^^^^^^^^^^^^

1. A new list ``tepsNotHostedInTransportZone`` will be added to container
   ``transport-zones`` for storing details of TEP received from southbound
   having transport zone which is not yet hosted from northbound.
2. Existing list ``transport-zone`` would be modified for leaf ``zone-name``
   and ``tunnel-type`` to make them mandatory parameters.

.. code-block:: none
   :caption: itm.yang
   :emphasize-lines: 6,12,16-35

    list transport-zone {
        ordered-by user;
        key zone-name;
        leaf zone-name {
            type string;
            mandatory true;
        }
        leaf tunnel-type {
            type identityref {
                base odlif:tunnel-type-base;
            }
            mandatory true;
        }
    }

    list tepsNotHostedInTransportZone {
        key zone-name;
        leaf zone-name {
            type string;
        }
        list unknown-vteps {
            key "dpn-id";
            leaf dpn-id {
                type uint64;
            }
            leaf ip-address {
                type inet:ip-address;
            }
            leaf of-tunnels {
                description "Use flow based tunnels for remote-ip";
                type boolean;
                default false;
            }
        }
    }

itm-config.yang changes
^^^^^^^^^^^^^^^^^^^^^^^

1. New container ``itm-config`` is added to contain following configurable parameters
   which can be configured in ``genius-itm-config.xml`` on startup.

   * ``def-tz-auto-tunnel-enabled``: this is boolean type parameter which would
     enable/disable tunnel mesh formation for ``default-transport-zone``. By default,
     value is ``false``.
   * ``def-tz-tunnel-type``: this is string type parameter which would allow user to
     configure tunnel-type for ``default-transport-zone``. By default, value is ``vxlan``.

.. code-block:: none
   :caption: itm-config.yang
   :emphasize-lines: 1-11

    container itm-config {
       config true;
       leaf def-tz-auto-tunnel-enabled {
          type boolean;
          default false;
       }
       leaf def-tz-tunnel-type {
          type string;
          default "vxlan";
       }
    }

Workflow
--------

TEP Addition
^^^^^^^^^^^^
When TEP IP ``external_ids:tep-ip`` and ``external_ids:tzname`` are configured at OVS side
using ovs-vsctl commands to add TEP, then TEP parameters details are passed to the OVSDB
plugin via OVSDB connection which in turn, is updated into Network Topology Operational DS.
ITM listens for change in Network Topology Node.

When TEP parameters (like ``tep-ip``, ``tzname``, ``dpn-br-name``, ``of-tunnels``) are
received in add notification of OVSDB Node, then TEP is added.

For TEP addition, TEP-IP and DPN-ID are mandatory. TEP-IP is obtained from ``tep-ip``
TEP parameter and DPN-ID is fetched from OVSDB node based on ``dpn-br-name`` TEP parameter:

* if bridge name is specified, then datapath ID of the specified bridge is fetched.
* if bridge name is not specified, then datapath ID of the ``br-int`` bridge is fetched.

TEP-IP and fetched DPN-ID would be needed to add TEP in the transport-zone.
Once TEP is added in config datastore, transport-zone listener of ITM would
internally take care of creating tunnels on the bridge whose DPN-ID is
passed for TEP addition. It is noted that TEP parameter ``of-tunnels`` would be
checked if it is true, then ``of-tunnels`` flag would be set for vtep to be added
under transport-zone or ``tepsNotHostedInTransportZone``.

TEP would be added under transport zone with following conditions:

* TEPs not configured with ``external_ids:tzname`` i.e. without transport zone will be
  placed under the ``default-transport-zone``. This will fire a DTCN to transport zone
  yang listener and the ITM tunnels gets built.
* TEPs configured with ``external_ids:tzname`` i.e. with transport zone and
  if the specified transport zone exists in the ITM Config DS, then TEP will
  be placed under the specified transport zone. This will fire a DTCN to
  transport zone yang listener and the ITM tunnels gets built.
* TEPs configured with ``external_ids:tzname`` i.e. with transport zone and
  if the specified transport zone does not exist in the ITM Config DS, then
  TEP will be placed under the ``tepsNotHostedInTransportZone`` under ITM
  config DS.

TEP Movement
^^^^^^^^^^^^
When transport zone which was not configured earlier, is created through REST, then
it is checked whether any “orphan” TEPs already exists in the ``tepsNotHostedInTransportZone``
for the newly created transport zone, if present, then such TEPs are removed from
``tepsNotHostedInTransportZone``, and then added under the newly created transport zone
in ITM config DS and then TEPs are added to the tunnel mesh of that transport zone.

TEP Updation
^^^^^^^^^^^^
* TEP updation for IP address is considered as TEP deletion followed by TEP addition.
  Remove existing TEP-IP ``external_ids:tep-ip`` and then add new TEP-IP using ovs-vsctl
  commands. TEP with old TEP-IP is deleted and then TEP with new TEP-IP gets added.
* TEP updation for transport zone can be done dynamically. When ``external_ids:tzname``
  is updated at OVS side, then such change will be notified to OVSDB plugin via OVSDB
  protocol, which in turn is reflected in Network topology Operational DS. ITM gets
  DTCN for Node update. Parsing Node update notification for ``external_ids:tzname``
  parameter in old and new node can determine change in transport zone for TEP.
  If it is updated, then TEP is deleted from old transport zone and added into new
  transport zone. This will fire a DTCN to transport zone yang listener and
  the ITM tunnels gets updated.

TEP Deletion
^^^^^^^^^^^^
When an ``openvswitch:external_ids:tep-ip`` parameter gets deleted through ``ovs-vsctl``
command, then network topology Operational DS gets updated via OVSB update notification.
ITM which has registered for the network-topology DTCNs, gets notified and this deletes
the TEP from Transport zone or ``tepsNotHostedInTransportZone`` stored in ITM config DS
based on ``external_ids:tzname`` parameter configured for TEP.

* If ``external_ids:tzname`` is configured and corresponding transport zone exists
  in Configuration DS, then remove TEP from transport zone. This will fire a DTCN
  to transport zone yang listener and the ITM tunnels of that TEP gets deleted.
* If ``external_ids:tzname`` is configured and corresponding transport zone does not
  exist in Configuration DS, then check if TEP exists in ``tepsNotHostedInTransportZone``,
  if present, then remove TEP from ``tepsNotHostedInTransportZone``.
* If ``external_ids:tzname`` is not configured, then check if TEP exists in the default
  transport zone in Configuration DS, if present, then remove TEP from default transport
  zone. This will fire a DTCN to transport zone yang listener and the ITM tunnels of
  that TEP gets deleted.

Configuration impact
---------------------
1. New configuation file named ``genius-itm-config.xml`` is introduced into ITM
   in which following parameters are added:

   * ``def-tz-auto-tunnel-enabled``: this is boolean type parameter which would
     enable/disable tunnel mesh formation for ``default-transport-zone``.
   * ``def-tz-tunnel-type``: this is string type parameter which would allow user to
     configure tunnel-type for ``default-transport-zone``.

.. code-block:: xml
   :caption: genius-itm-config.xml

    <itm-config xmlns="urn:opendaylight:genius:itm:config">
        <def-tz-auto-tunnel-enabled>false</def-tz-auto-tunnel-enabled>
        <def-tz-tunnel-type>vxlan</def-tz-tunnel-type>
    </itm-config>

2. This feature should be used when configuration flag for automatic tunnel creation
in transport-zone is disabled in netvirt.

Clustering considerations
-------------------------
Any clustering requirements are already addressed in ITM, no new requirements added
as part of this feature.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
This feature would not introduce any significant scale and performance issues in the ODL.

Targeted Release
-----------------
ODL Carbon

Known Limitations
-----------------
* Dummy Subnet prefix ``255.255.255.255/32`` under transport-zone is used to store the
  TEPs listened from southbound.

Alternatives
------------
N.A.

Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature. This feature would be available in
already existing ``odl-genius`` karaf feature.

REST API
--------
Creating transport zone
^^^^^^^^^^^^^^^^^^^^^^^

As per this feature, the TEP addition is based on the southbound configuation and
respective transport zone should be created on the controller to form the tunnel
for the same. The REST API to create the transport zone with mandatory parameters.

**URL:** restconf/config/itm:transport-zones/

**Sample JSON data**

.. code-block:: json

    {
        "transport-zone": [
            {
                "zone-name": "TZA",
                 "tunnel-type": "odl-interface:tunnel-type-vxlan"
            }
        ]
    }

Retrieving transport zone
^^^^^^^^^^^^^^^^^^^^^^^^^

To retrieve the TEP configuations from all the transport zones.

**URL:** restconf/config/itm:transport-zones/

**Sample JSON output**

.. code-block:: json

    {
        "transport-zones": {
           "transport-zone": [
              {
                "zone-name": "default-transport-zone",
                "tunnel-type": "odl-interface:tunnel-type-vxlan"
              },
              {
                "zone-name": "TZA",
                "tunnel-type": "odl-interface:tunnel-type-vxlan",
                "subnets": [
                  {
                    "prefix": "255.255.255.255/32",
                    "vteps": [
                      {
                        "dpn-id": 1,
                        "portname": "",
                        "ip-address": "10.0.0.1"
                      },
                      {
                        "dpn-id": 2,
                        "portname": "",
                        "ip-address": "10.0.0.2"
                      }
                    ],
                    "gateway-ip": "0.0.0.0",
                    "vlan-id": 0
                  }
                ]
              }
            ]
        }
    }

CLI
---
No CLI is added into ODL for this feature.

OVS CLI
^^^^^^^
ITM TEP parameters can be added/removed to/from the OVS switch using
the ovs-vsctl command:

.. code-block:: none
  :emphasize-lines: 9,13-16,21,25,26,30

  DESCRIPTION
    ovs-vsctl
    Command for querying and configuring ovs-vswitchd by providing a
    high-level interface to its configuration database.
    Here, this command usage is shown to store TEP parameters into
    ``openvswitch`` table of OVS database.

  SYNTAX
    ovs-vsctl  set O . [column]:[key]=[value]

  * To set TEP params on OVS table:

  ovs-vsctl    set O . external_ids:tep-ip=192.168.56.102
  ovs-vsctl    set O . external_ids:tzname=TZA
  ovs-vsctl    set O . external_ids:dpn-br-name=br0
  ovs-vsctl    set O . external_ids:of-tunnels=true

  * To clear TEP params in one go by clearing external_ids column from
    OVS table:

  ovs-vsctl clear O . external_ids

  * To clear specific TEP paramter from external_ids column in OVS table:

  ovs-vsctl remove O . external_ids tep-ip
  ovs-vsctl remove O . external_ids tzname

  * To check TEP params are set or cleared on OVS table:

  ovsdb-client dump -f list  Open_vSwitch

Implementation
==============

Assignee(s)
-----------

Primary assignee:

* Tarun Thakur

Other contributors:

* Sathish Kumar B T
* Nishchya Gupta
* Jogeswar Reddy

Work Items
----------
#. YANG changes
#. Add code to create ``default-transport-zone`` during bootup.
#. Add code to create xml config file for ITM to configure enable/disable of
   ``automatic tunnel mesh`` and ``tunnel-type`` for ``default-transport-zone``.
#. Add code to create listener for OVSDB to receive TEP-specific
   parameters configured at OVS.
#. Add code to update configuation datastore to add/delete TEP received from
   southbound into transport-zone.
#. Check tunnel mesh for transport-zone is updated correctly for TEP
   add/delete into transport-zone.
#. Add code to update configuation datastore for handling update in TEP-IP.
#. Add code to update configuation datastore for handling update in TEP's transport-zone.
#. Check tunnel mesh is updated correctly against TEP update.
#. Add code to create ``tepsNotHostedInTransportZone`` list in configuation datastore to
   store TEP received with not-configured transport-zone.
#. Add code to move TEP from ``tepsNotHostedInTransportZone`` list to transport-zone
   configured from REST.
#. Check tunnel mesh is formed for TEPs after their movement from ``tepsNotHostedInTransportZone``
   list to transport-zone.
#. Add UTs.
#. Add ITs.
#. Add CSIT.
#. Add Documentation.

Dependencies
============
1. This feature should be used when configuration flag for automatic tunnel creation
   in transport-zone is disabled in netvirt, otherwise netvirt feature of dynamic
   tunnel creation may duplicate tunnel for TEPs in the tunnel mesh.

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in, once UT framework is in place.

Integration Tests
-----------------
Integration tests will be added, once IT framework for ITM is ready.

CSIT
----
Following test cases will need to be added/expanded in Genius CSIT:

#. Verify mandatory parameters for the southbound TEP configuration as tep-ip when
   br-int configured
#. Verify mandatory parameters for the southbound TEP configuration as tep-ip and
   dpn-br-name when br-int is not configured
#. Verify default prefix as 255.255.255.255/32 for southbound TEP configuration
#. Verify mandatory parameters for TEP configuration on ODL as transport-zone
   name and tunnel-type
#. Verify ITM tunnel creation by configuring TEP parameters using REST
#. Verify default transport zone creation in ODL during bootup
#. Verify TEPs with no transport zone configuration from OVS added to default-transport-zone
#. Verify TEPs with transport zone configured from OVS will be added to corresponding
   transport-zone
#. Verify TEPs with unknown transport zone configured from OVS will be added to
   ``tepsNotHostedInTransportZone``
#. Verify auto mapping of OVS to corresponding transport zone group and full mesh
   tunnel formation
#. Verify full mesh tunnel update when adding new OVS to corresponding transport-zone group
#. Verify full mesh tunnel update when deleting TEP from OVS to corresponding
   transport-zone group
#. Verify auto mapping of OVS to default transport zone group and full mesh tunnel formation
#. Verify TEP local ip address delete will delete the tunnels
#. Verify transport-zone configuration with tunnel type VXLAN
#. Verify transport zone configured by OVS register with ODL but no tunnel formation
#. Verify tunnel formation initiates after ITM REST call on ODL with already registered
   transport zones by OVSs
#. Verify TEP transport zone change from OVS will move the TEP to corresponding
   transport-zone in ODL
#. Verify TEP delete from OVS will remove TEP from transport zone in ODL
#. Verify TEP configuration of dpn-br-name from OVS doesn't allow changes
   after connected to ODL
#. Verify the configuration and tunnel details are persist across multiple
   controller restarts
#. Verify the Tunnel mesh are created automatically after OVS restart
#. Verify the Tunnel mesh are created automatically after multiple OVS restart
#. Verify the Tunnel mesh are created automatically after OVS connect and disconnect

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information for below details:

* TEPs parameters to be configured from OVS side to use this feature.
* TEPs added from southbound can be viewed from REST APIs.
* TEPs added from southbound will be added under dummy subnet (255.255.255.255/32) in
  transport-zone.
* Usage details of genius-itm-config.xml config file for ITM to configure enable/disable of
  ``automatic tunnel mesh`` and ``tunnel-type`` for ``default-transport-zone``.

Developer Guide will need to capture how to use changes in ITM to create
tunnel automatically for TEPs configured from southbound.

References
==========
* https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan
