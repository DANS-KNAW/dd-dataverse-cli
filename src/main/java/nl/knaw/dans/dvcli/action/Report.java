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

/**
 * Report the success or failure of an action.
 *
 * @param <T> the type of the item that was processed
 * @param <R> the type of the result of the action
 */
public interface Report<T, R> {

    /**
     * Report a successful action.
     *
     * @param label a label for the item that was processed
     * @param t     the item that was processed
     * @param r     the result of the action
     */
    void reportSuccess(String label, T t, R r);

    /**
     * Report a failed action.
     *
     * @param label a label for the item for which the action was attempted
     * @param t     the item for which the action was attempted
     * @param e     the exception that was thrown
     */
    void reportFailure(String label, T t, Exception e);
}
