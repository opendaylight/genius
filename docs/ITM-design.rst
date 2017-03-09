================================
Internal Transport Manager (ITM)
================================

Internal Transport Manager creates and maintains mesh of tunnels of
type VXLAN or GRE between Openflow switches forming an overlay
transport network. ITM also builds external tunnels towards DC
Gateway. ITM does not provide redundant tunnel support.

The diagram below gives a pictorial representation of the different
modules and data stores and their interactions. 

.. image:: https://wiki.opendaylight.org/images/1/12/ITM_top_lvl.png


ITM Dependencies
================
ITM mainly interacts with following other genius modules-

#. **Interface Manager** – For creating tunnel interfaces
#. **Aliveness Monitor** - For monitoring the tunnel interfaces
#. **MdSalUtil** – For openflow operations


Following picture shows interface manager dependencies

.. graphviz:: images/itm_dep.dot

Code Structure
==============
As shown in the diagram, ITM has a common placeholder for various
datastore listeners, RPC implementation, config helpers. Config
helpers are responsible for creating / delete of Internal and
external tunnel.

.. image:: https://wiki.opendaylight.org/images/c/c3/Itmcodestructure.png

ITM Data Model
==============

ITM uses the following data model to create and manage tunnel interfaces 
Tunnels interfces are created by writing to Interface Manager’s Config DS.

itm.yang 
---------
follwoing datamodel is defined in `itm.yang <https://github.com/opendaylight/genius/blob/master/itm/itm-api/src/main/yang/itm.yang>`__
This DS stores the transport zone information populated through REST or Karaf CLI

|image33|

Itm-state.yang
--------------

This DS stores the tunnel end point information populated through
REST or Karaf CLI. The internal and external tunnel interfaces are
also stored here.

\ |image34|

Itm-rpc.yang
------------

This Yang defines all the RPCs provided by ITM.

|image35|

Itm-config.yang
~~~~~~~~~~~~~~~

|image36|

ITM Design
==========

ITM uses the datastore job coordinator module for all its operations.

.. toctree::
   :maxdepth: 1
   
   datastore-job-coordinator

When tunnel end point are configured in ITM datastores by CLI or 
REST, corresponding DTCNs are fired. ITM TransportZoneListener
listens to the . Based on the add/remove end point operation, 
the transport zone listener queues the approporiate job ( ItmInternalTunnelAddWorker or
ItmInternalTunnelDeleteWorker) to the DataStoreJob Coordinator. Jobs
within transport zones are queued to be executed serially and jobs
across transport zones are done parallel.

Tunnel Building Logic
---------------------

ITM will iterate over all the tunnel end points in each of the transport
zones and build the tunnels between every pair of tunnel end points in
the given transport zone. The type of the tunnel (GRE/VXLAN) will be
indicated in the YANG model as part of the transport zone.


ITM Operations
--------------

    ITM builds the tunnel infrastructure and maintains them. ITM builds
    two types of tunnels namely, internal tunnels between openflow
    switches and external tunnels between openflow switches and an
    external device such as datacenter gateway. These tunnels can be
    Vxlan or GRE. The tunnel endpoints are configured using either
    individual endpoint configuration or scheme based auto configuration
    method or REST. ITM will iterate over all the tunnel end points in
    each of the transport zones and build the tunnels between every pair
    of tunnel end points in the given transport zone.

-  ITM creates tunnel interfaces in Interface manager Config DS.

-  Stores the tunnel mesh information in tunnel end point format in ITM
   config DS

-  ITM stores the internal and external trunk interface names in
   itm-state yang

-  Creates external tunnels to DC Gateway when VPN manager calls the
   RPCs for creating tunnels towards DC gateway.

   ITM depends on interface manager for the following functionality.

-  Provides interface to create tunnel interfaces

-  Provides configuration option to enable monitoring on tunnel
   interfaces.

-  Registers tunnel interfaces with monitoring enabled with
   alivenessmonitor.

   ITM depends on Aliveness monitor for the following functionality.

-  Tunnel states for trunk interfaces are updated by alivenessmonitor.
   Sets OperState for tunnel interfaces

RPCs
----

The following are the RPCs supported by ITM

Get-tunnel-interface-id RPC
~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image37|

Get-internal-or-external-interface-name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image38|

Get-external-tunnel-interface-name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image39|

Build-external-tunnel-from-dpns
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image40|

Add-external-tunnel-endpoint
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image41|

Remove-external-tunnel-from-dpns
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image42|

Remove-external-tunnel-endpoint
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image43|

Create-terminating-service-actions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image44|

Remove-terminating-service-actions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|image45|
