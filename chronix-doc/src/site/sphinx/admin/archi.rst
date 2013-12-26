Deployment Architecture
#######################

General description
*******************

Chronix is a distributed system offering many different ways to deploy.

A *compute node* is a chronix process that will be able to process the plan (i.e. react to events in order to trigger launches)
and also to run processes, actually running jobs.

A *runner node* is a chronix process that only has the "run process" part - it cannot decide to launch anything. For all
purposes, it is a remote control agent for another *compute node*.

All compute nodes are independent - the failure of a node has no impact whatsoever on the other nodes themselves. (This does not mean it has no impact at all:
the production plan may have dependencies on jobs run on another node. Therefore: no technical impact, but potentially functional impact)

Communication between the nodes is done through a routed bus. It means you have to open some ports, describe the network links between the nodes
and Chronix will take care of the rest. In that regard, the following data may be of interest:

* Compute nodes can either create or receive links
* Runner nodes can only receive links
* All links are duplex - making it far easier to cross firewalls
* Routed means there is no need for direct links between nodes - as long as there is a route through other nodes, it will work
* Every node has a local database that guarantees no message will be ever lost even if a node is shutdown/crashes.
  (some may be withhold until the node is restarted or network is reestablished)
* LAN and WAN compatible (based on a JMS implementation named ActiveMQ). However, for better performances, WAN links should be avoided.

A distributed system is not easy to administrate. That's why a specific node is designated as the *console node*. This node is a
compute node that boasts a web server holding the administration console and the dev environment. Every log is always sent to this node.
(compute nodes hold all the logs of jobs that have run locally as well as the logs of the runner nodes they control. Runner nodes do
not keep logs).

Ports
*****

All ports may be changed at will. Please note that ports are specified inside the parameter file of each node. Afterwards, all nodes should be
declared inside the plan definition with exactly the same data for interfaces and JMS port.

+---------+-----------------------------------------------------------------------------------------+-----------+
| Name    | Description                                                                             | Default   |
+---------+-----------------------------------------------------------------------------------------+-----------+
| JMS     | The port used for communicating between nodes.                                          | 1789      |
+---------+-----------------------------------------------------------------------------------------+-----------+
| HTTP    | A port to expose the admin console and dev console. Only needed on the console node.    | 9000      |
+---------+-----------------------------------------------------------------------------------------+-----------+
| JMX     | **not implemented** Port for JMX administration                                         | N/A       |
+---------+-----------------------------------------------------------------------------------------+-----------+

.. note:: you may choose different ports for each node inside the same deployment. Just be careful when you change ports
   inside an already running network to report the changes inside the plan definition before you change them in the
   configuration file of the node.


Types of deployment
*******************

This section gives the three mostly used architectures - this does not mean one should limit himself to these. Very often,
different choices will be made for different parts of the information system.

Centralized
-----------

A single console node remotely controls all the servers (which sport a runner node each).

Pros:

* Simple

Cons:

* Network: the console must access all the servers
* Reliability: the console is a Single Point Of Failure (SPOF), which really goes against the very idea of Chronix.

Fully distributed
-----------------

Every operating system (be it a physical server of a virtual machine) has its own Chronix compute node.

Pros:

* reliability
* scaling

Cons:

* more monitoring
* more complex, therefore more prone to errements

Application centric
-------------------

Most often, the fully distributed model is overkill: its main advantage is reliability, but it's hard to consider that the production plan
is safe just because most compute nodes survive. Indeed, a plan will very often spread over muliple servers, and have jobs that can be
launched on a node only after other have finished on other servers. So loosing a server will block jobs on others.

This, however, is only true inside the same application. Applications are supposed to be independent of each other (the only point of
contact between applications should always be data flows - file reception, interface table filling, ... This is modelized as an "external event" inside
Chronix so as not to create a spaghetti monster of mingled plans). Therefore, applications do have separate levels of service. In this regard,
CHronix should b deployed with one compute node per application, the rest being runner nodes. And should performance scaling become an issue,
later converting a runner into a compute node is a painless operation, so this should not stop you from choosing this solution.

**This is the preferred deployment model in most cases**.


Availability
************

Chronix itself does not offer HA mechanisms. However, it is compatible with active/passive cluster systems relying on
a shared storage (HACMP, Windows cluster, ...) or a synchronously replicated storage (as some ZFS modes). Indeed, Chronix internals
are fully transactional, with transactions persisted to disk at commit time. Therefore, storage availability will imply Chronix
availability.

Note that the passive node must be accessible through an interface with the same name as the active node once activated, and that it cannot be
started as long as the primary node is still alive (this **would corrupt all Chronix data**). There is no need to replicate other server environment
parameters, such as IP addresses, therefore Chronix should not add any constraints to your cluster.

That being said, Chronix itself is a distributed system, and that very fact entails inherent availability: a node may be lost, but the
network of nodes survives. Therefore, instead of having few clusterized nodes, it may prove a lot more productive to have the "right" number of nodes inside
a network (see above for deployment types).

Monitoring
**********

Through JMX one day. Currently: a log file that should be mined. Log level can be changed.

Peformances
***********

Chronix is not exactly fast - and it has no reason to be, as it is for batch rather than real time processes. Should performances become an issue,
the natural reaction is to do horizontal scaling, by distributing your jobs on more nodes.

.. note:: most of the time, the limiting factor is not the compute engine but the underlying servers ability to actually run the jobs! Performance
   issues should only happen on compute nodes controlling a great number of runner nodes.  
   
The limiting factor and main performance metric is the number of events analyzed per second. Indeed, this is the core job of Chronix: decide whether
or not to launch a job when "something" (an event) happens. But it should be noted that this metric is somewhat twisted - the length of an
analysis depends on the complexity of your plan. Some events may be rejected at once, some only at the last filter. Etc.
Therefore, testing is the only way to determine how many events/s Chronix may handle inside your environment. But as a rule of thumb, consider
that on a 2GHz Intel x64 CPU, about 10 events/s may be handled (and during the handling, a full core will be taken). As event analysis is single
threaded (event order is important !), SMT or multi core systems do not have much impact (they help the other aspects of Chronix which are heavily
multithreaded). (and by the way, there is one event created per ended job)

Volumetry
*********

Chronix uses two directories:

* the engine install directory
* the repository

The engine only contains binaries. No data is ever created there.

The repository contains everything a node needs (by the way, this enables to run multiple nodes with different repositories with the
same engine).

* chronix internal log
* job logs
* persistence data for reliability

The main source of volumetry is job logs. Chronix does not purge them - therefore, the required volumetry can be deduced from the amount
of logs you want to retain. (please note that for Chronix, a log is the standard and error output. Therefore, the generated volume entirely
depends on your jobs).


