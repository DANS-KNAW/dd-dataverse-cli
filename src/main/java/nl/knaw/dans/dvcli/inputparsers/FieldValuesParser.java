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
package nl.knaw.dans.dvcli.inputparsers;

import lombok.AllArgsConstructor;
import nl.knaw.dans.lib.dataverse.CompoundFieldBuilder;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class FieldValuesParser {
    private final List<String> values;

    public List<MetadataField> parse() {
        Map<String, String> keyValues = new HashMap<>();

        for (var value : values) {
            String[] split = value.split("=", 2);
            keyValues.put(checkValidName(split[0]), split[1]);
        }

        Map<String, Map<String, String>> compoundFields = new HashMap<>();
        List<String> keysToRemove = new ArrayList<>();
        for (var key : keyValues.keySet()) {
            if (key.contains(".")) {
                String[] split = key.split("\\.", 2);
                String parent = split[0];
                String child = split[1];
                if (!compoundFields.containsKey(parent)) {
                    compoundFields.put(parent, new HashMap<>());
                }
                compoundFields.get(parent).put(child, keyValues.get(key));
                keysToRemove.add(key);
            }
        }
        for (var key : keysToRemove) {
            keyValues.remove(key);
        }

        List<MetadataField> result = new ArrayList<>();

        for (var key : keyValues.keySet()) {
            if (key.endsWith("*")) {
                result.add(new PrimitiveMultiValueField(key.substring(0, key.length() - 1), List.of(keyValues.get(key))));
            }
            else {
                result.add(new PrimitiveSingleValueField(key, keyValues.get(key)));
            }
        }

        for (var parent : compoundFields.keySet()) {
            Map<String, String> subfields = compoundFields.get(parent);
            if (parent.endsWith("*")) {
                var builder = new CompoundFieldBuilder(parent.substring(0, parent.length() - 1), true);
                for (var subfield : subfields.keySet()) {
                    builder.addSubfield(subfield, subfields.get(subfield));
                }
                result.add(builder.build());
            }
            else {
                var builder = new CompoundFieldBuilder(parent, false);
                for (var subfield : subfields.keySet()) {
                    builder.addSubfield(subfield, subfields.get(subfield));
                }
                result.add(builder.build());
            }
        }

        return result;
    }

    private String checkValidName(String name) {
        if (!name.matches("[a-zA-Z0-9]+\\*?(\\.[a-zA-Z0-9]+)?")) {
            throw new IllegalArgumentException("Invalid field name: " + name);
        }
        return name;
    }
}
