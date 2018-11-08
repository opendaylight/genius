
.. contents:: Table of Contents
   :depth: 3

=======================
ITM Yang Models cleanup
=======================

This Spec review discusses the changes required as part of the clean up
activity in ITM Module.

Problem description
===================

It was discovered during the code review that in ITM yang models, there is some code
which is now redundant because of various reasons, i.e. change in the requirements
and new features coming in making the older code/models redundant. Hence it was felt
that an activity needs to be taken to clean up such yang models so as to enhance
code readability and stability.

Use Cases
---------

Proposed change
===============

YANG changes
------------
Changes will be needed in ``itm.yang`` and ``itm-config.yang``.

ITM YANG changes
^^^^^^^^^^^^^^^^
1.  The below container``vtep-config-schemas`` will be removed from ``itm-config.yang`` as
    this is no longer required.

.. code-block:: none

        container vtep-config-schemas {
          list vtep-config-schema {
            key schema-name;

            leaf schema-name {
                type string;
                mandatory true;
                description "Schema name";
            }

            leaf transport-zone-name {
                type string;
                mandatory true;
                description "Transport zone";
            }

            leaf tunnel-type {
                type identityref {
                base odlif:tunnel-type-base;
                }
            }

            leaf port-name {
                type string;
                mandatory true;
                description "Port name";
            }

            leaf vlan-id {
                type uint16 {
                    range "0..4094";
                }
                mandatory true;
                description "VLAN ID";
            }

            leaf gateway-ip {
                type inet:ip-address;
                description "Gateway IP address";
            }

            leaf subnet {
                type inet:ip-prefix;
                mandatory true;
                description "Subnet Mask in CIDR-notation string, e.g. 10.0.0.0/24";
            }

            leaf exclude-ip-filter {
                type string;
                description "IP Addresses which needs to be excluded from the specified subnet. IP address range or comma separated IP addresses can to be specified. e.g: 10.0.0.1-10.0.0.20,10.0.0.30,10.0.0.35";
            }

            list dpn-ids {
                key "DPN";

                leaf DPN {
                   type uint64;
                   description "DPN ID";
                }
            }
          }
        }


2.  The below list``subnets`` will be removed from the container ``transport-zones``  in ``itm.yang``.
    The updated container will contain the lists ``vteps`` and ``device-teps`` only.

.. code-block:: none

            list subnets {
                key "prefix";
                leaf prefix {
                    type inet:ip-prefix;
                }
                leaf gateway-ip {
                    type inet:ip-address;
                }
                leaf vlan-id {
                    type uint16 {
                        range "0..4094";
                    }
                }
                list vteps {
                    key "dpn-id portname";
                    leaf dpn-id {
                        type uint64;
                    }
                    leaf portname {
                        type string;
                    }
                    leaf ip-address {
                        type inet:ip-address;
                    }
                    leaf option-of-tunnel {
                        description "Use flow based tunnels for remote-ip";
                        type boolean;
                        default false;
                    }
                    leaf weight {
                        type uint16;
                        default 1;
                        description "Bucket weight if tunnel belongs to OF select group";
                    }
                    leaf option-tunnel-tos {
                        description "Value of ToS bits to be set on the encapsulating
                            packet.  The value of 'inherit' will copy the DSCP value
                            from inner IPv4 or IPv6 packets.  When ToS is given as
                            a numberic value, the least significant two bits will
                            be ignored.";
                        type string {
                            length "1..8";
                        }
                    }
                 }
                 list device-vteps {
                     key "node-id ip-address";

                     leaf topology-id {
                         type string;
                     }
                     leaf node-id {
                         type string;
                     }
                     leaf ip-address {
                         type inet:ip-address;
                     }
                 }
             }
         }
    }

3. To improve the yang models readability all the Configuarion Datastore based Yang models will be moved into one file
    ``itm-config.yang`` and the Operational Datastore based Yang models into ``itm-operational.yang``.
    To achieve the same the following changes will be done:-
    3.1 ``itm.yang`` will be removed and its content be merged to ``itm-config.yang``.
    3.2 The follwing yang models will be moved from ``itm-state.yang`` to ``itm-config.yang`` : -
    3.2.1   ``dpn-endpoints``
    3.2.2   ``dpn-teps-state``
    3.2.3   ``external-tunnel-list``
    3.2.4   ``tunnel-list``
    3.3 ``itm-state.yang`` will be renamed as ``itm-operational.yang``.

Workflow
--------
N.A.

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
Any clustering requirements are already addressed in ITM , no new
requirements added as part of this feature.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
This solution will improve the readability and code stability so as to remove
dead/unwarranted code.
Targeted Release(s)
-------------------
Neon

Known Limitations
-----------------
N.A.

Alternatives
------------
N.A.
Usage
=====

Features to Install
-------------------
This feature doesn't add any new karaf feature.

REST API
--------

For the changes listed in 2.,
the REST API to configure a transport-zone will be changed.

Implementation
==============

Assignee(s)
-----------
Primary assignee:
  <Chintan Apte>

Other contributors:
  <Vacancies available>


Work Items
----------
#. YANG changes
#. Code changes
#. Add UTs.
#. Add ITs.
#. Update CSIT.
#. Add Documentation

Dependencies
============
N.A.

Testing
=======

Unit Tests
----------
Appropriate UTs will be added for the new code coming in once framework is in place.
2. UT should cover configuring the tunnels via tep-add commands using the new JSON format (post-cleanup).

Integration Tests
-----------------
Integration tests will be added once IT framework for ITM and IFM is ready.

CSIT
----
2. CSIT should be updated to take care of configuring the transport-zone using the new JSON.

Documentation Impact
====================
2. The change in the JSON format for configuring the transport-zone needs to be documented.

References
==========

N.A.