/* * Copyright (c) 2012 Global Radio UK Limited *  * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at *  *   http://www.apache.org/licenses/LICENSE-2.0 *  * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     * See the License for the specific language governing permissions and  * limitations under the License. */package org.radiodns;

import java.util.List;

/**
 * * RadioDNS Application * * @author Byrion Smith
 * <byrion.smith@thisisglobal.com> * @version 1.0.1
 */
public class Application {
    private String mApplicationId;
    private List<Record> mRecords;

    public Application(String applicationId, List<Record> records) {
        mApplicationId = applicationId;
        mRecords = records;
    }

    public String getApplicationId() {
        return mApplicationId;
    }

    public List<Record> getRecords() {
        return mRecords;
    }
}
