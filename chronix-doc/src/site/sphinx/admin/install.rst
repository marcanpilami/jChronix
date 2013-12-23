Installation
############

Prerequisites
*************

Operating system
================

One of the following operating systems:
* Windows 2003R2, 2008, 2008R2, 2012, 2012R2 (and for tests the client versions: XP, Vista, 7, 8, 8.1.)
* RHEL 5.x
* Ubuntu 11.x

Please note that these are the tested OS. Other OS should be fine, as long as you have a supported full Java SE 6 JVM.

Java
====

A Sun/Oracle standard Java 1.6 or 1.7 JSE JRE. (Only JRE is recquired - no need for JDK). Note that full JSE
(Java Standard Edition) is needed - Chronix is probably not compatible with partial Java implementations such as Android's.

Chronix is only supported on the latest JVM patch. If this is a problem for you, please note that you can start Chronix with a
specific JVM used only for it and therefore patch it without impacting other software.

Chronix was **not** tested with an IBM or an OpenJDK JVM.

Network
=======

If you plan to use Chronix in a multi server environment, you must open the recquired ports. See :ref:`deployment`.

Installation
************

Choose whether you will separate the runner agent from the compute server. See :ref:`deployment` for this. On one hand,
a single system will ease operations. On the other hand, separating the two allows to patching the compute server with minimal
to no downtime (as the runner agent is hardly ever patched).

Single process
==============

Simply unzip the zip file into a directory.