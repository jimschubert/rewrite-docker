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
package com.github.jimschubert.rewrite.docker.internal;

import com.github.jimschubert.rewrite.docker.internal.parsers.*;

import java.util.List;

/**
 * Registry for instruction parsers.
 * This class is responsible for managing and providing access to different instruction parsers.
 */
public class InstructionParserRegistry implements ParserRegistry {

    private final List<InstructionParser> parsers = List.of(
            new FromInstructionParser(),
            new RunInstructionParser(),
            new OnBuildInstructionParser(this),
            new AddInstructionParser(),
            new CmdInstructionParser(),
            new CommentParser(),
            new ArgInstructionParser(),
            new LabelInstructionParser(),
            new StopSignalInstructionParser(),
            new ExposeInstructionParser(),
            new MaintainerInstructionParser(),
            new HealthcheckInstructionParser(),
            new EnvInstructionParser(),
            new CopyInstructionParser(),
            new EntrypointInstructionParser(),
            new VolumeInstructionParser(),
            new WorkdirInstructionParser(),
            new ShellInstructionParser(),
            new UserInstructionParser(),
            new UnknownInstruction()
    );


    public InstructionParser getParserFor(String keyword) {
        return parsers.stream()
                .filter(p -> p.supports(keyword))
                .findFirst()
                .orElse(new UnknownInstruction());
    }
}
