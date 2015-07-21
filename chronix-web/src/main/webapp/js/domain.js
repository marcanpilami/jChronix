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