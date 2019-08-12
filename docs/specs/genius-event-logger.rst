.. contents:: Table of Contents
      :depth: 3

========================================
Genius Event Logging Using Log4j2
========================================

`Genius Event Logging Reviews <https://git.opendaylight.org/gerrit/#/c/genius/+/83210/>`__

Genius event logger is the feature which is used to log some important events of Genius into a separate
file using log4j2.

User should configure log4j appender configuration for genius event logger in ``etc/org.ops4j.pax.logging.cfg``
file to achieve this.


Problem Description
===================
When many log events are available in karaf.log file, it will be difficult for user to quickly find the main events with
respect to genius southbound connection. And also, as there will be huge amount of karaf logs, there are chances of log
events getting rolled out in karaf.log files. Due to which we may tend to miss some of the events related to genius.

Genius event logger feature is intended to overcome this problem by logging important events of genius into a separate
file using log4j2 appender, so that user can quickly refer to these event logs to identify important events of genius
related to connection, disconnection, reconciliation, port events, errors, failures, etc.

Use Cases
---------
1. By default genius event logging feature will not be enabled without any configuration changes in logging
   configuration file.

2. User can configure log4j2 appender for genius event logger(as mentioned in the configuration section) to
   log the important logs of genius in a separate file at the path mentioned in configuration file.

Proposed Change
===============
1. A log4j2 logger with name ``GeniusEventLogger`` will be created and used to log the event at the time of connection,
   disconnection, reconciliation, etc.

2. By default the event logger logging level is fixed to DEBUG level. Unless there will be an appender configuration
   present in logging configuration file, the events will not be in enqueued for logging.

3. The genius event logs will be having a pattern consisting of time stamp of the event, description of event
   followed by the datapathId of the switch for which events are related.

4. The event logs will be moved to a separate file(data/events/genius/genius.log file as per the configuration
   mentioned in configuration section) and this can be configured to different path as per the need.

5. The file roll over strategy is chosen as to roll events into other file if the current file reaches maximum size(10MB
   as per configuration) and the event logs will be overwritten if such 10 files(as per configuration) are completed.

Command Line Interface (CLI)
============================
None.

Other Changes
=============

Pipeline changes
----------------
None.

Yang changes
------------
None.

Configuration impact
--------------------
Below log4j2 configuration changes should be added in ``etc/org.ops4j.pax.logging.cfg`` file for logging genius events
into a separate file.

.. code-block:: none
   :caption: org.ops4j.pax.logging.cfg

   log4j2.logger.genius.name = GeniusEventLogger
   log4j2.logger.genius.level = INFO
   log4j2.logger.genius.additivity = false
   log4j2.logger.genius.appenderRef.GeniusEventRollingFile.ref = GeniusEventRollingFile

   log4j2.appender.genius.type = RollingRandomAccessFile
   log4j2.appender.genius.name = GeniusEventRollingFile
   log4j2.appender.genius.fileName = \${karaf.data}/events/genius/genius.log
   log4j2.appender.genius.filePattern = \${karaf.data}/events/genius/genius.log.%i
   log4j2.appender.genius.append = true
   log4j2.appender.genius.layout.type = PatternLayout
   log4j2.appender.genius.layout.pattern =  %d{ISO8601} | %m%n
   log4j2.appender.genius.policies.type = Policies
   log4j2.appender.genius.policies.size.type = SizeBasedTriggeringPolicy
   log4j2.appender.genius.policies.size.size = 10MB
   log4j2.appender.genius.strategy.type = DefaultRolloverStrategy
   log4j2.appender.genius.strategy.max = 10

Clustering considerations
-------------------------
The genius event logger will be configured in the controller and are related to log events only in that controller. This
will not be affecting cluster environment in any way.

Other Infra considerations
--------------------------
N.A.

Security considerations
-----------------------
None.

Scale and Performance Impact
----------------------------
None.

Targeted Release
----------------
Sodium.

Alternatives
------------
N.A.

Usage
=====

Features to Install
-------------------
included with common genius features.

REST API
--------
None

CLI
---
None

Implementation
==============

Assignee(s)
-----------
Primary assignee:
 - Nidhi Adhvaryu(nidhi.adhvaryu@ericsson.com)


Work Items
----------
N.A.

Dependencies
============
This doesn't add any new dependencies.


Testing
=======
1. Verifying the event logs in karaf.log file, when there is no appender configuration added in logger configuration
   file.
2. Making appender configuration in logger configuration file and verifying the important events of genius in the log
   file specified in configuration.

Unit Tests
----------
None added newly.

Integration Tests
-----------------
None

CSIT
----
None

Documentation Impact
====================

References
==========