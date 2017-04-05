
.. contents:: Table of Contents
         :depth: 3

===========================================
Enhanced Id allocation for id-manager pools
===========================================

https://git.opendaylight.org/gerrit/#/q/topic:enhanced-id-allocation

Enhanced Id allocation feature enables the following extensions:

* allocation of a specific id out of the pool
* id allocation for a limited time and re-using expired ids

Problem description
===================

Currently genius id-manager allocates ids from a pool only dynamically by getting an available id,
while requesting a specific id is not possible. In addition, re-allocation of allocated ids can
happen only after their release, because allocations have no expiration-time.

Use Cases
---------

This feature will support the following use cases:

* Use case 1: check the availability of an id in the pool when allocation requests for a specific id
* Use case 2: start re-allocating expired ids if pool has no available or released ids

This feature does not support the following use case:

* Use case 1: allocation of multiple specific ids
* Use case 2: delayed re-use of released ids

Proposed change
===============

The proposed change extends the id-manager pools management. This feature, if enabled on a
created pool, will create and maintain a reversed lookup list of id allocations in addition to the
time of expiration for each allocation. The list maintenance will be id oriented and triggered by
an id allocation or release. Requests for new id allocations will be able to define the allocation
expiration-time and the requested id. Available ids for dynamic allocations (with no requested id)
will still be searched, first among released ids and then from available ids. Only if no id will
be found in the traditional way, the allocated ids list will be scanned to find an expired id that
wasn't released. In that time, the expired id should be updated with the new key and expired-time,
and removed from the id-entries list according to the expired id key. In case of a specific id
allocation, only the allocated ids list will be scanned to determine if the id is available - if
not available, the allocation will change to dynamic and start over again.

In an enhanced pool, the delayed-time-sec in released-ids-holder should be set to 0, in order to 
revoke the possibility that ids that do not exist in the allocated-ids list, are in a state of
released but not yet ready for re-allocation.


Pipeline changes
----------------
No pipeline changes.


RPC Changes
-----------

allocateId RPC currently supports the input parameters - pool-name and id-key.
The following optional input fields will be added:

* Id-value - if set, attempt to allocate a specific id
* expiration-time-sec - if set, allocation will have a time-limit

.. code-block:: json
   :caption: id-manager.yang
   :emphasize-lines: 9-14

    rpc allocateId {
        input {
            leaf pool-name {
                type string;
            }
            leaf id-key {
                type string;
            }
            leaf id-value {
                type uint32;
            }
            leaf expiration-time-sec {
                type uint32;
            }
        }
        output {
            leaf id-value {
                type uint32;
            }
        }
    }

createIdPool RPC input parameters will extend to contain the enhanced-id-allocation enable/ disable
flag.

.. code-block:: json
   :caption: id-manager.yang
   :emphasize-lines: 12-14

    rpc createIdPool {
        input {
            leaf pool-name {
                type string;
            }
            leaf low {
                type uint32;
            }
            leaf high {
                type uint32;
            }
            leaf enhanced-id-allocation {
                type boolean;
            }
        }
    }

Yang changes
------------
id-manager.yang needs to be modified to support the new allocated-ids list

.. code-block:: json
   :caption: id-manager.yang

    container allocated-ids-holder {
        uses allocated-ids;
    }

    grouping allocated-ids {
        list allocated-id-entries {
            key "id";
            leaf id {
                type uint32;
            }
            leaf expired-time-sec {
                type uint32;
            }
            leaf id-key { 
                type string;
            }
        }
    }

Workflow
--------

Pool creation
^^^^^^^^^^^^^
When id-manager pool is created, the enhanced-id-allocation flag will be checked.
If enhanced-id-allocation exists and true, the child/ local pool will be created with an additional 
ids holder - **allocated-ids-holder**. The behavior listed below is relevant only if the 
allocated-ids-holder exists in the child pool.

Dynamic id allocation
^^^^^^^^^^^^^^^^^^^^^
Look for expired-ids in allocated-ids-holder only after no released/ available ids were found

Specific id allocation
^^^^^^^^^^^^^^^^^^^^^^
If allocate-id input holds an id-value - look for it only in allocated-ids-holder:

* Id exists and expired - allocate the requested ID

  + update the entry expired-time
  + compare between the id-key in the request and allocation - if different, remove the former
    id-entry from the parent pool and update the new id-key in the allocated-ids-holder entry

* Id exists and valid - compare between the id-key in the request and allocation:

  + if identical, this is a renew request - update the allocation expired-time
  + if different, try to dynamically allocate a different ID

* Id doesn't exist - allocate the requested ID (assuming that if it was released, it is ready
  for re-use as described earlier). Due to the fact that specific id allocations don't progress
  the pool cursor or clear the id from released-ids in case it was released, allocations of
  released/ available ids should be double checked with the allocated-ids list in order to avoid
  duplicate allocations.

Each id allocation
^^^^^^^^^^^^^^^^^^
* Id came from the released/ available-ids-holder - make sure it doesn't exist in
  allocated-ids-holder (as described above)

* Id is free - allocate and update allocated-ids-holder with the allocated ID and expired-time-sec
  according to expiration-time-sec:

  + if expiration-time-sec > 0, then expired-time-sec = cur-time + value
  + otherwise, expired-time-sec will be set to 0 to signal that this allocation never ages

Id release
^^^^^^^^^^
Remove the id from the allocated-ids-holder as well

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
N.A.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
Searching for an expired id is not effective because it requires going over the allocated ids list 
until finding an expired allocation. The decision wether or not to use this feature, should be 
taken under this consideration and therefor might be less recomended in very large pools. In case 
of a required performance improvement, the allocated ids list can be cached and constantly sorted 
by expiration time.

Targeted Release
-----------------
Nitrogen.

Alternatives
------------
There is no alternative for allocations of specific ids from the pool, besides writing your own 
pool management implementation. For the case of id expiration, there is an option to maintain a 
private list of allocations and schedule a task per allocation that will trigger an id release in 
time of expiration. Implementation should maintain the scheduling after application restart and in 
general can lead to multiple implementations in case of multiple users.


Usage
=====

Features to Install
-------------------
This feature doesn't add any new genius feature.

REST API
--------

Pool with enhanced id allocation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Example of an ehanced-id-allocation pool that is used for DHCP 

**URL:** restconf/config/id-manager:id-pools

**Sample JSON data**

.. code-block:: json
   :emphasize-lines: 47-55

   {
        "pool-name": "dhcpPool.a9904b22-7b5c-3661-9a04-dccf8c20f918.10.1.2.0/24",
        "released-ids-holder": {
          "available-id-count": 0,
          "delayed-time-sec": 0
        },
        "block-size": 19,
        "available-ids-holder": {
          "cursor": 167838228,
          "start": 167838210,
          "end": 167838408
        },
        "child-pools": [
          {
                "child-pool-name": "dhcpPool.a9904b22-7b5c-3661-9a04-dccf8c20f918.10.1.2.0/24.168101180",
                "last-access-time": 1491391312
          }
        ],
        "id-entries": [
          {
                "id-key": "fa:16:3e:92:45:08",
                "id-value": [
                  167838211
                ]
          }
        ]
   },
   {
        "pool-name": "dhcpPool.a9904b22-7b5c-3661-9a04-dccf8c20f918.10.1.2.0/24.168101180",
        "released-ids-holder": {
          "delayed-time-sec": 0,
          "available-id-count": 1,
          "delayed-id-entries": [
                {
                  "ready-time-sec": 1491391342,
                  "id": 167838210
                }
          ]
        },
        "block-size": 19,
        "parent-pool-name": "dhcpPool.a9904b22-7b5c-3661-9a04-dccf8c20f918.10.1.2.0/24",
        "available-ids-holder": {
          "cursor": 167838211,
          "start": 167838210,
          "end": 167838228
        }
        "allocated-ids-holder": {
          "allocated-id-entries": [
                {
                  "id-key": "fa:16:3e:92:45:08",
                  "id": "167838211",
                  "expired-time-sec": "1491477742"
                }
          ]
        }
   }


CLI
---
N.A.


Implementation
==============

Assignee(s)
-----------
Primary assignee:
  Shai Haim (shai.haim@hpe.com)


Work Items
----------

Dependencies
============

Testing
=======
N.A.

Unit Tests
----------

Integration Tests
-----------------

CSIT
----


Documentation Impact
====================
This will require changes to User Guide and Developer Guide.


References
==========


