Transition patterns and parallelism
###################################


States are launched on Groups of Places. States are linked by Transitions. But
if the group contains more than one Place, how is the Transition evaluated? Is
it evaluated on a every Place independently, or does it wait for the result on
every Place before becoming possible ? This question is at the heart of
*transition parallelism*.


The first case, waiting for the result on every Place before deciding wether the
transition is possible or not, is the *normal transition*. It is very simple to
grasp: a State must be completely done before the Plan is allowed to go on.
This is the default. Some usual patterns are detailed in the first part of this
chapter.


The second case, deciding on transitions on every Place independently, is the
*parallel enabled transition*. It is a more advance way of using the scheduler.
The second part of the chapter details its how it works.


Without parallelism
*******************

N1. Simple transition
=====================

A (Group 1 with three Places) ---(OK)---> B (Group 2 with two Places)


In this case, B will run on the two Places of group 2 after A has finished
running (and is OK) on all three Places of Group 1. What's important here is
that A must be completely finished on every Place of its Group for B to run on
every Place of its Group. There is no partial start of B on some its Places
before A has finished everywhere.

Note that is doesn't matter if Group 1 = Group2 or if Group 1 and Group 2 have
common Places.

Beware that a single A going awry on one Place means that B will be blocked on
every of its Places! However, this is often what is desired.


N2. AND logical door
====================

A AND logical door is a particuliar job that runs only if all the incoming
transitions are verified.

A (Group 1) ---(OK)---> AND (Group Y) ---> Z (Group Z)


B (Group 2) ---(OK)--|


C (Group 3) ---(OK)--|


AND will be launched on all of the Places of group Y when **all** the following
conditions are met :

* A is over (OK) on all the Places of Group 1

* B is over (OK) on all the Places of Group 2

* C is over (OK) on all the Places of Group 3


The transition between AND and Z is the N1 case: Z will launch as soon as AND is
done on every Place of group Y (which will won't be long as soon as AND is
triggered, given than a AND logical door runs instantaneously).

Once again, a single failure of A, B or C on any of their Places will prevent Z
from running. One may use transitions with softer conditions than "OK", such as
"any result" to prevent this, or have different paths for "OK" and "NOT OK".


With parallélism
****************

P1. Simple parallel transition
==============================

A //(Group 1 with Places P1, P2, P3) ---(OK)---> B //(same Group 1)

In this case, both States run on the **same Group** and have
**enabled parallelism** (notice the "//" sign). This is the one and only
condition to trigger the parallelism pattern.


The result is:

* B will run on P1 as soon as A is done OK on P1

* B will run on P2 as soon as A is done OK on P2

* B will run on P3 as soon as A is done OK on P3


Calendar note
-------------

If a parallelised State uses a Calendar, the calendar advance of the State is
followed Place by Place. So the State may have different *current dates* on its
different Places. As this is often the sign of something gone wrong, it will
trigger a visual alert in the console.


P2. Mixing parallel and normal transitions
==========================================

A //(Group 1) --- (OK) ---> AND //(Group 1) ---> C //(Group 1)

B (Group 2) --- (OK) --|

In this case, we have two parallel transitions (A -> AND and AND -> C), along
with a normal transition (B -> AND). The scheduler will preserve as much
parallelism as it can in that case:

* C will run on P1 as soon as AND is done on P1, and so on: this is same as
  above.
* AND will run on P1 as soon as A is done on P1 (since the A->AND transition
  is parallel enabled) and as soon as B is done on all the Places of Group 2
  (normal case for a non-parallel enabled transition).

Basically, this is the "naturaly expected" conclusion.


P3. Enabling parallelism with different Groups
==============================================

A //(Group 1) ---(OK)---> B //(Group 2)


The two States have enabled parallelism, but the Groups are different. Event if
the two Groups have common Places, this is considered as a normal non-parallel
transition.


P4. Mix of P2 and P3.
=====================

A //(Group 1) --- (OK) ---> AND //(Group 1) ----> C //(Group 1)

B //(Group 2) --- (OK) --|


This is the same case as P2, but for B which is parallel-enabled. However, as B
and AND are not on the same Group, the B -> AND transition is still a normal
one and not a parralel-enabled one.

So the result is the same as P2. What's important here is that it is possible to
mix parallelism and normal executions at will, and that the engine will sort it
out.


P5. Just enabling parallelism on one State
==========================================

A (Group 1) ---(OK)---> B //(Group 1) ---(OK)---> C (Group 1)


Here, all States run on the same Group, but only B is parallel enabled. This is
actually a case without parallelism at all: parallelism deals with transitions,
not states. A transition is only parallel-enabled if both its extremities are
parallel-enabled and run on the same group. No transitions respect these
conditions here. So :

* B will run on every place of group 1 when A is done (OK) on all places of
  group 1.
* C will run on every place of group 1 when B is done (OK) on all places of
  group 1.


