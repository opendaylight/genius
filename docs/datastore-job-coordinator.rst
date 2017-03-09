Datastore Job Coordination framework
------------------------------------

The datastore job coordinator framework offers the following benefits :

#. “Datastore Job” is a set of updates to the Config/Operational
   Datastore.
#. Dependent Jobs (eg. Operations on interfaces on same port) that need
   to be run one after the other will continue to be run in sequence.
#. Independent Jobs (eg. Operations on interfaces across different
   Ports) will be allowed to run paralelly.
#. Makes use of ForkJoin Pools that allows for work-stealing across
   threads. ThreadPool executor flavor is also available… But would be
   deprecating that soon.
#. Jobs are enqueued and dequeued to/from a two-level Hash structure
   that ensures point 1 & 2 above are satisfied and are executed using
   the ForkJoinPool mentioned in point 3.
#. The jobs are enqueued by the application along with an application
   job-key (type: string). The Coordinator dequeues and schedules the
   job for execution as appropriate. All jobs enqueued with the same
   job-key will be executed sequentially.
#. DataStoreJob Coordination to distribute jobs and execute them
   paralelly within a single node.
#. This will still work in a clustered mode by handling optimistic lock
   exceptions and retrying of the job.
#. Framework provides the capability to retry and rollback Jobs.
#. Applications can specify how-many retries and provide callbacks for
   rollback.
#. Aids movement of Application Datastore listeners to “Follower” also
   listening mode without any change to the business logic of the
   application.
#. Datastore Job Coordination function gets the list of listenable
   futures returned from each job.
#. The Job is deemed complete only when the onSuccess callback is
   invoked and the next enqueued job for that job-key will be dequeued
   and executed.
#. On Failure, based on application input, retries and/or rollback will
   be performed. Rollback failures are considered as double-fault and
   system bails out with error message and moves on to the next job with
   that Job-Key.
