package nl.knaw.dans.dvcli;/*
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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;

/**
 * A test class that creates a test directory for each test method.
 */
public abstract class AbstractTestWithTestDir {
    protected final Path testDir = Path.of("target/test")
        .resolve(getClass().getSimpleName());

    @BeforeEach
    public void setUp() throws Exception {
        if (testDir.toFile().exists()) {
            // github stumbled: https://github.com/DANS-KNAW/dans-layer-store-lib/actions/runs/8705753485/job/23876831089?pr=7#step:4:106
            FileUtils.deleteDirectory(testDir.toFile());
        }
    }
}
