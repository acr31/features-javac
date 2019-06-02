/*
 * Copyright © 2018 The Authors (see NOTICE file)
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
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class FormalArgTest {

  @Test
  public void formalArg_createsEdge_betweenIdentifierAndFormalParameter() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static void b(int formalParameter) {}",
            "  public static void main(String[] args) {",
            "    int a = 4;",
            "    b(a);",
            "  }",
            "}");
    SourceSpan formalParameter = compilation.sourceSpan("formalParameter");
    SourceSpan a = compilation.sourceSpan("a", ");");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.FORMAL_ARG_NAME))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(graph, a, formalParameter, EdgeType.FORMAL_ARG_NAME));
  }

  @Test
  public void formalArg_createsEdge_betweenConstructor() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  Test(int arg0) {}",
            "  void test() {",
            "    int a = 0;",
            "    Test o = new Test(a);",
            "  }",
            "}");
    SourceSpan arg0 = compilation.sourceSpan("arg0");
    SourceSpan a = compilation.sourceSpan("a", ")");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.FORMAL_ARG_NAME))
        .containsExactly(FeatureGraphChecks.edgeBetween(graph, a, arg0, EdgeType.FORMAL_ARG_NAME));
  }
}
