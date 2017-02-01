
.. contents:: Table of Contents
   :depth: 3

=====================
Counter Framework
=====================

https://git.opendaylight.org/gerrit/#/q/topic:counter-framework

Counter framework feature retrieves statistics from JMX and exposes a 8080 rest api
to fetch cumulative statistics on flowcapable switch and bgp.

Problem description
===================
Counter applications registers a counter mbean in the JMX(Java Management Extensions) server and
post its counter statistics individually. A framework application is required to aggregate all
the counters, persist across reboots and provide a bulk API's to retrieve all statistics.

Use Cases
---------

This feature will support following use cases:

* Use case 1: From each counter mbean, the statistics are collected. If the counter value does
  not exist in the datastore then the value is copied to the datastore.
* Use case 2: If statistics are available in datastore, then, for cumulative counters, the
  incremental value noticed over the polling period is added to the value in the datastore.
* Use case 3: On flow capable switch reboot, all the cumulative counter values are persisted
  (Ex: transmit/receive byte counters).
* Use case 4: On flow capable switch port deletion, the port statistics for the switch is removed.
* Use case 5: The data is located in the configuration data store to persist the data across
  reboots.Data is written to the datastore in a batch operation to minimize datastore accesses.
* Use case 6: In addition to data-store persistence, counters are also stored in an in memory
  cache for faster reads.
* Use case 7: To enable counter data retrieval, a 8080 REST API is exposed. The API can return
  the counter data for all counters (bulk retrieve API) with a single API call.
* Use case 8: All API calls will make use of the in-memory cache to return data. Data store will
  not be used.

Following use cases will not be supported:

When a switch is disconnected and never reconnects, the statistics stored for the switch will
remain in datastore.

Proposed change
===============

Counterframework module class implements fcaps api and injects its object instance through
blueprint. Countermanager module gets the injected instance and does method invocation to send
the counter statistics for each counter mbean registered by counter applications periodically.
Framework segregates the statistics and does computation of each counter before storing into
the datastore. To aggregate the statistics, 8080 bulk rest api is exposed which reads data
from in-memory cache.

Pipeline changes
----------------
N.A

Yang changes
------------
yang model ``counter-service.yang`` introduced to persist flowcapable switch and bgp statistics.

.. code-block:: none
   :caption: counter-service.yang

    container performance-counters {
        config true;
        list controllers {
            key hostNodeName;
            leaf hostNodeName { type string; }
            leaf no-of-OF-switch { type uint64; }
        }
        container switch-counters {
            uses switch-info;
        }

        container bgp-counters {
            uses bgp-info;
        }
    }

    grouping bgp-info {
        leaf bgp-total-prefixes { type uint64; }
        list bgp-neighbor-counters {
             key as-id;
             leaf as-id { type uint64; }
             leaf neighbor-ip { type string; }
             leaf bgp-neighbor-packets-received { type uint64; }
             leaf bgp-neighbor-packets-sent { type uint64; }
        }

        list bgp-rd-route-counters {
             key rd;
             leaf rd { type uint64; }
             leaf bgp-rd-route-count { type uint64; }
        }
    }

    grouping switch-info {
        list switch {
            key switch-id;
            leaf switch-id { type uint64; }
            leaf no-of-OF-ports  { type uint64; }
            leaf injected-OF-messages-sent { type uint64; }
            leaf injected-OF-messages-receive { type uint64; }

            list switch-ports-counters {
                key port-id;
                leaf port-id { type uint64; }

                leaf packets-per-OF-port-receive-drop { type uint64; }
                leaf packets-per-OF-port-receive-error { type uint64; }
                leaf OF-port-duration { type uint64; }
                leaf packets-per-OF-port-sent { type uint64; }
                leaf packets-per-OF-port-receive { type uint64; }
                leaf bytes-per-OF-port-sent { type uint64; }
                leaf bytes-per-OF-port-receive { type uint64; }
                leaf packets-per-internal-port-receive { type uint64; }
                leaf packets-per-internal-port-sent { type uint64; }
            }

            list table-counters {
                key table-id;
                leaf table-id { type uint64; }
                leaf entries-per-OF-table { type uint64; }
            }
        }
    }

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
The solution is supported on a 3-node cluster using entity ownership service (EOS). Using EOS, once
the leader is elected, the specific port is activated on the leader. The other nodes will refrain
from opening the port. Subsequently, the north-bound will invoke a REST API call to a virtual IP
that is front-ended by a load balancer (ex: HA-Proxy). The load balancer redirects the requests to
the only node with activated 8080 port. Since the owner of 8080 port is also the leader according to
the EOS, the request will be processed and the corresponding response is generated.

In the event of the failure of the leader node, the EOS election is triggered again and a new
leader is elected.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
The feature takes into account both scale and performance at design time. Given that the number
of DPNs that need to be supported by the controller is very large (could be as large as 200) and
each DPN can host up to 128 ports, there could be around 25600 ports in the network. For each
port, a minimum of 10 counters are being polled, which implies that the total number of counters
to be returned is in the order of 256000. This is a large number for ODL. To ensure that there
is not substantial burden on the datastore or on the network, the following improvements are
being considered.

* The rate of polling the network for counter data is reduced to 15 minutes or higher. Assuming
  that each switch data is retrieved in a single message, this translates to a query by the
  controller once every 4.5 seconds.

* All the data is replicated across the cluster for redundancy purposes. To ensure that the
  counter data is persisted across reboots, the data is stored in configuration data store.
  To reduce datastore access, all the data is pushed into the datastore using batching.
  Furthermore, a in-memory cache is used on all the nodes for faster read operations.
  REST API calls are processed by using the in-memory cache instead of the data-store. Due to
  batching and the use of in-memory cache, the overall data-store access is substantially reduced.

* As a back-of-envelope calculation, consider a polling time of 15 minutes and a default batch
  size of 1000. In this time, we poll for 256,000 counter values and update the corresponding
  data-store values. By using the batch size of 1000, 256 transactions are carried out in 900
  seconds. This indicates a increased in the overall transactions on the MD-SAL data store by
  0.3 transactions/second.


Targeted Release
-----------------
Carbon.
Boron-SR3.

Alternatives
------------
Alternatives considered and why they were not selected.

Usage
=====
Counter Applications are residing in individual feature bundles. To get all statistics,
it has to be ensured those features are installed.

Features to Install
-------------------
This feature doesn't add any new karaf feature, with existing karaf feature
"odl-genius-fcaps-framework" counterframework bundle is been added and needs to be installed.

Following features are

* To get bgp statistics, ensure "odl-netvirt-openstack" is installed.
* To get switch statistics, ensure  "odl-genius" feature is installed.
* To get controller-switch mapping counters, ensure "odl-genius-fcaps-application" is installed.

REST API
--------

flow-capable-switches statistics
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**URL:** http://localhost:8080/controller/statistics/flow-capable-switches

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 43

    {
       "flow_capable_switches" : [ {
          "packet_in_messages_received" : 300,
          "packet_out_messages_sent" : 0,
          "ports" : 0,
          "flow_datapath_id" : 2
       }, {
          "packet_in_messages_received" : 501,
          "packet_out_messages_sent" : 300,
          "ports" : 3,
          "flow_datapath_id" : 1,
          "switch_port_counters" : [{
             "bytes_received" : 9800,
             "bytes_sent" : 6540,
             "duration" : 0,
             "packets_received_on_tunnel" : 0,
             "packets_sent_on_tunnel" : 7650,
             "packets_received" : 0,
             "packets_received_drop" : 0,
             "packets_received_error" : 0,
             "packets_sent" : 0,
             "port_id" : 2
          }, {
             "bytes_received" : 9800,
             "bytes_sent" : 840,
             "duration" : 7800,
             "packets_internal_received" : 984,
             "packets_internal_sent" : 7950,
             "packets_received" : 9900,
             "packets_received_drop" : 1500,
             "packets_received_error" : 1000,
             "packets_sent" : 7890,
             "port_id" : 1
          } ],
          "table_counters" : [ {
             "flow_count" : 90,
             "table_id" : 96
          }, {
             "flow_count" : 80,
             "table_id" : 44
          } ]
       } ]
    }

BGP statistics
^^^^^^^^^^^^^^

**URL:** http://localhost:8080/controller/statistics/bgp

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 23

   {
       "bgp" : {
          "bgp_neighbor_counters" : [ {
             "autonomous_system_number" : 100,
             "neighbor_ip" : "1.1.1.1",
             "packets_received" : 5654,
             "packets_sent" : 987
          }, {
             "autonomous_system_number" : 200,
             "neighbor_ip" : "2.2.2.2",
             "packets_received" : 765,
             "packets_sent" : 678
          } ],
          "bgp_route_counters" : [ {
             "route_distinguisher" : 123,
             "routes" : 98
          }, {
             "route_distinguisher" : 333,
             "routes" : 100
          } ],
          "total_routes" : 198
       }
    }

Controller-switch-mappings statistics
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
**URL:** http://localhost:8080/controller/statistics/controller-switch-mappings

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 9

   {
       "controller_switch_mappings" : [ {
          "connected_flow_capable_switches" : 2,
          "controller_host_name" : "host-3"
       }, {
          "connected_flow_capable_switches" : 1,
          "controller_host_name" : "host-4"
       } ]
    }

CLI
---
N.A.


Implementation
==============

Assignee(s)
-----------
Primary assignee:
  <Viji J>

Other contributors:
  <Vacancies available>


Work Items
----------
#. Blueprint Module creation
#. yang model creation
#. Counter retrieval logic, computation and datastore updation.
#. Batching of write calls into datastore
#. 8080 rest api implementation
#. Cluster cache implementation of statistics

The Trello cards have already been raised for this feature
under the genius_carbon.

https://trello.com/c/BqdgJaId/55-implementation-of-counter-framework-module

Dependencies
============
No dependencies.

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
Appropriate UTs will be added once counter framework module is in place.

Integration Tests
-----------------
Integration tests will be added will be added once counter framework module is in place.

CSIT
----

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information on how OpenDaylight can
be used to retrieve aggregated statistics.

Developer Guide will capture the implementation sketch of how
aggregated statistics is retrieved.

References
==========

* https://wiki.opendaylight.org/view/Genius:Carbon_Release_Plan
* http://docs.opendaylight.org/en/latest/documentation.html
