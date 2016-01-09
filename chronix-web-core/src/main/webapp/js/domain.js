/* global uuid, network */


////////////////////////////////////////////////////////////////////////////////
// Shells
////////////////////////////////////////////////////////////////////////////////

function filterStatesUsing(app, sourceId)
{
    var res = [];
    $.each(app.chains, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            var state = this;
            if (state.representsId === sourceId)
            {
                res.push(state);
            }
        });
    });
    $.each(app.plans, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            var state = this;
            if (state.representsId === sourceId)
            {
                res.push(state);
            }
        });
    });
    return res;
}

function removeShell(app, shellId, removeItself)
{
    // Clean from states
    var toDelete = filterStatesUsing(app, shellId);
    $.each(toDelete, function ()
    {
        removeState(app, this.id);
    });

    // Remove from app
    if (removeItself)
    {
        $.each(app.shells, function (i)
        {
            if (this.id === shellId)
            {
                app.shells.splice(i, 1);
            }
        });
    }
}

////////////////////////////////////////////////////////////////////////////////
// Externals
////////////////////////////////////////////////////////////////////////////////

function removeExternal(app, id, removeItself)
{
    // Clean from states
    var toDelete = filterStatesUsing(app, id);
    $.each(toDelete, function ()
    {
        removeState(app, this.id);
    });

    // Remove from app
    if (removeItself)
    {
        $.each(app.externals, function (i)
        {
            if (this.id === id)
            {
                app.externals.splice(i, 1);
            }
        });
    }
}

////////////////////////////////////////////////////////////////////////////////
// Groups
////////////////////////////////////////////////////////////////////////////////

function removeGroup(app, id)
{
    // Remove states using this group
    var toDelete = [];
    $.each(app.chains, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            var state = this;
            if (state.runsOnId === id)
            {
                toDelete.push(state);
            }
        });
    });
    $.each(app.plans, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            var state = this;
            if (state.runsOnId === id)
            {
                toDelete.push(state);
            }
        });
    });

    $.each(toDelete, function ()
    {
        removeState(app, this.id);
    });

    // Clean places referencing this group
    $.each(network.places, function ()
    {
        var place = this;
        $.each(place.memberOf, function ()
        {
            place.memberOf.splice(place.memberOf.indexOf(id), 1);
        });
    });
}

////////////////////////////////////////////////////////////////////////////////
// States
////////////////////////////////////////////////////////////////////////////////

function removeState(app, stateId)
{
    var toDelete = [];
    function clean()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            if (this.id === stateId)
            {
                toDelete.push(this);
            }
        });
        $.each(toDelete, function ()
        {
            chain.states.splice(chain.states.indexOf(this), 1);
        });
        toDelete = [];

        $.each(chain.transitions, function ()
        {
            if (this.from === stateId || this.to === stateId)
            {
                toDelete.push(this);
            }
        });
        $.each(toDelete, function ()
        {
            chain.transitions.splice(chain.transitions.indexOf(this), 1);
        });
        toDelete = [];
    }

    $.each(app.chains, clean);
    $.each(app.plans, clean);
}

function newState(app, x, y, representsId, runsOnId)
{
    var represents = null;
    var runsOnName = null;
    var type = null;

    var canReceiveLink = false, canEmitLinks = false, canBeRemoved = false, canReceiveMultipleLinks = false, isStart = false,
            isEnd = false, isAnd = false, isOr = false;

    // Look for the linked source
    $.each(app.shells, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "shell";
            canReceiveLink = true;
            canEmitLinks = true;
            canBeRemoved = true;
        }
    });
    $.each(app.chains, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "chain";
            canReceiveLink = true;
            canEmitLinks = true;
            canBeRemoved = true;
        }
    });
    $.each(app.plans, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "plan";
        }
    });
    $.each(app.clocks, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "clock";
            canEmitLinks = true;
            canBeRemoved = true;
        }
    });
    $.each(app.externals, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "external";
            canEmitLinks = true;
            canBeRemoved = true;
        }
    });
    $.each(app.calnexts, function ()
    {
        if (this.id === representsId)
        {
            represents = this;
            type = "calnext";
            canReceiveLink = true;
            canEmitLinks = true;
            canBeRemoved = true;
        }
    });
    if (app.andId === representsId)
    {
        represents = {name: 'AND'};
        type = "and";
        canReceiveLink = true;
        canEmitLinks = true;
        canBeRemoved = true;
        canReceiveMultipleLinks = true;
        isAnd = true;
    }
    if (app.orId === representsId)
    {
        represents = {name: 'OR'};
        type = "or";
        canReceiveLink = true;
        canEmitLinks = true;
        canBeRemoved = true;
        canReceiveMultipleLinks = true;
        isOr = true;
    }
    if (app.startId === representsId)
    {
        represents = {name: 'Chain start'};
        type = "start";
        canEmitLinks = true;
    }
    if (app.endId === representsId)
    {
        represents = {name: 'Chain end'};
        type = "end";
        canReceiveLink = true;
    }

    if (!represents)
    {
        console.error("could not find an event source with ID " + representsId);
        return null;
    }

    // Find group
    var group = null;
    $.each(app.groups, function ()
    {
        if (this.id === runsOnId)
        {
            group = this;
        }
    });
    if (!group)
    {
        console.error("could not find an group with ID " + runsOnId);
        return null;
    }
    runsOnName = group.name;

    return {
        id: uuid.v4(),
        x: x,
        y: y,
        label: represents.name,
        representsId: representsId,
        runsOnId: runsOnId,
        runsOnName: runsOnName,
        WarnAfterMn: null, KillAfterMn: null, MaxPipeWaitTime: null, EventValidityMn: null,
        calendarShift: 0,
        parallel: false,
        canReceiveLink: canReceiveLink, canEmitLinks: canEmitLinks, canBeRemoved: canBeRemoved,
        canReceiveMultipleLinks: canReceiveMultipleLinks,
        start: isStart, end: isEnd, and: isAnd, or: isOr
    };
}


////////////////////////////////////////////////////////////////////////////////
// Chains
////////////////////////////////////////////////////////////////////////////////

function newPlan(app)
{
    var res = {id: uuid.v4(), name: "plan " + (app.plans.length + 1), description: "mandatory description", states: [], transitions: []};
    app.plans.push(res);
    return res;
}

function newChain(app)
{
    var res = {id: uuid.v4(), name: "chain " + (app.chains.length + 1), description: "mandatory description", states: [], transitions: []};
    var start = newState(app, 150, 50, app.startId, app.groups[0].id);
    var end = newState(app, 150, 350, app.endId, app.groups[0].id);
    res.states.push(start);
    res.states.push(end);
    app.chains.push(res);
    return res;
}

function removeChain(app, chain, removeItself)
{
    var toDelete = filterStatesUsing(app, chain.id);
    $.each(toDelete, function ()
    {
        removeState(app, this.id);
    });

    // Remove from app
    if (removeItself)
    {
        $.each(app.chains, function (i)
        {
            if (this.id === chain.id)
            {
                app.chains.splice(i, 1);
            }
        });
        $.each(app.plans, function (i)
        {
            if (this.id === chain.id)
            {
                app.plans.splice(i, 1);
            }
        });
    }
}