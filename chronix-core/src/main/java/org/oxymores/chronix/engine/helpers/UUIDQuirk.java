/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.engine.helpers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.sql2o.quirks.NoQuirks;

/**
 *
 * @author Marc-Antoine
 */
public class UUIDQuirk extends NoQuirks
{

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx, Object value) throws SQLException
    {
        if (value instanceof UUID)
        {
            statement.setString(paramIdx, ((UUID) value).toString());
        }
        else
        {
            super.setParameter(statement, paramIdx, value); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
