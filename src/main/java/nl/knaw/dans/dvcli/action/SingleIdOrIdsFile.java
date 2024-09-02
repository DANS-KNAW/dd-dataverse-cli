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

import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

@AllArgsConstructor
public class SingleIdOrIdsFile {
    public static final String DEFAULT_TARGET_PLACEHOLDER = "__DEFAULT_TARGET_PLACEHOLDER__";

    private final String singleIdOrIdFile;
    private final String defaultId;

    @SuppressWarnings("resource")
    public Stream<String> getPids() throws IOException {
        if (singleIdOrIdFile.equals(DEFAULT_TARGET_PLACEHOLDER)) {
            return Stream.of(defaultId);
        }

        Stream<String> lines;

        if ("-".equals(singleIdOrIdFile)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            lines = reader.lines();
        }
        else {
            var pidFile = Paths.get(singleIdOrIdFile);
            if (Files.exists(pidFile)) {
                if (!Files.isRegularFile(pidFile)) {
                    throw new IOException(singleIdOrIdFile + " is not a regular file");
                }
                lines = Files.lines(pidFile)
                    .flatMap(line -> Arrays.stream(line.trim().split("\\s+")));
            }
            else {
                lines = Stream.of(singleIdOrIdFile);
            }

        }
        // Split lines further by whitespace
        return lines.flatMap(line -> Arrays.stream(line.trim().split("\\s+")));
    }
}
