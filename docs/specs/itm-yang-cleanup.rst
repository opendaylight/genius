
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


2.  The list "transport-zone" in container "transport-zones" will have the following modifications: -

    1. "weight" will be removed.

    2. "option-tunnel-tos" will be a part of the list.

    3. "option-of-tunnel" will be a part of the list.

    4. "monitoring" will be part of the list.

    5. "portname" will be removed.

    6. list "subnets" will be removed along with the leaves "prefix", "gateway-ip" and "vlan-id".

       The earlier list "vteps" and "device-vteps" which were part of the list "subnets"

       will now be part of the parent list "transport-zone".

    7. key for list "vteps" will be only "dpn-id".


.. code-block:: none

       container transport-zones {
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
            leaf tunnel-type {
                type identityref {
                    base odlif:tunnel-type-base;
                }
                mandatory true;
            }
            leaf option-of-tunnel {
                description "Use flow based tunnels for remote-ip";
                type boolean;
                default false;
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
            list vteps {
                key "dpn-id";
                leaf dpn-id {
                     type uint64;
                }
                leaf ip-address {
                     type inet:ip-address;
                }
                leaf option-of-tunnel {
                    description "Use flow based tunnels for remote-ip";
                    type boolean;
                    default false;
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
                container monitoring {
                    uses tunnel-monitor-params
                }
            }
             list device-vteps{
                    key "ip-address";
                    leaf ip-address{
                        type inet:ip-address;
                    }
                    leaf tunnnel-type{
                        type identityref {
                              base odlif:tunnel-type-base;
                        }
                     }
                }
             }
         }
    }

    grouping tunnel-monitoring-params {
        leaf enabled {
            type boolean;
            default true;
        }

        leaf monitor-protocol {
            type identityref {
                base odlif:tunnel-monitoring-type-base;
            }
            default odlif:tunnel-monitoring-type-bfd;
        }
        leaf interval {
            type uint16 {
                range "1000..30000";
            }
        }
    }


3.  container "dc-gateway-ip-list" will be removed from the list "transport-zone"

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
The changes will need changes in the following: -
Suites:-
    Configure_ITM
    ITM Direct Tunnels
    BFD Monitoring
    Service Recovery

Keywords :
    Create Vteps
    Set Json
    SRM start suite

CSIT/Variables/Genius :
Itm_creation_no_vlan.json
l2vlanmember.json


Documentation Impact
====================
2. The change in the JSON format for configuring the transport-zone needs to be documented.

References
==========

N.A.