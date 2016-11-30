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

* OVS and other TOR switches that support OVSDB can be configured without requring
  TEP configuration through REST API. But, user has to use ITM REST APIs for TEP
  configuration on OVS switches, which makes this process cumbersome and error-prone.

Use Cases
---------
This feature will support following use cases:

* Use case 1: Allow user to add tep from southbound interface(SBI).
* Use case 2: Allow user to delete tep from SBI.
* Use case 3: Allow user to update the transport zone from SBI.
* Use case 4: Allow user to update the Datapath Node (DPN) bridge from SBI.
* Use case 5: TEP-IP, Port, vlan, subnet, gateway IP are optional parameters
  for creating a transport zone from REST.
* Use case 6: User must configure Transport zone name and tunnel type parameters
  while creating a transport zone from REST, as both are mandatory parameters.
* Use case 7: Store TEP with not-hosted transport-zone into
  ``tepsNotHostedInTransportZone`` list.
* Use case 8: Move TEP into ``transport-zone`` from `tepsNotHostedInTransportZone``
  list when not-hosted ``transport-zone`` is created.

Following use cases will not be supported:

* If a switch gets disconnected, the corresponding TEP entries will not get cleared
  off from the ITM config datastore (DS) and operator must explicitly clean it up.
* Operator is not supposed to delete ``default-transport-zone`` from rest, such
  scenario will be taken as incorrect configuration.
* Dynamic change in the bridge for tunnel creation via change in Openvswitch table’s
  other_config parameter ``dpn-br-name`` is not supported.

Proposed change
===============
1. ITM will create a default transport zone upon initialization with

  #. Default transport zone will be created with name ``default-transport-zone``.
  #. Tunnel type: ITM will take Virtual Extensible Local Area Network (VXLAN) as
     the default tunnel type for ``default-transport-zone``.
  #. Monitoring protocol type: ITM will take Bidirectional Forwarding Detection (BFD) as
     default tunnel monitor protocol for ``default-transport-zone``.
  #. Monitoring interval: ITM will take 10000 as default value for
     ``default-transport-zone``.

2. When transport-zone is added from northbound i.e. REST interface.
   Few of the transport-zone parameters are mandatory, one is optional
   and fewer are deprecated now.

  #. Mandatory transport zone Parameters: transport-zone name, tunnel type
  #. Optional Parameters: ip-address
  #. Deprecated parameters: subnet prefix, dpn-id, gateway-ip, vlan-id, portname
  #. When a new transport zone is created check for any TEPs if present in
     ``tepsNotHostedInTransportZone`` for that transport zone. If present,
     remove from ``tepsNotHostedInTransportZone`` and add them under the
     transport zone and include the TEP in the tunnel mesh.

3. ITM will register listeners to the Node of network topology Operational DS
   to receive Data Tree Change Notification (DTCN) for add/update/delete notification
   in the OVSDB node so that such DTCN can be parsed and changes in the ``other_config``
   for TEP parameters can be determined to perform TEP add/update/delete operations.

**URL:** restconf/operational/network-topology:network-topology/topology/ovsdb:1

**Sample JSON output**

.. code-block:: json
   :emphasize-lines: 32,33,36,37

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
            "local-ip": "10.164.225.140",
            "local-port": 6640,
            "remote-ip": "10.164.225.143"
          },
          "ovsdb:openvswitch-other-configs": [
            {
              "other-config-key": "tep-ip",
              "other-config-value": "20.0.0.1"
            },
            {
              "other-config-key": "tzname",
              "other-config-value": "TZA"
            }
          ],
          "ovsdb:interface-type-entry": [
            {
              "interface-type": "ovsdb:interface-type-patch"
            },
            {
              "interface-type": "ovsdb:interface-type-vxlan"
            },
            {
              "interface-type": "ovsdb:interface-type-geneve"
            },
            {
              "interface-type": "ovsdb:interface-type-gre"
            },
            {
              "interface-type": "ovsdb:interface-type-system"
            },
            {
              "interface-type": "ovsdb:interface-type-ipsec-gre"
            },
            {
              "interface-type": "ovsdb:interface-type-stt"
            },
            {
              "interface-type": "ovsdb:interface-type-tap"
            },
            {
              "interface-type": "ovsdb:interface-type-lisp"
            },
            {
              "interface-type": "ovsdb:interface-type-internal"
            }
          ],
          "ovsdb:manager-entry": [
            {
              "target": "tcp:10.164.225.140:6640",
              "connected": true,
              "number_of_connections": 1
            }
          ],
          "ovsdb:managed-node-entry": [
            {
              "bridge-ref": "/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='ovsdb://uuid/83192e6c-488a-4f34-9197-d5a88676f04f/bridge/br-int']"
            }
          ]
        },
        {
          "node-id": "ovsdb://uuid/83192e6c-488a-4f34-9197-d5a88676f04f/bridge/br-int",
          "ovsdb:bridge-other-configs": [
            {
              "bridge-other-config-key": "disable-in-band",
              "bridge-other-config-value": "true"
            },
            {
              "bridge-other-config-key": "hwaddr",
              "bridge-other-config-value": "f4:cb:16:28:ce:10"
            }
          ],
          "ovsdb:bridge-uuid": "079026b6-1fad-4164-9a90-b405c719f4ba",
          "ovsdb:stp_enable": false,
          "ovsdb:bridge-external-ids": [
            {
              "bridge-external-id-key": "opendaylight-iid",
              "bridge-external-id-value": "/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='ovsdb://uuid/83192e6c-488a-4f34-9197-d5a88676f04f/bridge/br-int']"
            }
          ],
          "ovsdb:datapath-type": "ovsdb:datapath-type-system",
          "ovsdb:protocol-entry": [
            {
              "protocol": "ovsdb:ovsdb-bridge-protocol-openflow-13"
            }
          ],
          "ovsdb:fail-mode": "ovsdb:ovsdb-fail-mode-secure",
          "ovsdb:managed-by": "/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='ovsdb://uuid/83192e6c-488a-4f34-9197-d5a88676f04f']",
          "ovsdb:bridge-name": "br-int",
          "ovsdb:controller-entry": [
            {
              "target": "tcp:10.164.225.140:6633",
              "is-connected": true,
              "controller-uuid": "24485030-5f52-4f94-ab79-675ccc1ce158"
            },
            {
              "target": "tcp:10.164.225.140:6653",
              "is-connected": false,
              "controller-uuid": "7cfed064-4b0d-4933-b2cf-9543f3005aca"
            }
          ],
          "ovsdb:datapath-id": "00:00:f4:cb:16:28:ce:10",
          "termination-point": [
            {
              "tp-id": "br-int",
              "ovsdb:port-uuid": "9294031a-ce93-4cb0-9f35-152282b70543",
              "ovsdb:interface-uuid": "18dc2153-e28e-401b-9285-497f93ed18d4",
              "ovsdb:ingress-policing-rate": 0,
              "ovsdb:ingress-policing-burst": 0,
              "ovsdb:name": "br-int",
              "ovsdb:interface-type": "ovsdb:interface-type-internal",
              "ovsdb:ifindex": 29,
              "ovsdb:ofport": 65534
            }
          ]
        }
      ]
      }
     ]
    }

TEP Addition
------------
When TEP parameters are configured at OVS side, then TEP parameters details are passed
to the OVSDB plugin via OVSDB connection which in turn, is updated into Network Topology
Operational DS. ITM listens for change in Network Topology Node.

When TEP parameters (like ``tep-ip``, ``tzname``, ``dpn-br-name``) are received in add
notification of Ovsdb Node, then TEP is added.

For TEP addition, TEP-IP and DPN-ID are mandatory. TEP-IP is obtained from ``tep-ip``
TEP parameter and DPN-ID is fetched from OVSDB node based on ``dpn-br-name`` TEP parameter:

* if bridge name is specified, then datapath ID of the specified bridge is fetched.
* if bridge name is not specified, then datapath ID of the ``br-int`` bridge is fetched.

TEP-IP and fetched DPN-ID would be needed to add TEP in the transport-zone.
Once TEP is added in config datastore, transport-zone listener of ITM would
internally take care of creating tunnels on the bridge whose DPN-ID is
passed for TEP addition.

TEP would be added under transport zone with following conditions:

* TEPs not configured with ``other_config:tzname`` i.e. without transport zone will be
  placed under the default transport zone. This will fire a DTCN to transport zone yang
  listener and the ITM tunnels gets built.
* TEPs configured with ``other_config:tzname`` i.e. with transport zone and
  if the specified transport zone exists in the ITM Config DS, then TEP will
  be placed under the specified transport zone. This will fire a DTCN to
  transport zone yang listener and the ITM tunnels gets built.
* TEPs configured with ``other_config:tzname`` i.e. with transport zone and
  if the specified transport zone does not exist in the ITM Config DS, then
  TEP will be placed under the ``tepsNotHostedInTransportZone`` under ITM
  config DS. In this case, when the not hosted transport zone gets created
  from northbound i.e. REST call, then “orphan” TEPs are removed from
  ``tepsNotHostedInTransportZone``, and then stored under the specific transport
  zone in ITM config DS and then TEPs are added to the tunnel mesh of that transport zone.

TEP Updation
------------
* TEP updation for IP address is considered as TEP deletion followed by TEP addition.
* TEP updation for transport zone can be done dynamically. When ``other_config:tzname``
  is updated at OVS side, then such change will be notified to OVSDB plugin via OVSDB
  protocol, which in turn is reflected in Network topology Operational DS. ITM gets
  DTCN for Node update. Parsing Node update notification for ``other_config:tzname``
  parameter in old and new node can determine change in transport zone for TEP.
  If it is updated, then TEP is deleted from old transport zone and added into new
  transport zone. This will fire a DTCN to transport zone yang listener and
  the ITM tunnels gets updated.

TEP Deletion
------------
When an ``openvswitch:other_config:tep-ip`` parameter gets deleted through ``ovs-vsctl``
command, then network topology Operational DS gets updated via OVSB update notification.
ITM which has registered for the network-topology DTCNs, gets notified and this deletes
the TEP from Transport zone or ``tepsNotHostedInTransportZone`` stored in ITM config DS
based on ``other_config:tzname`` parameter configured for TEP.

* If ``other_config:tzname`` is configured and corresponding transport zone exists
  in Configuration DS, then remove TEP from transport zone. This will fire a DTCN
  to transport zone yang listener and the ITM tunnels of that TEP gets deleted.
* If ``other_config:tzname`` is configured and corresponding transport zone does not
  exist in Configuration DS, then check if TEP exists in ``tepsNotHostedInTransportZone``,
  if present, then remove TEP from ``tepsNotHostedInTransportZone``.
* If ``other_config:tzname`` is not configured, then check if TEP exists in the default
  transport zone in Configuration DS, if present, then remove TEP from default transport
  zone. This will fire a DTCN to transport zone yang listener and the ITM tunnels of
  that TEP gets deleted.

OVSDB changes
-------------
Below table covers how ITM TEP parameter are mapped with OVSDB and which fields of
OVSDB would provide ITM TEP parameter values.

====================      ==================================================================
ITM TEP parameter         OVSDB field
====================      ==================================================================
DPN-ID                    ``ovsdb:datapath-id`` from bridge whose name is pre-configured
                          with ``openvswitch:other_config:dpn-br-name``:``value``.

IP-Address                ``openvswitch:other_config:tep-ip``:``value``

Transport Zone Name       ``openvswitch:other_config:tzname``:``value``
====================      ==================================================================

NOTE: If ``openvswitch:other_config:dpn-br-name`` is not configured, then by default
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
Changes will be needed in ``itm.yang``.

ITM YANG changes
^^^^^^^^^^^^^^^^
1. A new list ``tepsNotHostedInTransportZone`` will be added to container
   ``transport-zones`` for storing details of TEP received from southbound
   having transport zone which is not yet hosted from northbound.
2. Existing list ``transport-zone`` would be modified for leaf ``zone-name``
   and ``tunnel-type`` to make them mandatory parameters.

.. code-block:: none
   :caption: itm.yang
   :emphasize-lines: 6,12,16-30

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
        }
    }

Workflow
--------
Adding tep
^^^^^^^^^^

#. User: Specify TEP IP ``other_config:tep-ip`` and ``other_config:tzname`` using
   ovs-vsctl commands to add TEP.
#. ITM: Based on ``other_config:tzname``, TEP is added either in ``transport-zone`` or
   ``tepsNotHostedInTransportZone`` in ITM config DS.

  * If TEP not configured with ``other_config:tzname``, it will be stored under the
    ``default-transport-zone``.
  * If TEPs configured with ``other_config:tzname`` and specified transport zone exists
    in the ITM Config DS, then TEP will be stored under the specified transport zone.
  * If TEPs configured with ``other_config:tzname`` and if the specified transport zone
    does not exist in the ITM Config DS, then TEP will be stored under the
    ``tepsNotHostedInTransportZone`` in ITM config DS.

Deleting tep
^^^^^^^^^^^^

#. User: Remove the TEP IP ``other_config:tep-ip`` using ovs-vsctl commands
   to delete TEP.
#. ITM: Based on ``other_config:tzname`` configuration done while TEP addition,
   TEP is deleted either from ``transport-zone`` or ``tepsNotHostedInTransportZone``
   in ITM config DS.

  * If ``other_config:tzname`` is configured and corresponding transport zone exists
    in Configuration DS, then remove TEP from transport zone.
  * If ``other_config:tzname`` is configured and corresponding transport zone does not
    exist in Configuration DS, then check if TEP exists in ``tepsNotHostedInTransportZone``,
    if present, then remove TEP from ``tepsNotHostedInTransportZone``.
  * If ``other_config:tzname`` is not configured, then check if TEP exists in the
    ``default-transport-zone`` in Configuration DS, if present, then remove TEP
    from ``default-transport-zone``.

Updating tep
^^^^^^^^^^^^

* Update IP of TEP

  #. User: Remove existing TEP-IP ``other_config:tep-ip`` and then add new TEP-IP using ovs-vsctl commands.
  #. ITM: TEP with old TEP-IP is deleted and then TEP with new TEP-IP gets added.

* Update transport zone of TEP

  #. User: Change TEP's transport zone ``other_config:tzname`` using ovs-vsctl commands.
  #. ITM: TEP is deleted from old transport zone and added into new transport zone.

Moving tep
^^^^^^^^^^

* Add TEP into ``tepsNotHostedInTransportZone`` list

  #. User: Specify TEP IP ``other_config:tep-ip`` and not-configured transport zone
     ``other_config:tzname`` using ovs-vsctl commands.
  #. ITM: TEP will be stored into ``tepsNotHostedInTransportZone`` list.

* Move TEP into configured transport-zone

  #. User: Create transport zone which was not configured earlier through REST
  #. ITM: TEP corresponding to transport-zone will be moved from ``tepsNotHostedInTransportZone``
     list to configured ``transport-zone``.

Configuration impact
---------------------
This feature should be used when configuration flag for automatic tunnel creation
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
  :emphasize-lines: 9,13-15,19,23,24,28

  DESCRIPTION
    ovs-vsctl
    command for querying and configuring ovs-vswitchd by providing a
    high-level interface to its configuration database.
    Here, this command usage is shown to store TEP parameters into
    ``openvswitch`` table of OVS database.

  SYNTAX
    ovs-vsctl  set O . [column]:[key]=[value]

  * To set TEP params on OVS table:

  ovs-vsctl    set O . other_config:tep-ip=192.168.56.102
  ovs-vsctl    set O . other_config:tzname=TZA
  ovs-vsctl    set O . other_config:dpn-br-name=br0

  * To clear TEP params in one go by clearing other_config column from
    OVS table:

  ovs-vsctl clear O . other_config

  * To clear specific TEP paramter from other_config column in OVS table:

  ovs-vsctl remove O . other_config tep-ip
  ovs-vsctl remove O . other_config tzname

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

Developer Guide will need to capture how to use changes in ITM to create
tunnel automatically for TEPs configured from southbound.

References
==========
* https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan
