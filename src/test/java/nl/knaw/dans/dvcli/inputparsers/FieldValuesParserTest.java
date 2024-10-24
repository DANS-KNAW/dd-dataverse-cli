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

import nl.knaw.dans.lib.dataverse.CompoundFieldBuilder;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldValuesParserTest {

    @Test
    public void parse_should_create_one_single_value_field() {
        var values = List.of("field1=value1");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new PrimitiveSingleValueField("field1", "value1"));

    }

    @Test
    public void parse_should_create_one_multivalue_field_if_name_ends_with_asterisk() {
        var values = List.of("field1*=value1");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new PrimitiveMultiValueField("field1", List.of("value1")));
    }

    @Test
    public void parse_should_create_two_single_value_fields() {
        var values = List.of("field1=value1", "field2=value2");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new PrimitiveSingleValueField("field1", "value1"),
                             new PrimitiveSingleValueField("field2", "value2"));
    }

    @Test
    public void parse_should_create_one_multivalue_field_if_name_contains_dot() {
        var values = List.of("parent.child=value1");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new CompoundFieldBuilder("parent", false)
                .addSubfield("child", "value1")
                .build());
    }

    @Test
    public void parse_should_create_one_multivalue_field_if_name_contains_dot_and_parent_name_ends_with_asterisk() {
        var values = List.of("parent*.child=value1");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new CompoundFieldBuilder("parent", true)
                .addSubfield("child", "value1")
                .build());
    }

    @Test
    public void parse_should_create_one_multivalue_field_if_name_contains_dot_and_parent_name_ends_with_asterisk_and_multiple_subfields() {
        var values = List.of("parent*.child1=value1", "parent*.child2=value2");
        assertThat(new FieldValuesParser(values).parse())
            .containsExactly(new CompoundFieldBuilder("parent", true)
                .addSubfield("child1", "value1")
                .addSubfield("child2", "value2")
                .build());
    }

}
