==========
OF Tunnels
==========

https://git.opendaylight.org/gerrit/#/q/topic:of-tunnels

OF Tunnels feature adds support for flow based tunnels to allow
scalable overlay tunnels.

Problem description
===================

Today when tunnel interfaces are created, InterFaceManager [IFM] creates
one OVS port for each tunnel interface i.e. source-destination pair. For
N devices in a TransportZone this translates to N*(N-1) tunnel ports
created across all devices and N-1 ports in each device. This has obvious
scale limitations.

Use Cases
---------
This feature will support following use cases:

* Use case 1: Allow user to specify if they want to use flow based tunnels at
  the time of configuration.
* Use case 2: Create single OVS Tunnel Interface if flow based tunnels are
  configured and this is the first tunnel on this device/tep.
* Use case 3: Flow based and non flow based tunnels should be able to exist
  in a given transport zone.
* Use case 4: On tep delete, if this is the last tunnel interface on this
  tep/device and it is flow based tunnel, delete the OVS Tunnel Interface. 

Following use cases will not be supported:

* Configuration of flow based and non-flow based tunnels on the same device.
* Changing tunnel from one flow based to non-flow based at runtime. Such a
  change will require deletion and addition of tep. This is inline with
  existing model where tunnel-type can't be changed at runtime.
* Configuration of Source IP for tunnel through flow. It will still be fixed.

Proposed change
===============
``OVS 2.0.0`` onwards allows configuration of flow based tunnels through
interface ``option:remote_ip=flow``. Currently this field is set to
IP address of the destination endpoint.

``remote_ip=flow`` means tunnel desitnation IP will be set by an OpenFlow
action. This allows us to add different actions for different destinations
using the single OVS/OF port.

This change will add optional parameters to ITM and IFM yang files to allow
OF Tunnels. Based on this option, ITM will configure IFM which in turn will
create tunnel ports in OVSDB.

Using OVSDB Plugin
------------------
OVSDB Plugin provides following field in Interface to configure options:

.. code-block:: json
   
    list options {
        description "Port/Interface related optional input values";
        key "option";
        leaf option {
            description "Option name";
            type string;
        }
        leaf value {
            description "Option value";
            type string;
        }

For flow based tunnels we will set option name ``remote_ip`` to
value ``flow``.

MDSALUtil changes
-----------------
Following new actions will be added to ``mdsalutil/ActionType.java``
* set_tunnel_src_ip
* set_tunnel_dest_ip

Following new matches will be added to ``mdsalutil/NxMatchFieldType.java``
* tun_src_ip
* tun_dest_ip

Pipeline changes
----------------
This change adds a new match in **Table0**. Today we match in ``in_port``
to determine which tunnel interface this pkt came in on. Since currently
each tunnel maps to a source-destination pair it tells us about source device.
For interfaces configured to use flow based tunnels this will add an
additional match for ``tun_src_ip``. So, ``in_port+tunnel_src_ip`` will
give us which tunnel interface this pkt belongs to.

When services call ``getEgressActions(), they will get one additional action,
``set_tunnel_dest_ip`` before the ``output:ofport`` action.

Yang changes
------------
Changes will be needed in ``itm.yang`` and ``odl-interface.yang`` to allow
configuring a tunnel as flow base or not.

itm.yang changes
^^^^^^^^^^^^^^^^
A new parameter ``option-of-tunnel`` will be added to ``list-vteps``

.. code-block:: json
   :emphasize-lines: 12-15

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
            type boolean;
            default false;
        }
    }

This will allow to set OF Tunnels on per VTEP basis. So in a transport-zone
we can have some VTEPs (devices) that use OF Tunnels and others that don't.
Default of false means it will not impact existing behavior and will need to
be explicitly configured. Going forwad we can choose to set default true.

odl-interface.yang changes
^^^^^^^^^^^^^^^^^^^^^^^^^^
We'll add a new ``tunnel-optional-params`` and add them to ``iftunnel``

.. code-block:: json

    grouping tunnel-optional-params {
        leaf tunnel-source-ip-flow {
            type boolean;
            default false;
        }

        leaf tunnel-remote-ip-flow {
            type boolean;
            default false;
        }

        list tunnel-options {
            key "tunnel-option";
            leaf tunnel-option {
                description "Tunnel Option name";
                type string;
            }
            leaf value {
                description "Option value";
                type string;
            }
        }
    }

.. code-block:: json
   :emphasize-lines: 6

    augment "/if:interfaces/if:interface" {
        ext:augment-identifier "if-tunnel";
        when "if:type = 'ianaift:tunnel'";
        ...
        ...
        uses tunnel-optional-params;
        uses monitor-params;
    }

Configuration impact
---------------------
This change doesn't add or modify any configuration parameters.

Clustering considerations
-------------------------
Any clustering requirements are already addressed in ITM and IFM, no new
requirements added as part of this feature.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
N.A.

Scale and Performance Impact
----------------------------
This solution will help improve scale numbers by reducing no. of interfaces
created on devices as well as no. of interfaces and ports present in
``inventory`` and ``network-topology``.

Targeted Release
-----------------
Carbon.

Alternatives
------------
Alternatives considered and why they were not selected.

Usage
=====
How will end user use this feature? Primary focus here is how this feature
will be used in an actual deployment.

For most Genius features users will be other projects but this
should still capture any user visible CLI/API etc. e.g. ITM configuration.

This section will be primary input for Test and Documentation teams.
Along with above this should also capture REST API and CLI.

Features to Install
-------------------
This feature doesn't add any new karaf feature.

REST API
--------
Sample JSONS/URIs. These will be an offshoot of yang changes. Capture
these for User Guide, CSIT, etc.

CLI
---
Any CLI if being added.


Implementation
==============

Assignee(s)
-----------
Primary assignee:
  <Vishal Thapar>

Other contributors:
  <Vacancies available>


Work Items
----------
#. Add relevant match and actions to MDSALUtil
#. Add ``set_tunnel_dest_ip`` action to actions returned in
   ``getEgressActions()`` for OF Tunnels.
#. Add match on ``tun_src_ip`` in **Table0** for OF Tunnels.
#. Add UTs.
#. Add ITs.
#. Add CSIT.
#. Add Documentation

Dependencies
============
This doesn't add any new dependencies. This requires minimum of ``OVS 2.0.0``
which is already lower than required by some of other features.

This should also capture impacts on existing project that depend on Genius.
Following projects currently depend on Genius:

* Netvirt
* SFC

Testing
=======
TBD.

Unit Tests
----------
Appropriate UTs will be added for the new code coming in.

Integration Tests
-----------------
Integration tests will be added once IT framework for ITM and IFM is ready.

CSIT
----
Following test cases will need to be added in Genius CSIT:

#. Create a TZ with more than one TEPs set to use OF Tunnels and test datapath.
#. Create a TZ with mix of OF and non OF Tunnels and test datapath.
#. Delete a TEP using OF Tunnels and add it again with non OF tunnels and test
   the datapath.
#. Delete a TEP using non OF Tunnels and add it again with OF Tunnels and test
   datapath.

Documentation Impact
====================
This will require changes to User Guide and Developer Guide.

User Guide will need to add information on how to add TEPs with flow based
tunnels.

Developer Guide will need to capture how to use changes in IFM to create
individual tunnel interfaces.

References
==========

* TBD: Add link to Genius carbon planning at DDF, if available.

