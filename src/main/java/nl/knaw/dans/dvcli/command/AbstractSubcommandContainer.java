/*
 * Copyright (C) 2024 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.dvcli.command;

import lombok.NonNull;
import nl.knaw.dans.dvcli.action.BatchProcessor;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.dvcli.action.SingleIdOrIdsFile;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.List;

public abstract class AbstractSubcommandContainer<T> extends AbstractCmd {
    private static final long DEFAULT_DELAY = 1000;

    protected DataverseClient dataverseClient;

    public AbstractSubcommandContainer(@NonNull DataverseClient dataverseClient) {
        this.dataverseClient = dataverseClient;
    }

    @Parameters(index = "0", description = "The target(s) of the operation; this is either an ID a file with a with a list of IDs, or - if the subcommand supports it - a parameters file.",
                paramLabel = "targets", defaultValue = SingleIdOrIdsFile.DEFAULT_TARGET_PLACEHOLDER)

    protected String targets;

    @Option(names = { "-d", "--delay" }, description = "Delay in milliseconds between requests to the server (default: ${DEFAULT-VALUE}).", defaultValue = "" + DEFAULT_DELAY)
    protected long delay;

    protected BatchProcessor.BatchProcessorBuilder<T, String> batchProcessorBuilder() throws IOException {
        return BatchProcessor.<T, String> builder()
            .labeledItems(getItems())
            .delay(delay);
    }

    protected <P> BatchProcessor.BatchProcessorBuilder<P, String> paramsBatchProcessorBuilder() {
        return BatchProcessor.<P, String> builder()
            .delay(delay);
    }

    protected abstract List<Pair<String, T>> getItems() throws IOException;

    @Override
    public void doCall() throws IOException, DataverseException {
    }

    public BatchProcessor<T, String> batchProcessor(ThrowingFunction<T, String, Exception> action) throws IOException {
        return BatchProcessor.<T, String> builder()
            .labeledItems(getItems())
            .delay(delay)
            .action(action)
            .build();
    }
}
