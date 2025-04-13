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
package com.github.jimschubert.rewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static com.github.jimschubert.rewrite.docker.Assertions.dockerfile;

class NameAllStagesTest implements RewriteTest {
    @Test
    void nameAllStages() {
        rewriteRun(
                spec -> spec.recipe(new NameAllStages()).expectedCyclesThatMakeChanges(2),
                dockerfile(
                        """
                        # Stage 1: Build dependencies
                        FROM ubuntu:20.04
                        RUN apt-get update && apt-get install -y build-essential
                        RUN echo "Stage 1 complete" > /stage1.txt
                        
                        # Stage 2: Build application
                        FROM ubuntu:20.04
                        COPY --from=0 /stage1.txt /stage2.txt
                        RUN echo "Stage 2 complete" > /stage2_complete.txt
                        
                        # Stage 3: Run tests
                        FROM ubuntu:20.04
                        COPY --from=1 /stage2_complete.txt /stage3.txt
                        RUN echo "Stage 3 complete" > /stage3_complete.txt
                        
                        # Final Stage: Create runtime image
                        FROM ubuntu:20.04
                        COPY --from=2 /stage3_complete.txt /final_stage.txt
                        CMD ["cat", "/final_stage.txt"]
                        """,
                        """
                        # Stage 1: Build dependencies
                        FROM ubuntu:20.04 AS stage0
                        RUN apt-get update && apt-get install -y build-essential
                        RUN echo "Stage 1 complete" > /stage1.txt
                        
                        # Stage 2: Build application
                        FROM ubuntu:20.04 AS stage1
                        COPY --from=stage0 /stage1.txt /stage2.txt
                        RUN echo "Stage 2 complete" > /stage2_complete.txt
                        
                        # Stage 3: Run tests
                        FROM ubuntu:20.04 AS stage2
                        COPY --from=stage1 /stage2_complete.txt /stage3.txt
                        RUN echo "Stage 3 complete" > /stage3_complete.txt
                        
                        # Final Stage: Create runtime image
                        FROM ubuntu:20.04
                        COPY --from=stage2 /stage3_complete.txt /final_stage.txt
                        CMD ["cat", "/final_stage.txt"]
                        """)
        );
    }
}
