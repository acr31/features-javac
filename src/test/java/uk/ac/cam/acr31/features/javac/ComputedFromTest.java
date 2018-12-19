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
public class ComputedFromTest {

  @Test
  public void computedFrom_addsEdgesToLocalVariables_inAssignment() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int a = 0;",
            "    int b = 0;",
            "    int c;",
            "    c = a + b;",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.COMPUTED_FROM))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(
                graph, "ASSIGNMENT,IDENTIFIER,c", "PLUS,IDENTIFIER,a", EdgeType.COMPUTED_FROM),
            FeatureGraphChecks.edgeBetween(
                graph, "ASSIGNMENT,IDENTIFIER,c", "PLUS,IDENTIFIER,b", EdgeType.COMPUTED_FROM));
  }

  @Test
  public void computedFrom_addsEdgesToFields() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static int a = 0;",
            "  public static void main(String[] args) {",
            "    int b = 0;",
            "    int c;",
            "    c = a + b;",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.COMPUTED_FROM))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(
                graph, "ASSIGNMENT,IDENTIFIER,c", "PLUS,IDENTIFIER,a", EdgeType.COMPUTED_FROM),
            FeatureGraphChecks.edgeBetween(
                graph, "ASSIGNMENT,IDENTIFIER,c", "PLUS,IDENTIFIER,b", EdgeType.COMPUTED_FROM));
  }
}
