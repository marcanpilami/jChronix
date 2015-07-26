/* global uuid */


////////////////////////////////////////////////////////////////////////////////
// Shells
////////////////////////////////////////////////////////////////////////////////

function removeShell(app, shellId, removeItself)
{
    // Clean from states
    var toDelete = [];
    $.each(app.chains, function ()
    {
        var chain = this;
        $.each(chain.states, function ()
        {
            var state = this;
            if (state.representsId === shellId)
            {
                toDelete.push(state);
            }
        });
    });
    $.each(toDelete, function ()
    {
        console.debug(this);
        removeState(app, this.id);
    });
    toDelete = [];

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
// States
////////////////////////////////////////////////////////////////////////////////

function removeState(app, stateId)
{
    var toDelete = [];
    $.each(app.chains, function ()
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
    });
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