/*
 * Copyright © 2018 Andrew Rice (acr31@cam.ac.uk)
 *
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

package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class GuardedByTest {

  @Test
  public void guardedBy_linksIfStatementBodyToCondition() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int x = 0;",
            "    int y = 0;",
            "    if (x>y) {",
            "      x++;",
            "    }",
            "    else {",
            "      y++;",
            "    }",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.GUARDED_BY))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(
                graph, "POSTFIX_INCREMENT,IDENTIFIER,x", "IF,PARENTHESIZED", EdgeType.GUARDED_BY));
    assertThat(graph.edges(EdgeType.GUARDED_BY_NEGATION))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(
                graph,
                "POSTFIX_INCREMENT,IDENTIFIER,y",
                "IF,PARENTHESIZED",
                EdgeType.GUARDED_BY_NEGATION));
  }
}
