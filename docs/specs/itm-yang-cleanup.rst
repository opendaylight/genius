
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
The below container``vtep-config-schemas`` will be removed from ``itm-config.yang`` as
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

N.A.


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

Integration Tests
-----------------
Integration tests will be added once IT framework for ITM and IFM is ready.

CSIT
----
N.A.

Documentation Impact
====================
This will not require changes to User Guide and Developer Guide.
References
==========

N.A.