.. contents:: Table of Contents
      :depth: 3

======================
ITM Scale Enhancements
======================

Gerrit Topic Branch Name :- ``ITM-Scale-Improvements``

ITM creates tunnel mesh among switches with the help of interfacemanager.
This spec describes re-designing of ITM to create the tunnel mesh independently without interface manager.
This is expected to improve ITM performance and therefore support a larger tunnel mesh.


Problem description
===================
ITM creates tunnels among the switches. When ITM receives the configuration from NBI, it creates interfaces on ietf
interface config DS, which the interface manager listens to and creates the tunnel constructs on the switches.
This involves an additional hop from ITM to interface manager which constitutes many DCNs and DS reads and writes.
This induces a lot of load on the system, especially in a scale setup. Also, tunnel interfaces are catagorized as
generic ietf-interface along with tap and vlan interfaces. Interface manager deals with all these interfaces.
Applications listening for interface state gets updates on tunnel interfaces both from interface manager and ITM.
This degrades the performance and hence the internal tunnel mesh creation does not scale up very well beyond
80 switches.

Use Cases
---------

This feature will support the following use cases.

* Use case 1: ITM will create a full tunnel mesh when it receives the configuration from NBI. Tunnel ports or
  OF based tunnels  will be created on the switches directly by ITM, by-passing interface manager.
* Use case 2: ITM will support a config parameter which can be used to select the old or the new way of tunnel mesh
  creation. Changing this flag dynamically after configuration will not be supported. If one needs to switch the
  implementation, then the controller needs to be restarted.
* Use case 3: ITM will detect the tunnel state changes and publish it to the applications.
* Use case 4: ITM will provide the tunnel egress actions irrespective of the presence of tunnel in the dataplane.
* Use case 5: ITM will support a config parameter to enable/disable monitoring on a per tunnel basis
* Use case 6: ITM will support BFD event dampening during initial BFD config.
* Use Case 7: ITM will support a config parameter to turn ON/OFF the alarm generation based on tunnel status.
* Use case 8: ITM will support traffic switch over in dataplane based on the tunnel state.
* Use case 9: ITM will cache appropriate MDSAL data to further improve the performance.

Proposed change
===============
In order to improve the scale numbers, handling of tunnel interface is separated from other interfaces. Hence,
ITM module is being re-architectured to by-pass interface manager and create/delete the tunnels between the switches
directly. ITM will also provide the tunnel status without the support of interface manager.

By-passing interface manager provides the following advantage
* removes the creation of ietf interfaces in config DS
* reduces a number of DCN being generated
* reduces the number of datastore reads and writes.
* Applications to get tunnel updates only from ITM

All this should improves the performance and thereby the scale numbers.

Further improvements that can be done is to

* Decouple DPN Id requirement for tunnel port creation. Node id information will suffice the tunnel port creation as
  DPN Id is not required. This will make the tunnel creation simplier and will remove any timing issues as the
  mapping between DPN Id and Node id for tunnel creation is eliminated. This also decouples the OF channel
  establishment and tunnel port creation. Further ITM's auto tunnel creation, which learns
  the network topology details when OVS connects can be leveraged for tunnel creation.

Assumption
==========
This feature will not be used along with per-tunnel specific service binding use case as both use cases together
are not supported. Multiple Vxlan Tunnel feature will not work with this feature as it needs service binding on tunnels.

Implementation
==============
Most of the code for this proposed changes will be in separate package for code maintainability.
There will be minimal changes in some common code in ITM and interface manager to switch between the old and the new way
of tunnel creation

* If ``itm-direct-tunnels`` flag is ON, then
  -- itm:transport-zones listener will trigger the new code upon receiving transport zone configuration.
  -- interface manager will ignore events pertaining to OVSDB tunnel port and tunnel interface related inventory
  changes.
  -- When ITM gets the NBI tep configuration
  o ITM wires the tunnels by forming tunnel interface name and stores the Tep information in dpn ITM does not
  create the tunnel interfaces in the ietf-interface config DS. Stores the tunnel name in the ``dpn-teps-state``
  in ``itm-state.yang``.

  o ITM generates a unique number from ID Manager for each tep and this will be programmed as ``group id``
    in all other CSSs in order to reach this tep.
  o This unique number will also serve as ``if-index`` for each tep interface. This will be stored
    in ``if-indexes-interface-map`` in ``odl-itm-meta.yang``
  o Install the group on the switch. ITM will write the group in the openflow plugin inventory config DS,
    irrespective of the switch being connected.
  o Add ports to the Bridge through OVSDB, if the switch is connected. ITM will be using the bridge related
    information from the ``odl-itm-meta.yang``.
  -- Implement listeners to Topology Operational DS for ``OvsdbBridgeAugmentation``. When switch gets
     connected add ports to the bridge (in the pre-configured case)
  -- Implement listeners to Inventory Operational DS for ``FlowCapableNodeConnector``.
     -- On OfPort addition,
       -- push the table 0 flow entries
       -- populate the ``tunnels_state`` in ``itm-state.yang`` tunnel state that comes in OF Port status.
       -- update the group with watch-port for handling traffic switchover in dataplane.

  -- If this feature is not enabled, then ITM will take the usual route of configuring ietf-interfaces.

* If the ``alarm-generation-enabled`` is enabled, then register for changes in ``tunnels_state`` to generate the alarms.
* ITM will support individual tunnels to be monitored.
* If Global monitoring flag is enabled, then all tunnels will be monitored.
* If Global flag is turned OFF, then individual per tunnel monitoring flag will take effect.
* ITM will support dynamic enable/disable of bfd global flag / individual flag.
* BFD dampening logic for bfd states is as follows,
  -- On tunnel creation, ITM will consider initial tunnel status to be UP and LIVE and mark it as in ‘dampening’ state
  -- If it receives UP and LIVE event, the tunnel will come out of dampening state, no change/event will be is triggered
  -- If it does not receive UP and LIVE, for a configured duration, it will set the tunnel state to DOWN
  -- There be a configuration parameter for the above - ``bfd-dampening-timeout``.
* External Tunnel (HWVTEP and DC Gateway) Handling will take same existing path, that is through interfacemanager.
* OF Tunnel (flow based tunnelling) implementation will also be done directly by ITM following the same approach.

Pipeline changes
----------------
Pipeline will change as the egress action will be pointing to a group instead of output on port

* ITM will install Tunnel Ingress Table Table 0. Match on in_port and goto INTERNAL_TUNNEL_TABLE or L3_LFIB_TABLE.
  Metadata will contain LPort tag
  cookie=0x8000001, duration=6627.550s, table=0, n_packets=1115992, n_bytes=72424591, priority=5,in_port=6
  actions=write_metadata:0x199f0000000001/0x1fffff0000000001,goto_table:36
  cookie=0x8000001, duration=6627.545s, table=0, n_packets=280701, n_bytes=19148626, priority=5,in_port=7
  actions=write_metadata:0x19e90000000001/0x1fffff0000000001,goto_table:20
* ITM will create group-id for each (destination) DPN and install the group on all other DPNs to reach this destination
  DPN.
* ITM will update the group with watch-port as the tunnel openflow port.
  group_id=800000,type=ff, bucket=weight:100,watch_port=5,actions=output:5
* ITM will program Table 220 with match on Lport Tag and output: [group id]
* ITM will provide the RPC get-Egress-Action-For-Interface with the following actions,
  -- set Tunnel Id
  -- Load Reg6 (with IfIndex)
  -- Resubmit to Table 220


Yang changes
------------
A new container ``dpn-teps-state`` will be added. This will be a config DS

.. code-block:: none
   :caption: itm-state.yang
       :emphasize-lines: 145-180
    container dpn-teps-state {

                        list dpns-teps {

                          key "source-dpn-id";

                          leaf source-dpn-id {
                                   type uint64;
                          }
                          leaf tunnel-type {
                              type identityref {
                                 base odlif:tunnel-type-base;
                              }
                          }
                          leaf group-id {
                              type uint32;
                          }

                          /* Remote DPNs to which this DPN-Tep has a tunnel */
                          list remote-dpns {

                               key "destination-dpn-id";

                                 leaf destination-dpn-id {
                                    type uint64;
                                 }
                                 leaf tunnel-name {
                                     type string;
                                 }
                                 leaf monitor-enabled { // Will be enhanced to support monitor id.
                                      type boolean;
                                      default true;
                                 }
                             }
                         }
    }

    A new Yang ''odl-itm-meta.yang'' will be create to store OVS bridge related information.

.. code-block:: none
   :caption: odl-itm-meta.yang
           :emphasize-lines: 188-238

    container bridge-tunnel-info {
            description "Contains the list of dpns along with the tunnel interfaces configured on them.";

            list ovs-bridge-entry {
                key dpid;
                leaf dpid {
                    type uint64;
                }

                leaf ovs-bridge-reference {
                    type southbound:ovsdb-bridge-ref;
                    description "This is the reference to an ovs bridge";
                }
                list ovs-bridge-tunnel-entry {
                    key tunnel-name;
                    leaf tunnel-name {
                        type string;
                    }
                }
            }
        }

        container ovs-bridge-ref-info {
            config false;
            description "The container that maps dpid with ovs bridge ref in the operational DS.";

            list ovs-bridge-ref-entry {
                key dpid;
                leaf dpid {
                    type uint64;
                }

                leaf ovs-bridge-reference {
                    type southbound:ovsdb-bridge-ref;
                    description "This is the reference to an ovs bridge";
                }
            }
        }

        container if-indexes-tunnel-map {
               config false;
               list if-index-tunnel {
                   key if-index;
                   leaf if-index {
                       type int32;
                   }
                   leaf interface-name {
                       type string;
                   }
               }
       }

        New config parameters to be added to ``interfacemanager-config``

.. code-block:: none
   :caption: interfacemanager-config.yang
                 :emphasize-lines: 245-250
                 leaf itm-direct-tunnels {
                      description "Enable ITM to handle tunnels directly by-passing
                                 interface manager to scale up ITM tunnel mesh.";
                      type boolean;
                      default false;
                 }

    New config parameters to be added to ``itm-config``

.. code-block:: none
   :caption: itm-config.yang
           :emphasize-lines: 257-269
           leaf alarm-generation-enabled {
                description "Enable the ITM to generate alarms based on
                           tunnel state.";
                type boolean;
                default true;
           }
           leaf bfd-dampening-timeout {
                description "CSC will wait for this timeout period to receive the BFD - UP and LIVE event
                       from the switch. If not received within this time period, CSC will mark the tunnel as DOWN.
                       This value is in seconds";
                type uint16;
                default 30;
           }

The RPC call ``itm-rpc:get-egress-action`` will return the group Id which will point to tunnel port (when the tunnel
port  is created on the switch) between the source and destination dpn id.

.. code-block:: json
   :caption: itm-rpc.yang
   :emphasize-lines: 278-300

        rpc get-egress-action {
            input {
                 leaf source-dpid {
                      type uint64;
                 }

                 leaf destination-dpid {
                      type uint64;
                 }

                 leaf tunnel-type {
                     type identityref {
                          base odlif:tunnel-type-base;
                     }
                 }
           }

           output {
                leaf group-id {
                     type uint32;
                }
           }
        }

   ITM will also support another RPC ``get-tunnel-type``

.. code-block:: json
   :caption: itm-rpc.yang
       :emphasize-lines: 308-322

    rpc get-tunnel-type {
             description "to get the type of the tunnel interface(vxlan, vxlan-gpe, gre, etc.)";
                 input {
                     leaf intf-name {
                         type string;
                     }
                 }
                 output {
                     leaf tunnel-type {
                         type identityref {
                             base odlif:tunnel-type-base;
                         }
                 }
             }
    }

For the two above RPCs, when this feature is enabled ITM will service the two RPCs for internal tunnels and for the
external tunnels, ITM will forward it to interfacemanager. When this feature is disabled, ITM will forward the RPCs
for both internal and external to interfacemanager. Applications should now start using the above two RPCs from
ITM and not interfacemanager.

   ITM will enhance the existing RPCs ``create-terminating-service-actions`` and ``remove-terminating-service-actions``.

    New RPC will be supported by ITM to enable monitoring of individual tunnels - internal or external.

.. code-block:: json
   :caption: itm-rpc.yang
       :emphasize-lines: 337-350

    rpc set-bfd-enable-on-tunnel {
           description "used for turning ON/OFF to monitor individual tunnels";
           input {
               leaf source-node {
                  type string;
               }
               leaf destination-node {
                  type string;
               }
               leaf monitoring-params {
                  type itmcfg:tunnel-monitor-params;
               }
           }
    }

Configuration impact
--------------------
Following are the configuration changes and impact in the OpenDaylight.

* Following parameter is added to the ``genius-interfacemanager-config.xml``:

* ``itm-direct-tunnels``: this is boolean type parameter which enables or disables the new ITM realization
  of the tunnel mesh. Default value is ``false``.

* Following parameters are added to the ``genius-itm-config.xml``:

  * ``alarm-generation-enabled``: this is boolean type parameter which enables or disables the new generation of alarms
    by ITM. Default value is ``true``.
  * ``bfd-dampening-timeout``: timeout in seconds. Config parameter which the dampening logic will use

.. code-block:: xml
   :caption: genius-interfacemanager-config.xml
       :emphasize-lines: 371-373

              <interfacemanager-config xmlns="urn:opendaylight:genius:itm:config">
                  <itm-direct-tunnels>false</itm-direct-tunnels>
              </interfacemanager-config>

.. code-block:: xml
   :caption: genius-itm-config.xml
       :emphasize-lines: 379-382

        <itm-config xmlns="urn:opendaylight:genius:itm:config">
            <alarm-generation-enabled>true</alarm-generation-enabled>
            <bfd-dampening-timeout>30</bfd-dampening-timeout> -- value in seconds. Whats the ideal default value ?
        </itm-config>

    Runtime changes to the parameters of this config file will not be taken into consideration.

Clustering considerations
-------------------------
The solution is supported on a 3-node cluster.

Upgrade Support
---------------
Upgrading ODL versions from the previous ITM tunnel mesh creation logic to this new tunnel mesh creation logic will
be supported. When the ``itm-direct-tunnels`` flag changes from ``disable`` from previous version to ``enable`` in this
version, ITM will automatically mesh tunnels in the new way and clean up any data that was persisted in the previous
tunnel creation method.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
This solution will improve scale numbers by reducing no. of interfaces
created in ``ietf-interfaces`` and this will cut down on the additional processing done by interface manager.
This feature will provide fine granularity in bfd monitoring per tunnels. This should considerably reduce the
number bfd events generated for all the tunnels, instead monitoring only those tunnels that are required.
Overall this should improve the ITM performance and scale numbers.

Targeted Release
----------------
Oxygen

Alternatives
------------
N.A

Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature.Installing any of the below features
can enable the service:

odl-genius-rest
odl-genius

REST API
--------

Enable this feature
^^^^^^^^^^^^^^^^^^^
Before starting the controller, enable this feature in genius-interfacemanager-config.xml, by editing it as follows:-

.. code-block:: xml
   :caption: genius-itm-config.xml
       :emphasize-lines: 443-445

        <interfacemanager-config xmlns="urn:opendaylight:genius:interface:config">
            <itm-direct-tunnels>true</itm-direct-tunnels>
        </interfacemanager-config>

Creation of transport zone
^^^^^^^^^^^^^^^^^^^^^^^^^^

Post the ITM transport zone configuration from the REST.

**URL:** restconf/config/itm:transport-zones/

**Sample JSON data**

.. code-block:: json
       :emphasize-lines: 459-485

       {
        "transport-zone": [
            {
                "zone-name": "TZA",
                "subnets": [
                    {
                        "prefix": "192.168.56.0/24",
                        "vlan-id": 0,
                        "vteps": [
                            {
                                "dpn-id": "1",
                                "portname": "eth2",
                                "ip-address": "192.168.56.101",
                            },
                            {
                                "dpn-id": "2",
                                "portname": "eth2",
                                "ip-address": "192.168.56.102",
                            }
                        ],
                        "gateway-ip": "0.0.0.0"
                    }
                ],
                "tunnel-type": "odl-interface:tunnel-type-vxlan"
            }
        ]
       }

ITM RPCs
^^^^^^^^

**URL:** restconf/operations/itm-rpc:get-egress-action

..code-block:: json
       :emphasize-lines: 495-501

 {
    "input": {
        "source-dpid": "40146672641571",
        "destination-dpid": "102093507130250",
        "tunnel-type": "odl-interface:tunnel-type-vxlan"
        }
        }


CLI
---
This feature will not add any new CLI for configuration. Some debug CLIs to dump the cache information
may be added for debugging purpose.


Assignee(s)
-----------

Primary assignee:
  <Hema Gopalakrishnan>

Work Items
----------
Trello card:

* Add support for the configuration parameter ``itm-direct-tunnels``.
* Implement listeners for Topology Operational DS for ``OvsdbBridgeAugmentation``.
* Implement listeners to Inventory Operational DS for ``FlowCapableNodeConnector``.
* Implement support for creation / deletion of tunnel ports
* Implement support for installing / removal of Ingress flows
* Implement API/caches to access ``bridge-interface-info``, ``bridge-ref-info`` from ``odl-itm-meta.yang``.
* Add support for the config parameter ``alarm-generation-enabled``.
* Implement the dampening logic for bfd states.
* Add support to populate the new data store ``dpn-teps-state`` in ``itm-state.yang``.
* Add support for getting group id from ID Manager for each DPN and install it on all other switches.
* Add support to update the group with the tunnel port when OfPort add DCN is received.
* Add support for RPC - getEgressAction, getTunnelType, set-bfd-enable-on-tunnel.
* Enhance the existing createTerminationServiceActions and removeTerminatingServiceActions
* Add caches whereever required - this includes adding data to cache, cleaning them up, CLIs to dump the cache.
* Add support for upgrade from previous tunnel creation way to this new way of tunnel creation.

The following work items will be taken up later

* Add support for OF Tunnel based implementation.
* Removal of dependency on DPN Id for Tunnel mesh creation.


Dependencies
============
This requires minimum of ``OVS 2.8`` where the BFD state can be received in of-port events.

The dependent applications in netvirt and SFC will have to use the ITM RPC to
get the Egress actions. ITM will respond with egress actions for internal tunnels and for
external tunnels ITM will forward the RPC to to interface manager, fetch the output and forward it to the applications.

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in for this feature. This includes but not limited to :-

1. Add ITM configuration enabling this new feature, configure two TEPs and check if the tunnels are created. Check
   ietf interfaces to verify that interface manager is bypassed, check if groups are created on the switch.
2. Delete the TEPs and verify if the tunnels are deleted appropriately.
3. Toggle the ``alarm-generation-enabled`` and check if the alarm were generated / supressed based on the flag.
4. Enable monitoring on a specific tunnel and make the tunnel down on the dataplane and verify if the tunnel status
   is reflected correctly on the controller.


Integration Tests
-----------------
1. Configure ITM to build a larger tunnel mesh and check
   * if the tunnels are created correctly,
   * the tunnels are UP
   * the time taken to create the tunnel mesh
   * the tunnels come back up correctly after controller restart.
2. Increase the number of configured DPNs and find out the maximum configurable DPNs for which the tunnel mesh
   works properly.

CSIT
----
The following test cases will be added to genius CSIT.

1. Add ITM configuration enabling this new feature, configure two TEPs and check if the tunnels are created. Check
   ietf interfaces to verify that interface manager is bypassed,,check if groups are created on the switch.
2. Delete the TEPs and verify if the tunnels are deleted appropriately.
3. Toggle the ``alarm-generation-enabled`` and check if the alarm were generated / supressed based on the flag.
4. Enable monitoring on a specfic tunnel and make the tunnel down on the dataplane and verify if the tunnel status
   is reflected correctly on the controller.

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information for below details:
For the scale setup, this feature needs to be enabled so as to support tunnel mesh among scaled number of DPNs.

* Usage details of genius-interfacemanager-config.xml config file for ITM to enable this feature by configuring
  ``itm-direct-tunnels``  flag to true.

Developer Guide will need to capture how to use the ITM RPC -

* get-egress-action
* get-tunnel-type
* create-terminating-service-actions
* remove-terminating-service-actions
* set-bfd-enable-on-tunnel

References
==========

[1] Genius Oxygen Release Plan

[2] Genius Trello Card

[3] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__
