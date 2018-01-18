
.. contents:: Table of Contents
      :depth: 3

======================================
Running Genius CSIT on Dev Environment
======================================
Genius CSIT requires very minimal testbed topology, and it is very easy to run the same
on your laptop, with the below steps. This will help you run tests yourself on the code changes
you are making in genius,locally, without the need for waiting in jenkins job queue for long.

Test Setup
==========
Test setup consists of ODL with `odl-genius-rest` feature installed and two switches (DPNs) connected
to ODL over OVSDB and OpenflowPlugin channels.

Testbed Topologies
------------------
This setup uses the default Genius test topology.

Default Topology
^^^^^^^^^^^^^^^^

.. literalinclude:: topologies/default-topology.txt

Software Requirements
---------------------

ODL running on a VM or laptop
Two VMs with OVS 2.6 installed
Robotframework which can co-exist with any of the above VMs(provided it has connectivity to all the above entities).

Steps To Bring Up the CSIT Environment
--------------------------------------

We can run ODL on laptop, and OVS on two VMs. RobotFramework can be installed on of the two OVS VMs.
The documentation is based on ubuntu Desktop VMs, which were started using virtual box.

ODL Installation
----------------

* Pick up any ODL stable distribution or build it yourself
* cd genius/karaf/target/assembly/bin
* ./karaf
* In the karaf prompt, install the genius feature
  feature:install odl-genius-rest

OVS Installation
----------------
Most of the genius developers already know this.
Just for completion sake, on both the VMs, OVS has to be installed.

* sudo apt-get install openvswitch-switch
* service openvswitch-switch start
* You can type "ovs-vsctl show" command to check if ovs is running as expected.
* Make sure that the output of the above command should show different unique node UUIDs for OVS. If not, genius CSIT
  will have trouble creating ITM tunnels.[This is likely to happen, if you clone the first VM to run the second OVS]

RobotFrameowork Installation
----------------------------
* Install Python 2.7
* Install pip
* Install ride tool and required libraries:
  sudo pip install robotframework-ride
  sudo pip install robotframework-requests
  sudo pip install robotframework-sshlibrary
  sudo pip install --upgrade robotframework-httplibrary
  sudo pip install jmespath
* To start ride : ride.py <test suite name>
  To open genius test suite, opendaylight integration/test repo needs to be cloned.
  git clone https://<your_username>@git.opendaylight.org/gerrit/p/integration/test.git
  ride.py test/csit/suites/genius
  [Same can be imported after RIDE opens up, if you don't want to specify the path in the prompt]
* In the RIDE window that opens up, Genius test suite will be imported now
* Click on the Run panel, and Click Start, by passing below arguments
  -v ODL_SYSTEM_IP:<ODL_IP>   -v TOOLS_SYSTEM_IP:<OVS1_IP>  -v TOOLS_SYSTEM_2_IP:<OVS2_IP> -v USER_HOME:<home-folder> -v TOOLS_SYSTEM_USER:<user-name>
  Any arguments defined in Variables.py can be overriden, by passing the argument value like above.
  For eg:, there was a recent change in karaf prompt, in that case we could run genius csit by passing argument "-v KARAF_PROMPT:karaf@root"

References
==========

[1] `OpenDaylight Genius usrr Guide <http://docs.opendaylight.org/en/latest/user-guide/genius-user-guide.html#interface-manager-operations>`__
