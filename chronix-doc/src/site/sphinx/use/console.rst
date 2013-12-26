Operation console
#################

All operations necessary for the day to day operating of the production plan should be done through
the op console. It is available on the *console node* at the address http://consoledns:9000/console.html.

It is mainly a listing of launches. When a line is selected, a palette of options appears.

The different operations are:

* Checking the executions (OK, not OK, ...) through a listing of all runs that can filtered, and sorted along many
   attributes (launch time, application, node, job name, chain name...)
* Duplicating a launch that ended OK. The environment will be the same (env vars, node) but the new launch will
   have no impact on the plan.
* Restart a failed job. Once a previously failed job has succeeded, the failure will disappear from the console.
* Create an arbitrary new launch (inside or outside the plan).
* Kill a running job.