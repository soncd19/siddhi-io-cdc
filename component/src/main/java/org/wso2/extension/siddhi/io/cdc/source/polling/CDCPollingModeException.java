/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.io.cdc.source.polling;


import org.wso2.siddhi.core.exception.SiddhiAppCreationException;

/**
* This Exception is to be thrown in the CDC polling mode.
*/
public class CDCPollingModeException extends SiddhiAppCreationException {

    public CDCPollingModeException(String message) {
        super(message);
    }

    public CDCPollingModeException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public CDCPollingModeException(Throwable throwable) {
        super(throwable);
    }
}
