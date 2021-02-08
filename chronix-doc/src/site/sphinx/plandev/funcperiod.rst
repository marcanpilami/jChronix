Intrinsic period
################

Intrinsic periods are scheduling constraints that are not relative to other
objects (like a constraint "do not run X while Y is running") but to the
previous runs of the object itself. They allow to model some complex
functional conditions:

* Do not run if a previous execution is still ongoing

* Only run on certain dates (following a business calendar)


* Make external events (such as the arrival of a file) correspond to a
  particular launch (i.e. if a chain C is waiting for two files F1 and F2, only
  launch C on date D if both F1 and F2 of date D have arrived and not F1 and F2
  from two different dates)

* Force sequential execution following a functional calendar (only launch
  date D+1 if D has finished)


Basically, this is the use of functional calendars and sequences.

Should I use this?
******************

If you consider your scheduler as a crontab++, no. If your plan is strongly
dependent upon a (or many) period that directly comes from a business
constraint, this is the way to implement it.

.. note::

   by default, it is not enabled.

Calendars
*********

Definition
==========

They represent the intrinsic temporal progress of business. For example, a
retailer will often have a batch job for each day its stores are open to do
various closure computations. The calendar in this case will contain the open
days.

.. warning::
   Calendars are totally distinct from clocks. Clocks are technical: they trigger 
   something at a given time.
   Calendars are functional constraints: they trace the business date/time. 
   The two notions - trigger time and business time - are actually independent.
   In the retailer example, one could have chosen to have all the daily jobs on 
   Saturday (a technical decision, for example because Saturdays are the only 
   days when there is enough computation power available), one after another. 
   In that case, there are five triggers on Saturdays,
   but each resulting execution will correspond to one different business day. 


There are many example of business calendars in scheduling plans: financial
markets open days for investment firms, school days for education businesses,
bank holidays for banks, ... Most businesses have one, if only "every day".
(The "very day" calendar is far from stupid, as will be shown in the rest of
this paragraph)

.. note::

   We are talking of days, however calendars can only use hours and below. But 
   beware: a job scheduler is not supposed to do real time processing and 
   an hour periodicity should in most cases be considered an absolute minimum.
   
A calendar also bears the notion of "current day". This is optional. The
current day designate the day that is currently being used or that will be used
on next launch if all is already done. Basically, it is a mark on the calendar
designating the next day to run. It is useful both for operators who get a
clearer view of future exploitation, and also to prevent running chains too
soon: it is forbidden to run on a date greater than the current date.


Creation
========
A calendar is defined by a number of ordered occurrences. There is no recurrence
to define calendars are there are for clocks, as one would often be hard
pressed to find a set of simple recurrences and exceptions that would describe
a business calendar (for example, the reader is invited to try with the
religious calendar of his choice). Occurrences are therefore defined manually.



The actual value of an occurrence (50 characters of text, that will usually be
used to represent a date + time value but could be whatever is needed) is of no
importance to the scheduler itself. What is important to it is their order. The
actual value will be given as an environment variable to the various jobs
should they have use for it.


Actual use
==========
A calendar can be associated with states during chain definition. There is no
inheritance of calendars: if a chain is associated to a calendar, the elements
it contains will not.

The element can specify a calendar shift that will allow it to run on the same
calendar as everyone, but shifted by the given number of occurrences. (for
example, one partner of our retailer sends many files that should be processed
every night. All of these files relate to activity of the previous day (D),
except for one that related to day before that (D-1). The same


Scheduling consequences
=======================

On an macroscopic level, a calendar occurrence groups under its label (often a
date) different runs of batch jobs. It helps identify "the 23/07 batch night"
from "the 24/07 batch night", even if some jobs from the 23 actually ran the
24, or if some 24 jobs were advanced the 23.

The following constraints are evaluated either once an element was triggered and
entered the pre-execution queue (just before exclusions are evaluated) or
during event analysis.

.. note:: 

   If there are no calendars associated to the state, the scheduler will act as
   a crontab: elements will run when triggered without paying attention to
   still running previous launches, the status of previous runs, ...

#. There can be only one instance of the job running on a single place
#. Sequenciality will be enforced: occurrence O+1 can only run if occurrence
   O has finished (and ended OK) on a given Place.
#. If the calendar "current occurrence" is D, no job using this calendar can
   run on a further occurrence. This takes shifting onto account.
#. Calendar progress is only made if the job succeeds.


Calendars have no consequences on transitions :

#.  If a job fails, its calendar status won't be updated and the next time
    you'll run it, it will be on the same occurrence as described above. But it
    won't prevent transitions from the failed job to others that may be
    triggered on its failure.
#. In an A->B transition, calendar status of A is not taken into account for
   launching B (A may be for example 10 occurrences late from B, and still B
   will launch after A.) It allows to mix jobs on different occurrences in a
   chain.




Impact on operations and operators
==================================


Calendars often make the plan as a whole a little more complicated.

Using calendars enables the following operations :

* modifying the current occurrence of a calendar

* modifying the next date a State will use (forwards or backwards)



Note that a disabled job will not update its calendar status so that to enable
to rerun missed opportunities. If this is not wanted, the "next date" for the
State will have to be set manually.


In case multiple occurrences have been missed (for example, the job has been
disabled for some time), the scheduler can rerun the job in a loop if the
correct option is checked. By default, this is of course disabled as it can
have unforeseen consequences. In case this is enabled, a chain A -> B - > C
where B is three days late on its calendar will see this execution sequence: A
-> B (D-2) -> B (D-1) -> B (D) -> C. Of course, that only works if the
calendar's current occurrence is defined (otherwise the scheduler would not
know where to stop).


Behind the scenes...
====================

Every State associated to a calendar keeps what is called a Calendar Pointer, an
element that keeps track (for each Place the State runs on) of the latest
occurrence used on a calendar. After each successful run, the pointer is
updated to the next occurrence in the calendar.


The current occurrence is the value of the pointer for the State that was
specified as "end of run".

