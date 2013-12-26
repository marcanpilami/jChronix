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

A Sun/Oracle standard Java 1.6 or 1.7 JSE JRE. (Only JRE is required - no need for JDK). Note that full JSE
(Java Standard Edition) is needed - Chronix is probably not compatible with partial Java implementations such as Android's.

Chronix is only supported on the latest JVM patch. If this is a problem for you, please note that you can start Chronix with a
specific JVM used only for it and therefore patch it without impacting other software.

Chronix was **not** tested with an IBM or an OpenJDK JVM.

Network
=======

If you plan to use Chronix in a multi server environment, you must open the required ports. See :ref:`deployment`.

Installation
************

Choose whether you will separate the runner agent from the compute server. See :ref:`deployment` for this. On one hand,
a single system will ease operations. On the other hand, separating the two allows to patching the compute server with minimal
to no downtime (as the runner agent is hardly ever patched).

.. note::
   The binaries are the same for both runners and engines. The difference between the two is merely the value of a parameter. This also means that
   it is not necessary to reinstall anything to convert a runner to an engine and reciprocally. 


Single process
==============

Choose an installation directory. On Windows, the suggestion is $env:ProgramFiles\\Oxymores\\chronix. On Unix-like system, /opt/freeware/oxymores/chronix.
You are actually free to choose whatever directory you want. This directory will be refered to as CHRONIX_HOME in the rest of this document
(but note there is no need to actually create an environment variable named like that).

Simply unzip the zip file as CHRONIX_HOME ("as" meaning: there should be a CHRONIX_HOME/doc directory after unzipping).

Under Windows, create a service with the script sphinx.ps1::

   chronix.ps1 -InstallEngine [ -JavaHome <xxx> ] [ -UserName <xxx> -Password <xxx> ]

Under Linux, use your preferred service provider.

Next, you may want to create a node parameter file by copying CHRONIX_HOME/conf/chronix.properties.sample as CHRONIX_HOME/conf/chronix.properties.
Edit this file to your liking. Most importantly, choose a repository directory - this directory will hold all the plan data (the plan definition
and the transient data created during run).

.. warning::

   If no parameter file is present, Chronix will use defaults that are sensible... for Windows but stupid for Unix.

Separate runner and engine
==========================

Use the procedure for single process. Afterwards, use the following script to create a separate runner:::

   chronix.ps1 -InstallRunner [ -ServiceName <xxx> ] [ -JavaHome <xxx> ] [ -UserName <xxx> -Password <xxx> ] [ -Port <iii> ]

This will copy all the binaries into CHRONIX_HOME/runners/<ServiceName> and create a new service. A runner also has a parameter file,
but there should be no need to change it as all interesting parameters can be specified with chronix.ps1.

Multiple engines on the same server
===================================

There is no problem with having multiple runners and engines on the same server/computer. Just be careful with TCP ports - Chronix
always binds all available interfaces so there is no way to share a port. As Chronix can run on any port, this is not much of a restriction.


Uninstall
*********

First, delete the services using chronix.ps1 under Windows, or your Unix-like service maanger. Then, simply delete the files inside CHRONIX_HOME.