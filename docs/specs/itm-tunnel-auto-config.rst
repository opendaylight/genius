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

User has to use ITM REST APIs for addition/deletion of TEPs into/from Transport zone.
But, OVS and other TOR switches that support OVSDB can be configured for TEP without
requring TEP configuration through REST API, which leads to redundancy and makes the
process cumbersome and error-prone.

Use Cases
---------
This feature will support following use cases:

* Use case 1: Add tep to existing transport-zone from southbound interface(SBI).
* Use case 2: Delete tep from SBI.
* Use case 3: Move the tep from one transport zone to another from SBI.
* Use case 4: User can specify the Datapath Node (DPN) bridge for tep other
  than ``br-int`` from SBI.
* Use case 5: Allow user to configure a tep from SBI if they want to use
  flow based tunnels.
* Use case 6: TEP-IP, Port, vlan, subnet, gateway IP are optional parameters
  for creating a transport zone from REST.
* Use case 7: User must configure Transport zone name and tunnel type parameters
  while creating a transport zone from REST, as both are mandatory parameters.
* Use case 8: Store teps received on OVS connect for transport-zone which is not yet
  created and also allow to move such teps into transport-zone when it gets created
  from northbound.
* Use case 9: Allow user to control creation of default transport zone through
  start-up configurable parameter ``def-tz-enabled`` in config file.
* Use case 10: Tunnel-type for default transport zone should be configurable through
  configurable parameter ``def-tz-tunnel-type`` in config file.
* Use case 11: Allow user to change ``def-tz-enabled`` configurable parameter from
  OFF to ON during OpenDaylight controller restart.
* Use case 12: Allow user to change ``def-tz-enabled`` configurable parameter from
  ON to OFF during OpenDaylight controller restart.
* Use case 13: Default value for configurable parameter ``def-tz-enabled`` is OFF
  and if it is not changed by user, then it will be OFF after OpenDaylight
  controller restart as well.
* Use case 14: Allow dynamic change for ``local_ip`` tep configuration via change in
  Openvswitch table’s other_config parameter ``local_ip``.

Following use cases will not be supported:

* If a switch gets disconnected, the corresponding TEP entries will not get cleared
  off from the ITM config datastore (DS) and operator must explicitly clean it up.
* Operator is not supposed to delete ``default-transport-zone`` from REST, such
  scenario will be taken as incorrect configuration.
* Dynamic change for ``of-tunnel`` tep configuration via change in Openvswitch table’s
  external_ids parameter ``of-tunnel`` is not supported.
* Dynamic change for configurable parameters ``def-tz-enabled`` and
  ``def-tz-tunnel-type`` is not supported.

Proposed change
===============
ITM will create a default transport zone on OpenDaylight start-up if configurable parameter
``def-tz-enabled`` is ``true`` in ``genius-itm-config.xml`` file (by default, this flag
is false). When the flag is true, default transport zone is created and configured with:

* Default transport zone will be created with name ``default-transport-zone``.
* Tunnel type: This would be configurable parameter via config file.
  ITM will take tunnel type value from config file for ``default-transport-zone``.
  Tunnel-type value cannot be changed dynamically. It will take value of
  ``def-tz-tunnel-type`` parameter from config file ``genius-itm-config.xml`` on startup.

  * If ``def-tz-tunnel-type`` parameter is changed and ``def-tz-enabled`` remains ``true``
    during OpenDaylight restart, then ``default-transport-zone`` with previous value of
    tunnel-type would be first removed and then ``default-transport-zone`` would be created
    with newer value of tunnel-type.

If ``def-tz-enabled`` is configured as ``false``, then ITM will delete
``default-transport-zone`` if it is present already.

When transport-zone is added from northbound i.e. REST interface.
Few of the transport-zone parameters are mandatory and fewer are optional now.

====================      =======================================================
Status                    Transport zone parameters
====================      =======================================================
Mandatory                 transport-zone name, tunnel-type

Optional                  TEP IP-Address, Subnet prefix, Dpn-id, Gateway-ip,
                          Vlan-id, Portname
====================      =======================================================

When a new transport zone is created, check for any TEPs if present in
``tepsInNotHostedTransportZone`` container in Oper DS for that transport zone.
If present, remove from ``tepsInNotHostedTransportZone`` and then add them
under the transport zone and include the TEP in the tunnel mesh.

ITM will register listeners to the Node of network topology Operational DS
to receive Data Tree Change Notification (DTCN) for add/update/delete
notification in the OVSDB node so that such DTCN can be parsed and changes
in the ``other_config`` and ``external_ids`` columns of openvswitch table for
TEP parameters can be determined to perform TEP add/update/delete operations.

**URL:** restconf/operational/network-topology:network-topology/topology/ovsdb:1

**Sample JSON output**

.. code-block:: json
   :emphasize-lines: 16,17,20,21,30,31

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
              "external-id-key": "transport-zone",
              "external-id-value": "TZA"
            },
            {
              "external-id-key": "of-tunnel",
              "external-id-value": "true"
            }
          ],
          "ovsdb:openvswitch-other-configs": [
            {
              "other-config-key": "provider_mappings",
              "other-config-value": "physnet1:br-physnet1"
            },
            {
              "other-config-key": "local_ip",
              "other-config-value": "20.0.0.1"
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
                          with openvswitch:external_ids:br-name:value

IP-Address                ``openvswitch:other_config:local_ip``:value

Transport Zone Name       ``openvswitch:external_ids:transport-zone``:value

of-tunnel                 ``openvswitch:external_ids:of-tunnel``:value
====================      ==================================================================

NOTE: If ``openvswitch:external_ids:br-name`` is not configured, then by default
``br-int`` will be considered to fetch DPN-ID which in turn would be used for
tunnel creation. Also, ``openvswitch:external_ids:of-tunnel`` is not required to be
configured, and will default to false, as described below in Yang changes section.

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
Changes are needed in ``itm.yang`` and ``itm-config.yang`` which are described in
below sub-sections.

itm.yang changes
^^^^^^^^^^^^^^^^
Following changes are done in ``itm.yang`` file.

1. A new container ``tepsInNotHostedTransportZone`` under Oper DS will be added
   for storing details of TEP received from southbound having transport zone
   which is not yet hosted from northbound.
2. Existing list ``transport-zone`` would be modified for leaf ``zone-name``
   and ``tunnel-type`` to make them mandatory parameters.

.. code-block:: none
   :caption: itm.yang
   :emphasize-lines: 6,12,16-38

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

    container not-hosted-transport-zones {
        config false;
        list tepsInNotHostedTransportZone {
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
                leaf of-tunnel {
                    description "Use flow based tunnels for remote-ip";
                    type boolean;
                    default false;
                }
            }
        }
    }

itm-config.yang changes
^^^^^^^^^^^^^^^^^^^^^^^

``itm-config.yang`` file is modified to add new container to contain following parameters
which can be configured in ``genius-itm-config.xml`` on OpenDaylight controller startup.

* ``def-tz-enabled``: this is boolean type parameter which would create or delete
  ``default-transport-zone`` if it is configured true or false respectively. By default,
  value is ``false``.
* ``def-tz-tunnel-type``: this is string type parameter which would allow user to
  configure tunnel-type for ``default-transport-zone``. By default, value is ``vxlan``.

.. code-block:: none
   :caption: itm-config.yang
   :emphasize-lines: 1-11

    container itm-config {
       config true;
       leaf def-tz-enabled {
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
When TEP IP ``other_config:local_ip`` and ``external_ids:transport-zone`` are configured
at OVS side using ``ovs-vsctl`` commands to add TEP, then TEP parameters details are
passed to the OVSDB plugin via OVSDB connection which in turn, is updated into Network
Topology Operational DS. ITM listens for change in Network Topology Node.

When TEP parameters (like ``local_ip``, ``transport-zone``, ``br-name``, ``of-tunnel``)
are received in add notification of OVSDB Node, then TEP is added.

For TEP addition, TEP-IP and DPN-ID are mandatory. TEP-IP is obtained from ``local_ip``
TEP parameter and DPN-ID is fetched from OVSDB node based on ``br-name`` TEP parameter:

* if bridge name is specified, then datapath ID of the specified bridge is fetched.
* if bridge name is not specified, then datapath ID of the ``br-int`` bridge is fetched.

TEP-IP and fetched DPN-ID would be needed to add TEP in the transport-zone.
Once TEP is added in config datastore, transport-zone listener of ITM would
internally take care of creating tunnels on the bridge whose DPN-ID is
passed for TEP addition. It is noted that TEP parameter ``of-tunnel`` would be
checked if it is true, then ``of-tunnel`` flag would be set for vtep to be added
under transport-zone or ``tepsInNotHostedTransportZone``.

TEP would be added under transport zone with following conditions:

* TEPs not configured with ``external_ids:transport-zone`` i.e. without transport zone
  will be placed under the ``default-transport-zone`` if ``def-tz-enabled`` parameter
  is configured to true in ``genius-itm-config.xml``. This will fire a DTCN to
  transport zone yang listener and ITM tunnels gets built.
* TEPs configured with ``external_ids:transport-zone`` i.e. with transport zone and
  if the specified transport zone exists in the ITM Config DS, then TEP will
  be placed under the specified transport zone. This will fire a DTCN to
  transport zone yang listener and the ITM tunnels gets built.
* TEPs configured with ``external_ids:transport-zone`` i.e. with transport zone and
  if the specified transport zone does not exist in the ITM Config DS, then
  TEP will be placed under the ``tepsInNotHostedTransportZone`` container under ITM
  Oper DS.

TEP Movement
^^^^^^^^^^^^
When transport zone which was not configured earlier, is created through REST, then
it is checked whether any “orphan” TEPs already exists in the
``tepsInNotHostedTransportZone`` for the newly created transport zone, if present,
then such TEPs are removed from ``tepsInNotHostedTransportZone`` container
in Oper DS, and then added under the newly created transport zone in ITM config DS
and then TEPs are added to the tunnel mesh of that transport zone.

TEP Updation
^^^^^^^^^^^^
* TEP updation for IP address can be done dynamically. When ``other_config:local_ip``
  is updated at OVS side, then such change will be notified to OVSDB plugin via OVSDB
  protocol, which in turn is reflected in Network topology Operational DS. ITM gets
  DTCN for Node update. Parsing Node update notification for ``other_config:local_ip``
  parameter in old and new node can determine change in local_ip for TEP.
  If it is updated, then TEP with old local_ip is deleted from transport zone and
  TEP with new local_ip is added into transport zone. This will fire a DTCN to transport
  zone yang listener and the ITM tunnels get updated.
* TEP updation for transport zone can be done dynamically.
  When ``external_ids:transport-zone`` is updated at OVS side, then such change
  will be notified to OVSDB plugin via OVSDB protocol, which in turn is reflected in
  Network topology Operational DS. ITM gets DTCN for Node update.
  Parsing Node update notification for ``external_ids:transport-zone`` parameter in
  old and new node can determine change in transport zone for TEP.
  If it is updated, then TEP is deleted from old transport zone and added into new
  transport zone. This will fire a DTCN to transport zone yang listener and
  the ITM tunnels get updated.

TEP Deletion
^^^^^^^^^^^^
When an ``openvswitch:other_config:local_ip`` parameter gets deleted through *ovs-vsctl*
command, then network topology Operational DS gets updated via OVSB update notification.
ITM which has registered for the network-topology DTCNs, gets notified and this deletes
the TEP from Transport zone or ``tepsInNotHostedTransportZone`` stored in ITM config/Oper
DS based on ``external_ids:transport-zone`` parameter configured for TEP.

* If ``external_ids:transport-zone`` is configured and corresponding transport zone exists
  in Configuration DS, then remove TEP from transport zone. This will fire a DTCN
  to transport zone yang listener and the ITM tunnels of that TEP get deleted.
* If ``external_ids:transport-zone`` is configured and corresponding transport zone does
  not exist in Configuration DS, then check if TEP exists in ``tepsInNotHostedTransportZone``
  container in Oper DS, if present, then remove TEP from ``tepsInNotHostedTransportZone``.
* If ``external_ids:transport-zone`` is not configured, then check if TEP exists in the
  default transport zone in Configuration DS, if and only if ``def-tz-enabled``
  parameter is configured to true in ``genius-itm-config.xml``. In case, TEP is present,
  then remove TEP from ``default-transport-zone``. This will fire a DTCN to transport
  zone yang listener and ITM tunnels of that TEP get deleted.

Configuration impact
---------------------
Following are the configuation changes and impact in the OpenDaylight.

* ``genius-itm-config.xml`` configuation file is introduced newly into ITM
  in which following parameters are added:

  * ``def-tz-enabled``: this is boolean type parameter which would create or delete
    ``default-transport-zone`` if it is configured true or false respectively. Default
    value is ``false``.
  * ``def-tz-tunnel-type``: this is string type parameter which would allow user to
    configure tunnel-type for ``default-transport-zone``. Default value is ``vxlan``.

.. code-block:: xml
   :caption: genius-itm-config.xml

    <itm-config xmlns="urn:opendaylight:genius:itm:config">
        <def-tz-enabled>false</def-tz-enabled>
        <def-tz-tunnel-type>vxlan</def-tz-tunnel-type>
    </itm-config>

Runtime changes to the parameters of this config file would not be
taken into consideration.

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
This feature would not introduce any significant scale and performance issues
in the OpenDaylight.

Targeted Release
-----------------
OpenDaylight Carbon

Known Limitations
-----------------
* Dummy Subnet prefix ``255.255.255.255/32`` under transport-zone is used to
  store the TEPs listened from southbound.

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
No CLI is added into OpenDaylight for this feature.

OVS CLI
^^^^^^^
ITM TEP parameters can be added/removed to/from the OVS switch using
the ``ovs-vsctl`` command:

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

  ovs-vsctl    set O . other_config:local_ip=192.168.56.102
  ovs-vsctl    set O . external_ids:transport-zone=TZA
  ovs-vsctl    set O . external_ids:br-name=br0
  ovs-vsctl    set O . external_ids:of-tunnel=true

  * To clear TEP params in one go by clearing external_ids and other_config
    column from OVS table:

  ovs-vsctl clear O . external_ids
  ovs-vsctl clear O . other_config

  * To clear specific TEP paramter from external_ids or other_config column
    in OVS table:

  ovs-vsctl remove O . other_config local_ip
  ovs-vsctl remove O . external_ids transport-zone

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
#. Add code to create xml config file for ITM to configure flag which would control
   creation of ``default-transport-zone`` during bootup and configure ``tunnel-type`` for
   default transport zone.
#. Add code to handle changes in the ``def-tz-enabled`` configurable parameter during
   OpenDaylight restart.
#. Add code to handle changes in the ``def-tz-tunnel-type`` configurable parameter during
   OpenDaylight restart.
#. Add code to create listener for OVSDB to receive TEP-specific
   parameters configured at OVS.
#. Add code to update configuation datastore to add/delete TEP received from
   southbound into transport-zone.
#. Check tunnel mesh for transport-zone is updated correctly for TEP
   add/delete into transport-zone.
#. Add code to update configuation datastore for handling update in TEP-IP.
#. Add code to update configuation datastore for handling update in TEP's transport-zone.
#. Check tunnel mesh is updated correctly against TEP update.
#. Add code to create ``tepsInNotHostedTransportZone`` list in operational datastore to
   store TEP received with transport-zone not-configured from northbound.
#. Add code to move TEP from ``tepsInNotHostedTransportZone`` list to transport-zone
   configured from REST.
#. Check tunnel mesh is formed for TEPs after their movement from
   ``tepsInNotHostedTransportZone`` list to transport-zone.
#. Add UTs.
#. Add ITs.
#. Add CSIT.
#. Add Documentation.

Dependencies
============
This feature should be used when configuration flag i.e. ``use-transport-zone`` in
``netvirt-neutronvpn-config.xml`` for automatic tunnel configuration in transport-zone
is disabled in Netvirt's NeutronVpn, otherwise netvirt feature of dynamic tunnel
creation may duplicate tunnel for TEPs in the tunnel mesh.

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

#. Verify ``default-transport-zone`` is not created when ``def-tz-enabled`` flag is false.
#. Verify tunnel-type change is considered while creation of ``default-transport-zone``.
#. Verify ITM tunnel creation on default-transport-zone when TEPs are configured without
   transport zone or with ``default-transport-zone`` on switch when ``def-tz-enabled``
   flag is true.
#. Verify ``default-transport-zone`` is deleted when ``def-tz-enabled flag`` is changed
   from true to false during OpenDaylight controller restart.
#. Verify ITM tunnel creation by TEPs configured with transport zone on switch and
   respective transport zone should be pre-configured on OpenDaylight controller.
#. Verify auto-mapping of TEPs to corresponding transport zone group.
#. Verify ITM tunnel deletion by deleting TEP from switch.
#. Verify TEP transport zone change from OVS will move the TEP to corresponding
   transport-zone in OpenDaylight controller.
#. Verify TEPs movement from ``tepsInNotHostedTransportZone`` to transport-zone when
   transport-zone is configured from northbound.
#. Verify ``local_ip`` dynamic update is possible and corresponding tunnels are also
   updated.
#. Verify ITM tunnel details persist after OpenDaylight controller restart, switch
   restart.

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information for below details:

* TEPs parameters to be configured from OVS side to use this feature.
* TEPs added from southbound can be viewed from REST APIs.
* TEPs added from southbound will be added under dummy subnet (255.255.255.255/32) in
  transport-zone.
* Usage details of genius-itm-config.xml config file for ITM to configure
  ``def-tz-enabled`` flag and ``def-tz-tunnel-type`` to create/delete
  ``default-transport-zone`` and its ``tunnel-type`` respectively.
* User is explicitly required to configure ``def-tz-enabled`` as true if
  TEPs needed to be added into ``default-transport-zone`` from northbound.

Developer Guide will need to capture how to use changes in ITM to create
tunnel automatically for TEPs configured from southbound.

References
==========
* `Genius: Carbon Release Plan <https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan>`_
