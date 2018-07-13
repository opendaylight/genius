========================
Interface Manager Design
========================

The Interface Manager (IFM) uses MD-SAL based architecture, where
different software components operate on, and interact via a set of
data-models. Interface manager defines configuration data-stores where
other OpenDaylight modules can write interface configurations and
register for services. These configuration data-stores can also be
accessed by external entities through REST interface. IFM listens to
changes in these config data-stores and accordingly programs the
data-plane. Data in Configuration data-stores remains persistent across
controller restarts.

Operational data like network state and other service specific
operational data are stored in operational data-stores. Change in
network state is updated in southbound interfaces (OFplugin, OVSDB)
data-stores. Interface Manager uses ODL Inventory and Topology
datastores to retrive southbound configurations and events. IFM listens
to these updates and accordingly updates its own operational data-stores.
Operational data stores are cleaned up after a controller restart.

Additionally, a set of RPCs to access IFM data-stores
and provide other useful information. Following figure presents
different IFM data-stores and its interaction with other modules.

Follwoing diagram provides a toplevel architecture of Interface Manager.

.. image:: https://wiki.opendaylight.org/images/2/25/Ifmsbirenderers.png
   :scale: 60 %

InterfaceManager Dependencies
=============================
Interface Manager uses other Genius modules for its operations.
It mainly interacts with following other genius modules-

#. **Id Manager** – For allocating dataplane interface-id (if-index)
#. **Aliveness Monitor** - For registering the interfaces for monitoring
#. **MdSalUtil** – For interactions with MD-SAL and other openflow operations

Following picture shows interface manager dependencies

.. graphviz:: images/ifm_dep.dot

Code structure
==============
Interface manager code is organized in following folders -

#. **interfacemanager-api** contains the interface yang data models and
   corresponding interface implementation.
#. **interfacemanager-impl** contains the interfacemanager
   implementation
#. **interface-manager-shell** contains Karaf CLI implementation for
   interfacemanager

**interfacemanager-api**

| ``└───main``
| ``   ├───java``
| ``   │   └───org``
| ``   │       └───opendaylight``
| ``   │           └───genius``
| ``   │               └───interfacemanager``
| ``   │                   ├───exceptions``
| ``   │                   ├───globals``
| ``   │                   └───interfaces``
| ``   └───yang``

**interfacemanager-impl**

| ``├───commons                   <--- contains common utility functions``
| ``├───listeners                 <--- Contains interfacemanager DCN listenenrs for differnt MD-SAL datastores``
| ``├───renderer                  <--- Contains different southbound renderers' implementation``
| ``│   ├───hwvtep                <--- HWVTEP specific renderer``
| ``│   │   ├───confighelpers                     ``
| ``│   │   ├───statehelpers``
| ``│   │   └───utilities``
| ``│   └───ovs                   <--- OVS specific SBI renderer``
| ``│       ├───confighelpers``
| ``│       ├───statehelpers``
| ``│       └───utilities``
| ``├───servicebindings           <--- contains interface service binding DCN listener and corresponding implementation``
| ``│   └───flowbased``
| ``│       ├───confighelpers``
| ``│       ├───listeners``
| ``│       ├───statehelpers``
| ``│       └───utilities``
| ``├───rpcservice                <--- Contains interfacemanager RPCs' implementation``
| ``├───pmcounters                <--- Contains PM counters gathering``\
| ``└───statusanddiag             <--- contains status and diagnostics implementations``

 *'interfacemanager-shell*

Interfacemanager Data-model
===========================

FOllowing picture shows different MD-SAL datastores used by intetrface manager.
These datastores are created based on YANG datamodels defined in interfacemanager-api.

.. image:: https://wiki.opendaylight.org/images/4/46/Ifmarch.png


Config Datastores
-----------------

InterfaceManager mainly uses following two datastores to accept configurations.

#. **odl-interface** datamodel () where verious type of interface can be
   configuted.
#. **service-binding** datamodel () where different applications can
   bind services to interfaces.

In addition to these datamodels, it also implements several RPCs for
accessing interface operational data. Details of these datamodels and
RPCs are described in following sections.

Interface Config DS
~~~~~~~~~~~~~~~~~~~

Interface config datamodel is defined in
`odl-interface.yang <https://github.com/opendaylight/genius/blob/master/interfacemanager/interfacemanager-api/src/main/yang/odl-interface.yang>`__
. It is based on ‘ietf-interfaces’ datamodel (imported in
odl\_interface.yang) with additional augmentations to it. Common
interface configurations are –

-  **name (string)** : this is the unique interface name/identifier.
-  **type (identityref:iana-if-type)** : this configuration sets the
   interface type. Interface types are defined in iana-if-types data
   model. Odl-interfaces.yang data model adds augmentations to
   iana-if-types to define new interface types. Currently supported
   interface types are -

   -  **l2vlan** (trunk, vlan classified sub-ports/trunk-member)
   -  **tunnel** (OVS based VxLAN, GRE, MPLSoverGRE/MPLSoverUDP)

-  **enabled (Boolean)** : this configuration sets the administrative
   state of the interface.
-  **parent-refs :** this configuration specifies the parent of the
   interface, which feeds data/hosts this interface. It can be a
   physical switch port or a virtual switch port.

   -  Parent-interface (string) : is the port name with which a network
      port in dataplane in that appearing on the southbound interface.
      E.g. neutron port. this can also be another interface, thus
      supporting a hierarchy of linked interfaces.
   -  Node-identifier (topology\_id, node\_id) : is used for configuring
      parent node for HW nodes/VTEPs

Additional configuration parameters are defined for specific interface
type. Please see the table below.

+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **Vlan-xparent**                    | **Vlan-trunk**                   | **Vlan-trunk-member**                  | **vxlan**                    | **gre**                    |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **Name** =uuid                      | **Name** =uuid                   | **Name** =uuid                         | **Name** =uuid               | **Name** =uuid             |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| description                         | description                      | description                            | description                  | description                |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **Type** =l2vlan                    | **Type** =l2valn                 | **Type** =l2vlan                       | **Type** =tunnel             | **Type** =tunnel           |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **enabled**                         | **enabled**                      | **enabled**                            | **enabled**                  | **enabled**                |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **Parent-if** = port-name           | **Parent-if** = port-name        | **Parent-if** = vlan-trunkIf           | Vlan-id                      | Vlan-id                    |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
| **vlan-mode** = transparent         | **vlan-mode**  = trunk           | **vlan-mode** = trunk-member           | **tunnel-type** = vxlan      | **tunnel-type** = gre      |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
|                                     | vlan-list= [trunk-member-list]   | **Vlan-Id** =  trunk-vlanId            | dpn-id                       | dpn-id                     |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
|                                     |                                  | **Parent-if** = vlan-trunkIf           | Vlan-id                      | Vlan-id                    |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
|                                     |                                  |                                        | **local-ip**                 | **local-ip**               |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
|                                     |                                  |                                        | **remote-ip**                | **remote-ip**              |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+
|                                     |                                  |                                        | **gayeway-ip**               | **gayeway-ip**             |
+-------------------------------------+----------------------------------+----------------------------------------+------------------------------+----------------------------+

Interface service binding config
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Yang Data Model
`odl-interface-service-bindings.yang <https://github.com/opendaylight/genius/blob/master/interfacemanager/interfacemanager-api/src/main/yang/odl-interface-service-bindings.yang>`__
contains the service binding configuration datamodel.

An application can bind services to a particular interface by
configuring MD-SAL data node at path /config/interface-service-binding.
Binding services on interface allows particular service to pull traffic
arriving on that interface, depending upon the a service priority. It is
possible to bind services at ingress interface (when packet enters into
the packet-pipeline from particular interface) as well as on the egress
Interface (before the packet is sent out on particular interafce).
Service modules can specify openflow-rules to be applied on the packet
belonging to the interface. Usually these rules include sending the
packet to specific service table/pipeline. Service modules/applications
are responsible for sending the packet back (if not consumed) to service
dispatcher table, for next service to process the packet.

.. image:: https://wiki.opendaylight.org/images/5/56/App_co_exist_new.png
   :scale: 70 %

Following are the service binding parameters –

-  **interface-name** is name of the interface to which service binding
   is being configured
-  **Service-Priority** parameter is used to define the order in which
   the packet will be delivered to different services bind to the
   particular interface.
-  Service-Name
-  **Service-Info** parameter is used to configure flow rule to be
   applied to the packets as needed by services/applications.

   -  (for service-type openflow-based)
   -  Flow-priority
   -  Instruction-list

When a service is bind to an interface, Interface Manager programs the
service dispatcher table with a rule to match on the interface
data-plane-id and the service-index (based on priority) and the
instruction-set provided by the service/application. Every time when the
packet leaves the dispatcher table the service-index (in metadata) is
incremented to match the next service rule when the packet is
resubmitted back to dispatcher table. Following table gives an example
of the service dispatcher flows, where one interface is bind to 2
services.

+--------------------------------------------------------------------------------------------------+
| **Service Dispatcher Table**                                                                     |
+--------------------------------+-----------------------------------------------------------------+
| **Match**                      | **Actions**                                                     |
+--------------------------------+-----------------------------------------------------------------+
| -  if-index = I                | -  Set SI=2 in metadata                                         |
| -  ServiceIndex = 1            | -  service specific actions <e.g., Goto prio 1 Service table>   |
+--------------------------------+-----------------------------------------------------------------+
| -  if-index = I                | -  Set SI=3 in metadata                                         |
| -  ServiceIndex = 2            | -  service specific actions <e.g., Goto prio 2 Service table>   |
+--------------------------------+-----------------------------------------------------------------+
| miss                           | Drop                                                            |
+--------------------------------+-----------------------------------------------------------------+

Interface Manager programs openflow rules in the service dispatcher
table.

Egress Service Binding
~~~~~~~~~~~~~~~~~~~~~~

There are services that need packet processing on the
egress, before sending the packet out to particular port/interface. To
accommodate this, interface manager also supports egress
service binding. This is achieved by introducing a new “egress
dispatcher table” at the egress of packet pipeline before the interface
egress groups.

On different application request, Interface Manager returns the egress
actions for interfaces. Service modules program use these actions to
send the packet to particular interface. Generally, these egress actions
include sending packet out to port or appropriate interface egress
group. With the inclusion of the egress dispatcher table the **egress
actions** for the services would be to

-  Update REG6 -  Set service\_index =0 and egress if\_index
-  send the packet to Egress Dispatcher table

IFM shall add a default entry in Egress Dispatcher Table for each interface
With -

-  Match on if\_index with REG6
-  Send packet to corresponding output port or Egress group.

On Egress Service binding, IFM shall add rules to Egress Dispatcher
table with following parameters –

-  Match on

   -  ServiceIndex=egress Service priority
   -  if\_index in REG6 = if\_index for egress interface

-  Actions

   -  Increment service\_index
   -  Actions provided by egress service binding.

Egress Services will be responsible for sending packet back to Egress
Dispatcher table, if the packet is not consumed (dropped/ send out). In
this case the packet will hit the lowest priority default entry and the
packet will be send out.


Operational Datastores
----------------------
Interface Manager uses ODL Inventory and Topology datastores to retrive southbound
configurations and events.


Interface Manager modules
=========================

Interface manager is designed in a modular fashion to provide a flexible
way to support multiple southbound protocols. North-bound
interface/data-model is decoupled from south bound plugins. NBI Data
change listeners select and interact with appropriate SBI renderers. The
modular design also allows addition of new renderers to support new
southbound interfaces, protocols plugins. Following figure shows
interface manager modules –

.. image:: images/ifmsbirenderers.png
   :scale: 60 %

Threading Model and Clustering Considerations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

InterfaceManager uses the datastore-job-coordinator module for all its
operations.

.. toctree::
   :maxdepth: 1

   datastore-job-coordinator

Datastore job coordinator solves the following problems
which is observed in the previous Li-based interface manager :

#. The Business Logic for the Interface configuration/state handling is
   performed in the Actor Thread itself.
#. This will cause the Actor’s mailbox to get filled up and may start
   causing unnecessary back-pressure.
#. Actions that can be executed independently will get unnecessarily
   serialized.
#. Can cause other unrelated applications starve for chance to execute.
#. Available CPU power may not be utilized fully. (for instance, if 1000
   interfaces are created on different ports, all 1000 interfaces
   creation will happen one after the other.)
#. May depend on external applications to distribute the load across the
   actors.


IFM Listeners
-------------

IFM listeners listen to data change events for different MD-SAL data-stores. On the NBI side it
implements data change listeners for interface config data-store and the
service-binding data store. On the SBI side IFM implements listeners for
Topology and Inventory data-stores in opendaylight.

Interface Config change listener
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Interface config change listener listens to ietf-interface/interfaces data node.

service-binding change listener
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Interface config change listener listens to ietf-interface/interfaces data node.

Topology state change listener
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Interface config change listener listens to ietf-interface/interfaces data node.

inventory state change listener
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+++ this page is under construction +++

Dynamic Behavior
================

when a l2vlan interface is configured
-------------------------------------

#. Interface ConfigDS is populated
#. Interface DCN in InterfaceManager does the following :

   - Add interface-state entry for the new interface along with if-index generated
   - Add ingress flow entry
   - If it is a trunk VLAN, need to add the interface-state for all child interfaces, and add ingress flows for all child interfaces

when a tunnel interface is configured
-------------------------------------

#. Interface ConfigDS is populated
#. Interface DCN in InterfaceManager does the following :

   - Creates bridge interface entry in odl-interface-meta Config DS
   - Add port to Bridge using OVSDB
      - retrieves the bridge UUID corresponding to the interface and
      - populates the OVSDB Termination Point Datastore with the following information

|
| ``tpAugmentationBuilder.setName(portName);``
| ``tpAugmentationBuilder.setInterfaceType(type);``
| ``options.put(``\ “``key``”\ ``, ``\ “``flow``”\ ``);``
| ``options.put(``\ “``local_ip``”\ ``, localIp.getIpv4Address().getValue());``
| ``options.put(``\ “``remote_ip``”\ ``, remoteIp.getIpv4Address().getValue());``
| ``tpAugmentationBuilder.setOptions(options);``

 OVSDB plugin acts upon this data change and configures the tunnel end
points on the switch with the supplied information.

NodeConnector comes up on vSwitch
---------------------------------

Inventory DCN Listener in InterfaceManager does the following:
   #. Updates interface-state DS.
   #. Generate if-index for the interface
   #. Update if-index to interface reverse lookup map
   #. If interface maps to a vlan trunk entity, operational states of
      all vlan trunk members are updated
   #. If interface maps to tunnel entity, add ingress tunnel flow

Bridge is created on vSWitch
----------------------------

Topology DCN Listener in InterfaceManager does the following:
   #. Update odl-interface-meta OperDS to have the dpid to bridge
      reference
   #. Retrieve all pre provisioned bridge Interface Entries for this
      dpn, and add ports to bridge using ovsdb

ELAN/VPNManager does a bind service
-----------------------------------

#. Interface service-bindings config DS is populated with service name,
   priority and lport dispatcher flow instruction details
#. Based on the service priority, the higher priority service flow will
   go in dispatcher table with match as if-index
#. Lower priority service will go in the same lport dispatcher table
   with match as if-index and service priority

Interface Manager Sequence Diagrams
===================================

Following gallery contains sequence diagrams for different IFM
operations -

Removal of Tunnel Interface When OF Switch is Connected
-------------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/8/81/Removal_of_Tunnel_Interface_When_OF_Switch_is_Connected.png

Removal of Tunnel Interfaces in Pre provisioning Mode
-----------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/7/7e/Removal_of_Tunnel_Interfaces_in_Pre_provisioning_Mode.png

Updating of Tunnel Interfaces in Pre provisioning Mode
------------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/5/57/Updating_of_Tunnel_Interfaces_in_Pre_provisioning_Mode.png

creation of tunnel-interface when OF switch is connected and PortName already in OperDS
---------------------------------------------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/5/5d/Creation_of_tunnel-interface_when_OF_switch_is_connected_and_PortName_already_in_OperDS.png

creation of vlan interface in pre provisioning mode
---------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/d/d7/Creation_of_vlan_interface_in_pre_provisioning_mode.png

creation of vlan interface when switch is connected
---------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/b/b4/Creation_of_vlan_interface_when_switch_is_connected.png

deletion of vlan interface in pre provisioning mode
---------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/9/96/Deletion_of_vlan_interface_in_pre_provisioning_mode.png

deletion of vlan interface when switch is connect
-------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/9/96/Deletion_of_vlan_interface_when_switch_is_connected.png

Node connector added updated DCN handling
-----------------------------------------

.. image:: https://wiki.opendaylight.org/images/c/ce/Node_connector_added_updated_DCN_handling.png

Node connector removed DCN handling
-----------------------------------

.. image:: https://wiki.opendaylight.org/images/3/36/Node_connector_removed_DCN_handling.png

updation of vlan interface in pre provisioning mode
---------------------------------------------------

.. image:: https://wiki.opendaylight.org/view/File:Updation_of_vlan_interface_in_pre_provisioning_mode.png

updation of vlan interface when switch is connect
-------------------------------------------------

.. image:: https://wiki.opendaylight.org/images/e/e5/Updation_of_vlan_interface_when_switch_is_connected.png
