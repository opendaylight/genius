Genius Pipeline
====================

This document captures current OpenFlow pipeline as use by Genius and projects
using Genius for app-coexistence.

High Level Pipeline
===================

::

                          +---------+
                          | In Port |
                          +----+----+
                               |
                               |
                     +---------v---------+
                     | (0) Classifier    |
                     |     Table         |
                     +-------------------+
                     | VM Port           +------+
                     +-------------------+      +----------+
                     | Provider Network  +------+          |
                     +-------------------+                 |
                     | Internal Tunnel   |                 |
                     +-------------------+                 |
              +------+ BGPoMPLS GW       |                 |
              |      +---------+---------+       +---------v---------+
              |                |                 | (17) Dispatcher   |
              |                |                 |      Table        |
   +----------v--------+       |                 +-------------------+
   |     (18,38)       |       |   +-------------+Ing.ACL Service (1)|
   |                   |       |   |             +-------------------+
   |    GW Pipeline    |       |   | +-----------+IPv6 Service    (2)|
   +-------------------+       |   | |           +-------------------+
                               |   | |         +-+L2 Service      (4)|
              +----------------+   | |         | +-------------------+
              |                    | |         | |L3 Service      (3)+-+
   +----------v--------+           | |         | +-------------------+ |
   |       (20)        |           | |         |                       |
   |                   |           | |         |                       |
   |     L3 Pipeline   |           | |         |                       |
   +-------------------+           | |         |                       |
                                   | |         |                       |
                +------------------+ |         |                       |
                |                    |         |                       |
       +--------v--------+           |         |                       |
       |    (40 to 42)   |           |         |                       |
       |  Ingress ACL    |           |         |                       |
       |    Pipeline     |           |         |                       |
       +-------+---------+           |         |                       |
               |                     |         |                       |
            +--v-+      +------------v------+  |                       |
            |(17)|      |      (45)         |  |                       |
            +----+      |                   |  |                       |
                        |   IP^6 Pipeline   |  |                       |
                        +--+-------+--------+  |                       |
                           |       |           |                       |
                        +--v--+ +--v-+   +-----v-----------+           |
                        | ODL | |(17)|   |    (50 to 55)   |           |
                        +-----+ +----+   |                 |           |
                                         |   L2 Pipeline   |           |
                                         +-+---------------+           |
                                           |                           |
                                           |              +------------v----+
                                           |              |    (19 to 47)   |
                                           | +------------+                 |
                                           | |            |   L3 Pipeline   |
                                           | |            +----+-------+----+
                                           | |                 |       |
                                           | |              +--v--+ +--v-+
                                           | |              | ODL | |(17)|
                                           | |              +-----+ +----+
                                           | |
               +-----------------+     +---v-v-------------+
               |  (251 to 253)   <-----+ (220)  Egress     |
               |   Egress ACL    +----->   Dispatcher Table|
               |    Pipeline     |     +--------+----------+
               +-----------------+              |
                                                |
                                                |
                                           +----v-----+
                                           | Out Port |
                                           +----------+


Services Pipelines
==================

Ingress ACL Pipeline
--------------------
Owner Project: Netvirt

TBD.

IPv6 Pipeline
-------------
Owner Project: Netvirt

TBD.

L2 Pipeline
-----------
Owner Project: Netvirt

TBD.

L3 Pipeline
-----------
Owner Project: Netvirt

TBD.

Egress ACL Pipeline
-------------------
Owner Project: Netvirt

TBD.

