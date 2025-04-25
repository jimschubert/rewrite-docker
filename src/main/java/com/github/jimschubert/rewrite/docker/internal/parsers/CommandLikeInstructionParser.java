/*
 * Copyright (c) 2025 Jim Schubert
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jimschubert.rewrite.docker.internal.parsers;

import com.github.jimschubert.rewrite.docker.internal.ParserState;
import com.github.jimschubert.rewrite.docker.internal.StringWithPadding;
import com.github.jimschubert.rewrite.docker.tree.Form;
import com.github.jimschubert.rewrite.docker.tree.Space;
import lombok.Getter;
import lombok.Value;

abstract class CommandLikeInstructionParser implements InstructionParser {
    @Value
    @Getter
    static class Elements {
        String content;
        Form form;
        Space execFormPrefix;
        Space execFormSuffix;
    }

    protected Elements parseElements(String content, ParserState state) {
        Form form = Form.SHELL;
        Space execFormPrefix = Space.EMPTY;
        Space execFormSuffix = Space.EMPTY;
        if (content.trim().startsWith("[")) {
            StringWithPadding stringWithPadding = StringWithPadding.of(content);
            content = stringWithPadding.content();
            execFormPrefix = stringWithPadding.prefix();
            content = content.substring(1, content.length() - 1);
            form = Form.EXEC;

            execFormSuffix = state.rightPadding();
        }
        return new Elements(content, form, execFormPrefix, execFormSuffix);
    }
}
