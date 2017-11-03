
.. contents:: Table of Contents
      :depth: 3

=================
Interface Manager
=================
Test Suite for testing basic Interface Manager functions.

Test Setup
==========
Test setup consists of ODL with `odl-genius` installed and two switches (DPNs) connected
to ODL over OVSDB and OpenflowPlugin.

Testbed Topologies
------------------
This suit uses the default Genius topology.

Default Topology
^^^^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt


Hardware Requirements
---------------------
N.A.

Software Requirements
---------------------
OVS 2.6+
Mininet ???

Test Suite Requirements
=======================

Test Suite Bringup
------------------
Following steps are followed at beginning of test suite:

* Bring up ODL with `odl-genius` feature installed
* Add bridge to DPN
* Add `tap` interfaces to bridge created above
* Add OVSDB manager to DPN using `ovs-vsctl set-manager`
* Connect bridge to OpenFlow using `ovs-vsctl set-controller`
* Repeat above steps for other DPNs
* Create REST session to ODL


Test Suite Cleanup
------------------
Following steps are followed at beginning of test suite:

* Delete bridge DPN
* Delete OVSDB manager 'ovs-vsctl del-manager'
* Repeat above steps for other DPNs
* Delete REST session to ODL

Debugging
---------
Following DataStore models are captured at end of each test case:

* config/itm-config:tunnel-monitor-enabled
* config/itm-config:tunnel-monitor-interval
* config/itm-state:dpn-endpoints
* config/itm-state:external-tunnel-list
* config/itm:transport-zones
* config/network-topology:network-topology
* config/opendaylight-inventory:nodes
* operational/ietf-interfaces:interfaces
* operational/ietf-interfaces:interfaces-state
* operational/itm-config:tunnel-monitor-enabled
* operational/itm-config:tunnel-monitor-interval
* operational/itm-state:tunnels_state
* operational/network-topology:network-topology
* operational/odl-interface-meta:bridge-ref-info


Test Suite Code
---------------

.. code-block:: none
   :caption: 01_Interface_manager.robot

    *** Settings ***
    Documentation     Test Suite for Interface manager
    Suite Setup       Create Session    session    http://${ODL_SYSTEM_IP}:${RESTCONFPORT}    auth=${AUTH}    headers=${HEADERS}
    Suite Teardown    Delete All Sessions
    Test Teardown     Get Model Dump    ${ODL_SYSTEM_IP}    ${bfd_data_models}
    Library           OperatingSystem
    Library           String
    Library           RequestsLibrary
    Library           Collections
    Library           re
    Variables         ../../variables/Variables.py
    Variables         ../../variables/genius/Modules.py
    Resource          ../../libraries/DataModels.robot
    Resource          ../../libraries/Utils.robot

    *** Variables ***
    ${genius_config_dir}    ${CURDIR}/../../variables/genius
    ${bridgename}     BR1
    ${interface_name}    l2vlan-trunk
    ${trunk_json}     l2vlan.json
    ${trunk_member_json}    l2vlan_member.json


Test Cases
==========

Create l2vlan Transparent Interface
-----------------------------------
This creates a transparent l2vlan interface between two dpns

Test Steps
^^^^^^^^^^
* Create transparent l2vlan interface through REST

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Interface shows up in config
* Interface state shows up in operational
* Flows are added to `Table0` on the bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Create l2vlan transparent interface

    [Documentation]    This testcase creates a l2vlan transparent interface between 2 dpns.
    Log    >>>> Creating L2vlan interface <<<<<
    Create Interface    ${trunk_json}    transparent
    Log    >>>> Get interface config <<<<<
    @{l2vlan}    create list    l2vlan-trunk    l2vlan    transparent    l2vlan    true
    Check For Elements At URI    ${CONFIG_API}/ietf-interfaces:interfaces/    ${l2vlan}
    Log    >>>>> Get interface operational state<<<<
    Wait Until Keyword Succeeds    50    5    get operational interface    ${interface_name}
    ${ovs-check}    Wait Until Keyword Succeeds    40    10    table0 entry    ${conn_id_1}    ${bridgename}


Delete l2vlan Transparent Interface
-----------------------------------
This testcase deletes the l2vlan transparent interface created in previous test case.

Test Steps
^^^^^^^^^^
* Remove all interfaces in config

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Interface config is empty
* Interface states in operational is empty
* Flows are deleted from `Table0` on bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Delete l2vlan transparent interface

   [Documentation]    This testcase deletes the l2vlan transparent interface created between 2 dpns.
    Remove All Elements At URI And Verify    ${CONFIG_API}/ietf-interfaces:interfaces/
    No Content From URI    session    ${OPERATIONAL_API}/ietf-interfaces:interfaces/
    Wait Until Keyword Succeeds    30    10    no table0 entry


Create l2vlan Trunk Interface
-----------------------------
This testcase creates a l2vlan trunk interface between 2 DPNs.

Test Steps
^^^^^^^^^^
* Create l2vlan trunk interface through REST

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Interface shows up in config
* Interface state shows up in operational
* Flows are added to `Table0` on the bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Create l2vlan trunk interface

   [Documentation]    This testcase creates a l2vlan trunk interface between 2 DPNs.
    Log    >>>> Getting file for posting json <<<<<<<
    Create Interface    ${trunk_json}    trunk
    Log    >>>> Get interface config <<<<<
    @{l2vlan}    create list    l2vlan-trunk    l2vlan    trunk    tap8ed70586-6c    true
    Check For Elements At URI    ${CONFIG_API}/ietf-interfaces:interfaces/    ${l2vlan}
    Log    >>>>> Get interface operational state<<<<
    Wait Until Keyword Succeeds    50    5    get operational interface    ${interface_name}
    Wait Until Keyword Succeeds    30    10    table0 entry    ${conn_id_1}    ${bridgename}


Create l2vlan Trunk Member Interface
------------------------------------
This testcase creates a l2vlan Trunk member interface for the l2vlan trunk interface
created in previous testcase.

Test Steps
^^^^^^^^^^
* Create l2vlan trunk member interface through REST

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Interface shows up in config
* Interface state shows up in operational
* Flows are added to `Table0` on the bridge
* Flows match on `dl_vlan`
* Flows have `action=pop_vlan`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Create l2vlan Trunk member interface

   [Documentation]    This testcase creates a l2vlan Trunk member interface for the l2vlan trunk interface already created
    Log    >>>> Creating L2vlan member interface <<<<<
    Log    >>>> Getting file for posting json <<<<<<<
    ${body}    OperatingSystem.Get File    ${genius_config_dir}/l2vlan_member.json
    ${post_resp}    RequestsLibrary.Post Request    session    ${CONFIG_API}/ietf-interfaces:interfaces/    data=${body}
    Log    ${post_resp.content}
    Log    ${post_resp.status_code}
    Should Be Equal As Strings    ${post_resp.status_code}    204
    Log    >>>> Get interface config <<<<<
    @{l2vlan}    create list    l2vlan-trunk1    l2vlan    trunk-member    1000    l2vlan-trunk
    ...    true
    Check For Elements At URI    ${CONFIG_API}/ietf-interfaces:interfaces/    ${l2vlan}
    Log    >>>>> Get interface operational state<<<<
    Wait Until Keyword Succeeds    10    5    get operational interface    ${l2vlan[0]}
    Wait Until Keyword Succeeds    40    10    ovs check for member interface creation    ${conn_id_1}    ${bridgename}


Bind service on Interface
-------------------------
This testcase binds service to the L2vlan Trunk Interface earlier.

Test Steps
^^^^^^^^^^
* Add service bindings for `elan` and `VPN` services on L2Vlan Trunk Interface using REST

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Check bindings for `VPN` and `elan` services exist on L2Vlan Trunk interface
* Flows are added to `Table17` on the bridge
* Flows have action `goto_table:21`
* Flows have action `goto_table:50`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Bind service on Interface

    [Documentation]    This testcase binds service to the interface created .
    Log    >>>> Getting file for posting json <<<<<<<
    ${body}    OperatingSystem.Get File    ${genius_config_dir}/bind_service.json
    ${body}    replace string    ${body}    service1    VPN
    ${body}    replace string    ${body}    service2    elan
    log    ${body}
    ${service_mode}    Set Variable    interface-service-bindings:service-mode-ingress
    ${post_resp}    RequestsLibrary.Post Request    session    ${CONFIG_API}/interface-service-bindings:service-bindings/services-info/${interface_name}/${service_mode}/    data=${body}
    log    ${post_resp.content}
    log    ${post_resp.status_code}
    Should Be Equal As Strings    ${post_resp.status_code}    204
    Log    >>>>> Verifying Binded interface <<<<<
    @{bind_array}    create list    2    3    VPN    elan    50
    ...    21
    Check For Elements At URI    ${CONFIG_API}/interface-service-bindings:service-bindings/services-info/${interface_name}/${service_mode}/    ${bind_array}
    Log    >>>>> OVS check for table enteries <<<<
    ${command}    set variable    sudo ovs-ofctl -O OpenFlow13 dump-flows ${bridgename}
    Wait Until Keyword Succeeds    40    10    table entry    ${command}


Unbind service on Interface
---------------------------
This testcase Unbinds the services which were bound in previous testcase.

Test Steps
^^^^^^^^^^
* Delete service bindings for `elan` and `VPN` services on L2Vlan Trunk Interface using REST

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Check bindings for `VPN` and `elan` services on L2Vlan Trunk interface don't exist
* No flows on `Table0`
* No flows with action `goto_table:21`
* No flows with action `goto_table:50`

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: unbind service on interface

    [Documentation]    This testcase Unbinds the service which is binded by the 3rd testcase.
    Log    >>>>>>Unbinding the service on interface <<<<
    ${service-priority-1}    set variable    3
    ${service-priority-2}    set variable    4
    ${service_mode}    Set Variable    interface-service-bindings:service-mode-ingress
    Remove All Elements At URI And Verify    ${CONFIG_API}/interface-service-bindings:service-bindings/services-info/${interface_name}/${service_mode}/bound-services/${service-priority-1}/
    log    >>>> Ovs check for table 21 absence <<<
    ${table-id}    set variable    21
    Wait Until Keyword Succeeds    10    2    no goto_table entry    ${table-id}
    Remove All Elements At URI And Verify    ${CONFIG_API}/interface-service-bindings:service-bindings/services-info/${interface_name}/${service_mode}/bound-services/${service-priority-2}/
    No Content From URI    session    ${CONFIG_API}/interface-service-bindings:service-bindings/services-info/${interface_name}/${service_mode}/bound-services/${service-priority-2}/
    log    >>>> Ovs check for table 50 absence <<<
    ${table-id}    set variable    50
    Wait Until Keyword Succeeds    10    2    no goto_table entry    ${table-id}


Delete L2vlan Trunk Interface
-----------------------------
Delete l2vlan trunk interface created and used in earlier test cases

Test Steps
^^^^^^^^^^
* Remove all interfaces in config

Test Pass Criteria
^^^^^^^^^^^^^^^^^^
* Interface config is empty
* Interface states in operational is empty
* Flows are deleted from `Table0` on bridge

Troubleshooting
^^^^^^^^^^^^^^^
N.A.

Test Code
^^^^^^^^^

.. code-block:: none
   :caption: Delete l2vlan trunk interface

    [Documentation]    Deletion of l2vlan trunk interface is done.
    Remove All Elements At URI And Verify    ${CONFIG_API}/ietf-interfaces:interfaces/
    No Content From URI    session    ${OPERATIONAL_API}/ietf-interfaces:interfaces/
    ${resp}    Wait Until Keyword Succeeds    30    10    no table0 entry


Implementation
==============

Assignee(s)
-----------

Primary assignee:
  <developer-a>

Other contributors:
  <developer-b>
  <developer-c>


Work Items
----------
N.A.


References
==========

[1] `OpenDaylight Genius usrr Guide <http://docs.opendaylight.org/en/latest/user-guide/genius-user-guide.html#interface-manager-operations>`__
