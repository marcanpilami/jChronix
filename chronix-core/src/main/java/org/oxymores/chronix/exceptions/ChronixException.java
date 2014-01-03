/**
 * By Marc-Antoine Gouillart, 2012
 * 
 * See the NOTICE file distributed with this work for 
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file 
 * except in compliance with the License. You may obtain 
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oxymores.chronix.exceptions;

import edu.emory.mathcs.backport.java.util.Arrays;

public class ChronixException extends Exception
{
    private static final long serialVersionUID = 6684815812625785453L;

    public ChronixException()
    {
        super();
    }

    public ChronixException(String message)
    {
        super(message);
    }

    public ChronixException(String message, Exception innerException)
    {
        super(message, innerException);
    }

    @Override
    public String toString()
    {
        if (this.getCause() != null)
        {
            return this.getMessage() + "\n" + this.getCause().getMessage() + "\n\n" + Arrays.toString(this.getStackTrace());
        }
        else
        {
            return this.getMessage() + "\n\n" + Arrays.toString(this.getStackTrace());
        }
    }
}
