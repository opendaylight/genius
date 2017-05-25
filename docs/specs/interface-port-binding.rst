.. contents:: Table of Contents
      :depth: 3

=======================================================
Interface model extension for port-binding information
======================================================

[link to gerrit patch]
https://git.opendaylight.org/gerrit/#/q/topic:genius_port_binding_ext

This feature extends Genius Interface configuration YANG model to hold port-binding 
information received from northbound (e.g. neutron)


Problem description
===================

Currently, the ``parent-refs`` augmentation of Genius Interface YANG model (config) holds 
only information such as node-identifier and datapath-node-identifier for a given interface. 
It does not hold additional interface information related to ``vif`` which is generally 
provided by the northbound orchestration systems such as neutron.

Typically the ``vif`` information consists of:
* ``vnic-type``: {normal|direct|macvtap|baremetal}
* ``vif-type``: {unbound|vhostuser|ovs|macvtap|hwweb|hostdev_physical|other}
* ``vif-details``: List of Key-Value pairs

The ``vif`` information of the interface is critical while operating on certain types of 
southbound devices such as netconf based FD.io/VPP, OVS-DPDK...etc. OpenDaylight network 
control services such as NetVirt rely on ``vif`` information of an interface to invoke 
the right southbound rendering modules.


Use Cases
---------

This feature will support following use cases:

* Use case 1: Creation of ``normal`` interfaces on a OVS host (Existing supported use case)
* Use case 2: Creation of ``vhostuser`` interfaces on a DPDK enabled OVS host (Creating VMs in OVS-DPDK environment)
* Use case 3: Creation of ``vhostuser`` interfaces on a VPP host with ``vif-details`` containing ``vhostuser_socket`` field pointing to the socketfile to be connected from the VPP switch (Creating VMs in FD.io/VPP environment)

Proposed change
===============

This feature extends the ``parent-refs`` augmentation of Genius Interface YANG model (config) such that it holds ``vif`` related information associated with an interface.

Pipeline changes
----------------
None

Yang changes
------------
.. code-block:: none
   :caption:interfacemanager/interfacemanager-api/src/main/yang/odl-interface.yang

    augment "/if:interfaces/if:interface" {
        ext:augment-identifier "parent-refs";
        leaf datapath-node-identifier {
            type uint64;
            description "can be a physical switch identifier (optional)";
        }

        leaf parent-interface {
            type string;
            description "can be a physical switch port or virtual switch port e.g. neutron port";
        }

        list node-identifier {
            key "topology-id";
            description "an identifier of the dependent underlying configuration protocol";
            leaf "topology-id" {
                type string;
                description "can be ovsdb configuration protocol or netconf/vpp";
            }
            leaf "node-id" {
                type string;
                description "can be hwvtep configuration protocol";
            }
        }

        /* The following grouping is added as part of this feature */
        uses interface-vif-params;
    }

    /* The following grouping is added as part of this feature */
    grouping interface-vif-params {
        leaf device-owner {
            type identityref {
                base interface-device-type-base;
            }
            description "type of device being attached to this interface. e.g. compute, dhcp";
        }

        leaf vnic-type {
            type identityref {
                base interface-vnic-type-base;
            }
            default interface-vnic-type-normal;
            description "type of vnic being associated to this interface";
        }

        leaf vif-type {
            type identityref {
                base interface-vif-type-base;
            }
            default interface-vif-type-unbound;
            description "type of vif_driver used/to be used to attach a VM to the network";
        }

        list vif-details {
            key "vif-details-key";
            leaf vif-details-key {
                type string;
            }
            leaf vif-details-value {
                type string;
            }
        }
    }


Configuration impact
---------------------
Optional configuration parameters are getting added as part of this feature.

Clustering considerations
-------------------------
The existing clustering mechanisms apply for these additional configuration parameters.

Other Infra considerations
--------------------------
None

Security considerations
-----------------------
None

Scale and Performance Impact
----------------------------
What are the potential scale and performance impacts of this change?
None
Does it help improve scale and performance or make it worse?
N/A

Targeted Release
-----------------
Nitrogen

Alternatives
------------
N/A

Usage
=====
The ``vif`` related information for a given interface can be provided
through RESTCONF exposed by Genius Interface YANG model.

Example:

Interface{
  name=028f042e-4d7d-4c6f-9130-9a99425e1fef,
  type=l2vlan,
  isEnabled=true,
  augmentations={
    ParentRefs{
      device-owner=interface-device-type-dhcp,
      vif-type=interface-vif-type-vhostuser,
      vnic-type=interface-vnic-type-normal,
      node-identifier=[{node-id=overcloud-controller-0.opnfvlf.org, topology-id=topology-netconf}],
      vif-details=[{vif-details-key=port_prefix, vif-details-value=socket_},
                   {vif-details-key=vhostuser_mode, vif-details-value=server},
                   {vif-details-key=support_vhost_user, vif-details-value=True},
                   {vif-details-key=has_datapath_type_netdev, vif-details-value=False},
                   {vif-details-key=vhostuser_socket_dir, vif-details-value=/tmp/},
                   {vif-details-key=vhostuser_socket, vif-details-value=/tmp/socket_028f042e-4d7d-}],
    }
  }
}

Features to Install
-------------------
This feature doesn't add any new karaf feature.Installing any of the below features
can enable the service:

odl-genius-ui
odl-genius-rest
odl-genius

REST API
--------
The ``vif`` related information for a given interface can be provided
through RESTCONF exposed by Genius Interface YANG model. Refer to Usage section

CLI
---
N/A

Implementation
==============

Assignee(s)
-----------
Primary assignee:
  Srikanth Vavilapalli

Work Items
----------

#. Extend YANG model in interfacemanager/interfacemanager-api/src/main/yang/odl-interface.yang

Dependencies
============
Following projects currently depend on Genius:
* Netvirt
* SFC

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
Extend the existing Genius Interface model unit test cases with the following scenarios:

#. Populate ``parent-refs`` field of Interface model with different values of ``vif-type``, ``vnic-type``

Integration Tests
-----------------
None

CSIT
----
None

Documentation Impact
====================
Capture YANG model change and corresponding RESTCONF API parameter changes in User Guide, Developer Guide and in Release Notes.

References
==========

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
