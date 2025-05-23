/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink.procedure;

import org.apache.paimon.CoreOptions;
import org.apache.paimon.table.FileStoreTable;
import org.apache.paimon.table.sink.BatchTableCommit;
import org.apache.paimon.utils.ProcedureUtils;

import org.apache.flink.table.annotation.ArgumentHint;
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.ProcedureHint;
import org.apache.flink.table.procedure.ProcedureContext;

import java.util.HashMap;

/** Compact manifest file to reduce deleted manifest entries. */
public class CompactManifestProcedure extends ProcedureBase {

    private static final String COMMIT_USER = "Compact-Manifest-Procedure-Committer";

    @Override
    public String identifier() {
        return "compact_manifest";
    }

    @ProcedureHint(
            argument = {
                @ArgumentHint(name = "table", type = @DataTypeHint("STRING")),
                @ArgumentHint(name = "options", type = @DataTypeHint("STRING"), isOptional = true)
            })
    public String[] call(ProcedureContext procedureContext, String tableId, String options)
            throws Exception {

        FileStoreTable table = (FileStoreTable) table(tableId);
        HashMap<String, String> dynamicOptions = new HashMap<>();
        ProcedureUtils.putIfNotEmpty(
                dynamicOptions, CoreOptions.COMMIT_USER_PREFIX.key(), COMMIT_USER);
        ProcedureUtils.putAllOptions(dynamicOptions, options);

        table = table.copy(dynamicOptions);

        try (BatchTableCommit commit = table.newBatchWriteBuilder().newCommit()) {
            commit.compactManifests();
        }
        return new String[] {"success"};
    }
}
