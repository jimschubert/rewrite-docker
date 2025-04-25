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
import com.github.jimschubert.rewrite.docker.tree.Docker;

/**
 * Interface for parsing Dockerfile instructions.
 * This interface defines methods to determine if a specific instruction
 * is supported and to facilitate the parsing of that instruction.
 */
public interface InstructionParser {
    /**
     * Returns the name of the Dockerfile instruction that this parser supports.
     *
     * @return the name of the supported Dockerfile instruction.
     */
    String instructionName();

    /**
     * Determines if the provided keyword corresponds to a supported Dockerfile instruction.
     *
     * @param keyword the keyword to check for support.
     * @return true if the keyword is supported, false otherwise.
     */
    default boolean supports(String keyword) {
        return keyword.equalsIgnoreCase(instructionName());
    };

    /**
     * Parses a line of a Dockerfile and transforms it into a Docker instruction object.
     *
     * @param line  the line of the Dockerfile to be parsed.
     * @param state the current state of the parser which may influence how the line is interpreted.
     * @return a Docker instruction object representing the parsed line.
     */
    Docker.Instruction parse(String line, ParserState state);

    default String peekWord(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }

        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }

        int start = i;
        if (line.charAt(i) == '#') {
            i++;
        } else {
            while (i < line.length() && Character.isLetter(line.charAt(i))) {
                i++;
            }
        }

        return line.substring(start, i);
    }
}
