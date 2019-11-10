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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class AstScannerTest {

  @Test
  public void astScanner_escapesStringLiteral_emptyQuotes() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  String s = \"\";",
            "}");
    SourceSpan tokenSpan = compilation.sourceSpan("\"\"");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode token =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(graph, tokenSpan, FeatureNode.NodeType.TOKEN));
    assertThat(token.getContents()).isEqualTo("\"\"");
  }

  @Test
  public void astScanner_escapesStringLiteral_withQuoteInIt() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  String s = \"\\\"\";",
            "}");
    SourceSpan tokenSpan = compilation.sourceSpan("\"\\\"\"");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode token =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(graph, tokenSpan, FeatureNode.NodeType.TOKEN));
    assertThat(token.getContents()).isEqualTo("\"\\\"\"");
  }

  @Test
  public void astScanner_producesTree_whenAnonymousInnerClass() {
    // In this case javac says the identifier of the anonymous class and the extends clause of the
    // new class body are the same node. This would create a non-tree.
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  void test() {",
            "    Object o = new Object() {};",
            "  }",
            "}");
    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    ImmutableList<FeatureNode> nodesWithPredecessors =
        graph.astNodes().stream()
            .filter(
                n -> graph.predecessors(n, GraphProtos.FeatureEdge.EdgeType.AST_CHILD).size() != 1)
            .filter(n -> graph.getAstRoot() != n)
            .collect(toImmutableList());
    assertThat(nodesWithPredecessors).isEmpty();
  }
}
