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
package nl.knaw.dans.dvcli.action;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Processes a batch of labeled items by applying an action to each item. The labels are used for reporting. Typically, the label is the ID of the item. After each action, the processor waits for a
 * delay, if specified. The processor reports the results of the actions to a report.
 *
 * @param <I> the type of the items
 * @param <R> the type of action results
 * @see Report for the interface that the report must implement
 */
@Builder
@Slf4j
public class BatchProcessor<I, R> {
    /**
     * The labeled items to process.
     */
    @NonNull
    private final List<Pair<String, I>> labeledItems;

    /**
     * The action to apply to each item.
     */
    @NonNull
    private final ThrowingFunction<I, R, Exception> action;

    /**
     * The report to which the results of the actions are reported.
     */
    @Builder.Default
    private final Report<I, R> report = new ConsoleReport<>();

    /**
     * The delay in milliseconds between processing items. A delay of 0 or less means no delay.
     */
    @Builder.Default
    private final long delay = 1000;

    public void process() {
        log.info("Starting batch processing");
        int i = 0;
        for (var labeledItem : labeledItems) {
            delayIfNeeded(i);
            log.info("Processing item {} of {}", ++i, labeledItems.size());
            callAction(labeledItem.getFirst(), labeledItem.getSecond());
        }
        log.info("Finished batch processing of {} items", labeledItems.size());
    }

    private void callAction(String label, I item) {
        try {
            R r = action.apply(item);
            report.reportSuccess(label, item, r);
        }
        catch (Exception e) {
            report.reportFailure(label, item, e);
        }
    }

    private void delayIfNeeded(int i) {
        if (delay > 0 && i > 0) {
            log.debug("Sleeping for {} ms", delay);
            try {
                Thread.sleep(delay);
            }
            catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

}
