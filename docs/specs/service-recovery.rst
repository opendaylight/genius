
.. contents:: Table of Contents
      :depth: 3

==========================
Service Recovery Framework
==========================

https://git.opendaylight.org/gerrit/#/q/topic:service-recovery

Service Recovery Framework is a feature that enables recovery of services. This recovery
can be trigerred by user, or eventually, be used as a self-healing mechanism.


Problem description
===================

Status and Diagnostic adds support for reporting current status of different services.
However, there is no means to recover individual service or service instances that have
failed. Only recovery that can be done today is to restart the controller node(s) or
manually restart the bundle or reinstall the karaf feature itself.

Restarting the controller can be overkill and needlessly disruptive. Manually restarting bundle or
feature requires user to be aware of and have access to these CLIs. There may not be one-to-one
mapping from a service to corresponding bundle or feature. Also, a truly secure system
would provide role based access to users. Only someone with administrative rights will have
access to Karaf CLI to restart/reinstall while a less privileged user should be able to trigger
recovery without requiring higher level access.

Note that role based access is out of scope of this document


Use Cases
---------

This feature will support following use cases:

* Use Case 1: Provide RPC and CLI to trigger reinstall of a service.
* Use Case 2: Provide RPC and CLI to trigger recover a service.
* Use Case 3: Provide RPC and CLI to trigger recovery of specific instance object managed by a
  service, referred to as service instance.

Proposed change
===============

A new module Service Recovery Manager (SRM) will be added to Genius. SRM will provide single and
common point of interaction with all individual services. Recovery options will vary from highest
level service restart to restarting individual service instances.

SRM Terminology
---------------

SRM will introduce concept of service entities and operations.

SRM Entities
^^^^^^^^^^^^

* EntityName - Every object in SRM is referred to as an entity and EntityName is the
  unique identifier for a given entity. e.g. ``L3VPN``, ``ITM``, ``VPNInstance`` etc.
* EntityType - Every entity has a corresponding type. Currently supported types are
  ``service`` and ``instance``. e.g. ``L3VPN`` is a entity of type ``service`` and ``VPNInstance``
  is an entity of type ``instance``
* EntityId - Every entity of type ``instance`` will have a unique ``entity-id`` as an
  identifier. e.g. The ``uuid`` of ``VPNInstance`` is the ``entity-id`` identifying an
  individual VPN Instance from amongst many present in ``L3VPN`` service.

SRM Operations
^^^^^^^^^^^^^^

* reinstall - This command will be used to reinstall a service. This will be similar to
  ``karaf`` bundle restart, but may result in restart of more than one bundle as per the
  service. This operation will only be applicable to entity-type ``service``.
* recover - This command will be used to recover an individual entity, which can be ``service``
  or ``instance``. For ``entity-type: service`` the ``entity-name`` will be service name.
  For ``entity-type: instance`` the ``entity-name`` will be instance name and `entity-id`` will
  be a required field.

Example
^^^^^^^

This table gives some examples of different entities and operations for them:

+-----------+------------+------------------+----------------+---------------------------+
| OPERATION | EntityType |    EntityName    |   EntityId     |         Remarks           |
+===========+============+==================+================+===========================+
| reinstall | service    | ITM              | N.A.           | Restart ITM               |
+-----------+------------+------------------+----------------+---------------------------+
| recover   | service    | ITM              | ITM            | Recover ITM Service       |
+-----------+------------+------------------+----------------+---------------------------+
| recover   | instance   | TEP              | dpn-1          | Recover TEP               |
+-----------+------------+------------------+----------------+---------------------------+
| recover   | isntance   | TransportZone    | TZA            | Recover Transport Zone    |
+-----------+------------+------------------+----------------+---------------------------+

Out of Scope
------------

* SRM will not be implementing actual recovery mechanisms, it will only act as intermediary between user and
  individual services.
* SRM will not provide status of services. Status and Diagnostic (SnD) framework is expected to provide
  service status.

Pipeline changes
----------------
N.A.

Yang changes
------------
We'll be adding three new yang files

ServiceRecovery Types
^^^^^^^^^^^^^^^^^^^^^
This file will contain different types used by service recovery framework. Any service that wants
to use ServiceRecovery will have to define its supported names and types in this file.


.. code-block:: none
   :caption: srm-types.yang

    module srm-types {
        namespace "urn:opendaylight:genius:srm:types";
        prefix "srmtypes";

        revision "2017-05-31" {
            description "ODL Services Recovery Manager Types Module";
        }

        /* Entity TYPEs */

        identity entity-type-base {
            description "Base identity for all srm entity types";
        }
        identity entity-type-service {
            description "SRM Entity type service";
            base entity-type-base;
        }
        identity entity-type-instance {
            description "SRM Entity type instance";
            base entity-type-base;
        }


        /* Entity NAMEs */

        /* Entity Type SERVICE names */
        identity entity-name-base {
            description "Base identity for all srm entity names";
        }
        identity genius-ifm {
            description "SRM Entity name for IFM service";
            base entity-type-base;
        }
        identity genius-itm {
            description "SRM Entity name for ITM service";
            base entity-type-base;
        }
        identity netvirt-vpn {
            description "SRM Entity name for VPN service";
            base entity-type-base;
        }
        identity netvirt-elan {
            description "SRM Entity name for elan service";
            base entity-type-base;
        }
        identity ofplugin {
            description "SRM Entity name for openflowplugin service";
            base entity-type-base;
        }


        /* Entity Type INSTANCE Names */

        /* Entity types supported by GENIUS */
        identity genius-itm-tep {
            description "SRM Entity name for ITM's tep instance";
            base entity-type-base;
        }
        identity genius-itm-tz {
            description "SRM Entity name for ITM's transportzone instance";
            base entity-type-base;
        }

        identity genius-ifm-interface {
            description "SRM Entity name for IFM's interface instance";
            base entity-type-base;
        }

        /* Entity types supported by NETVIRT */
        identity netvirt-vpninstance {
            description "SRM Entity name for VPN instance";
            base entity-type-base;
        }

        identity netvirt-elaninstance {
            description "SRM Entity name for ELAN instance";
            base entity-type-base;
        }


        /* Service operations */
        identity service-op-base {
            description "Base identity for all srm operations";
        }
        identity service-op-reinstall {
            description "Reinstall or restart a service";
            base service-op-base;
        }
        identity service-op-recover {
            description "Recover a service or instance";
            base service-op-recover;
        }

    }

ServiceRecovery Operations
^^^^^^^^^^^^^^^^^^^^^^^^^^
This file will contain different operations that individual services must support on entities
exposed by them in `servicesrecovery-types.yang`. These are not user facing operations but
used by SRM to translate user RPC calls to

.. code-block:: none
   :caption: srm-ops.yang

    module srm-ops {
        namespace "urn:opendaylight:genius:srm:ops";
        prefix "srmops";

        import srm-types {
            prefix srmtype;
        }

        revision "2017-05-31" {
            description "ODL Services Recovery Manager Operations Model";
        }

        /* Operations  */

        container service-ops {
            config false;
            list services {
                key service-name
                leaf service-name {
                    type identityref {
                        base srmtype:entity-name-base
                    }
                }
                list operations {
                    key entity-name;
                    leaf entity-name {
                        type identityref {
                            base srmtype:entity-name-base;
                        }
                    }
                    leaf entity-type {
                        type identityref {
                            base srmtype:entity-type-base;
                            mandatory true;
                        }
                    }
                    leaf entity-id {
                        description "Optional when entity-type is service. Actual
                                     id depends on entity-type and entity-name"
                        type string;
                    }
                    leaf trigger-operation {
                        type identityref {
                            base srmtypes:service-op;
                            mandatory true;
                        }
                    }
                }
            }
        }

    }

ServiceRecovery RPCs
^^^^^^^^^^^^^^^^^^^^
This file will contain different RPCs supported by SRM. These RPCs are user facing
and SRM will translate these into ServiceRecovery Operations as defined in `srm-ops.yang`.

.. code-block:: none
   :caption: srm-rpcs.yang

    module srm-rpcs {
        namespace "urn:opendaylight:genius:srm:rpcs";
        prefix "srmrpcs";

        import srm-types {
            prefix srmtype;
        }

        revision "2017-05-31" {
            description "ODL Services Recovery Manager Rpcs Module";
        }

        /* RPCs */

        rpc reinstall {
            description "Reinstall a given service";
            input {
                leaf entity-name {
                    type identityref {
                        base srmtype:entity-name-base;
                        mandatory true;
                    }
                }
                leaf entity-type {
                    description "Currently supported entity-types:
                                    service";
                    type identityref {
                        base srmtype:entity-type-base;
                        mandatory false;
                    }
                }
            }
            output {
                leaf successful {
                    type boolean;
                }
                leaf message {
                    type string;
                }
            }
        }


        rpc recover {
            description "Recover a given service or instance";
            input {
                leaf entity-name {
                    type identityref {
                        base srmtype:entity-name-base;
                        mandatory true;
                    }
                }
                leaf entity-type {
                    description "Currently supported entity-types:
                                    service, instance";
                    type identityref {
                        base srmtype:entity-type-base;
                        mandatory true;
                    }
                }
                leaf entity-id {
                    description "Optional when entity-type is service. Actual
                                 id depends on entity-type and entity-name"
                    type string;
                    mandatory false;
                }
            }
            output {
                leaf response {
                    type identityref {
                        base rpc-result-base;
                        mandatory true;
                    }
                }
                leaf message {
                    type string;
                    mandatory false;
                }
            }
        }

        /* RPC RESULTs */

        identity rpc-result-base {
            description "Base identity for all SRM RPC Results";
        }
        identity rpc-success {
            description "RPC result successful";
            base rpc-result-base;
        }
        identity rpc-fail-op-not-supported {
            description "RPC failed:
                            operation not supported for given parameters";
            base rpc-result-base;
        }
        identity rpc-fail-entity-type {
            description "RPC failed:
                            invalid entity type";
            base rpc-result-base;
        }
        identity rpc-fail-entity-name {
            description "RPC failed:
                            invalid entity name";
            base rpc-result-base;
        }
        identity rpc-fail-entity-id {
            description "RPC failed:
                            invalid entity id";
            base rpc-result-base;
        }
        identity rpc-fail-unknown {
            description "RPC failed:
                            reason not known, check message string for details";
            base rpc-result-base;
        }
    }

Configuration impact
---------------------
N.A.

Clustering considerations
-------------------------
SRM will provide RPCs, which will only be handled on one of the nodes. In turn, it will
write to ``srm-ops.yang`` and each individual service will have Clustered
Listeners to track operations being triggered. Individual services will decide, based
on service and instance on which recovery is triggered, if it needs to run on all nodes
on cluster or individual nodes.

Other Infra considerations
--------------------------
Status and Diagnostics (SnD) may need to be updated to user service names similar to ones
used in SRM.

Security considerations
-----------------------
Providing RPCs to trigger service restarts will eliminate the need to give administrative
access to non-admin users just so they can trigger recovery though bundle restarts from
karaf CLI. Expectation is access to these RPCs will be role based, but role based access
and its implementation is out of scope of this feature.

Scale and Performance Impact
----------------------------
This feature allows recovery at a much fine grained level than full controller or node
restart. Such restarts impact and trigger recovery of services that didn't need to be
recover. Every restart of controller cluster or individual nodes has a significant overhead
that impacts scale and performance. This feature aims to eliminate these overheads by
allowing targeted recovery.

Targeted Release
-----------------

Nitrogen.

Alternatives
------------

Using existing karaf CLI for feature and bundle restart was considered but rejected
due to reasons already captured in earlier sections.

Usage
=====

TBD.

Features to Install
-------------------

odl-genius

REST API
--------

TBD.

CLI
---

srm:reinstall
^^^^^^^^^^^

All arguments are case insensitive unless specified otherwise. 


.. code-block:: none

  DESCRIPTION
    srm:reinstall
    reinstall a given service

  SYNTAX
    srm:reinstall <service-name>

  ARGUMENTS
    service-name
            Name of service. to re-install e.g. itm/ITM, ifm/IFM etc.

  EXAMPLE
    srm:reinstall ifm

srm:recover
^^^^^^^^^^^

.. code-block:: none

  DESCRIPTION
    srm:recover
    recover a service or service instance

  SYNTAX
    srm:recover <entity-type> <entity-name> [<entity-id>]

  ARGUMENTS
    entity-type
            Type of entity as defined in srm-types.
            e.g. service, instance etc.
    entity-name
            Entity name as defined in srm-types.
            e.g. itm, itm-tep etc.
    entity-id
            Entity Id for instances, requierd for entity-type instance.
            e.g. 'TZA', 'tunxyz' etc.

  EXAMPLES
    srm:recover service itm
    srm:recover instance itm-tep TZA
    srm:recover instance vpn-instance e5e2e1ee-31a3-4d0c-a8d8-b86d08cd14b1

Implementation
==============

Assignee(s)
-----------
Primary assignee:
  Vishal Thapar

Other contributors:
  Faseela K
  Hema Gopalakrishnan


Work Items
----------
#. Add srm modules and features
#. Add srm yang models
#. Add code for CLI
#. Add backend implementation for RPCs to tigger SRM Operations
#. Optionally, for each service and supported instances, add implementation for SRM Operations
#. Add UTs
#. Add CSITs

Dependencies
============

* Infrautils

Testing
=======

TBD.

Unit Tests
----------

Integration Tests
-----------------

CSIT
----

Documentation Impact
====================

This will require changes to User Guide based on information provided in Usage section.

References
==========

[1] Genius Nitrogen Release Plan https://wiki.opendaylight.org/view/Genius:Nitrogen_Release_Plan

[2] https://specs.openstack.org/openstack/nova-specs/specs/kilo/template.html

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
