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

package org.oxymores.chronix.engine.data;

import java.io.Serializable;
import java.util.UUID;

import org.joda.time.DateTime;

public class TokenRequest implements Serializable
{
    private static final long serialVersionUID = -7250533108215592799L;

    public enum TokenRequestType {
        /** We ask for a token. We are ready to wait until it is available. */
        REQUEST,
        /** We are done with a token and signal it is available again for others who may request it. **/
        RELEASE,
        /**
         * We are still using the token and regularly send this message to signal we are not dead and the token should not be forcefully
         * freed.
         **/
        RENEW,
        /** Token request is accepted. The token is yours. **/
        AGREE
    }

    /**
     * Type of the token message. See enumeration for details.
     */
    public TokenRequestType type;
    public UUID applicationID;
    public UUID tokenID;
    public UUID placeID;
    public UUID stateID;
    public UUID requestingNodeID;
    public UUID pipelineJobID;
    public DateTime requestedAt;

    public boolean local = true;
}
