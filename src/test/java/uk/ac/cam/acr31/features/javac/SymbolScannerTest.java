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

import com.google.common.collect.Iterables;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class SymbolScannerTest {

  @Test
  public void symbolScanner_attachesSymbolToClassDeclName() {
    // ARRANGE
    TestCompilation compilation = TestCompilation.compile("Test.java", "public class Test {}");

    SourceSpan clazz = compilation.sourceSpan("public class Test {}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Set<FeatureNode> classNodes = graph.findNode(clazz.start(), clazz.end());
    FeatureNode symbolNode = findSymbolNode(graph, classNodes);
    assertThat(symbolNode.getContents()).isEqualTo("Test");
  }

  @Test
  public void symbolScanner_attachesSymbolToMethodName() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static int method(double d) {",
            "    return 1;",
            "  }",
            "}");
    SourceSpan method = compilation.sourceSpan("static int method(double d) {\n    return 1;\n  }");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Set<FeatureNode> methodNodes = graph.findNode(method.start(), method.end());
    FeatureNode symbolNode = findSymbolNode(graph, methodNodes);
    assertThat(symbolNode.getContents()).isEqualTo("method(double)");
  }

  @Test
  public void symbolScanner_attachesSymbolToMethodInvocation() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  String a = String.valueOf(1);",
            "}");
    SourceSpan inv = compilation.sourceSpan("String.valueOf(1)");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Set<FeatureNode> invocationNode = graph.findNode(inv.start(), inv.end());
    FeatureNode symbolNode = findSymbolNode(graph, invocationNode);
    assertThat(symbolNode.getContents()).isEqualTo("valueOf(int)");
  }

  @Test
  public void symbolScanner_attachesSymbolToFieldReference() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  String a = null;",
            "  String b = a;",
            "}");
    SourceSpan inv = compilation.sourceSpan("a", ";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Set<FeatureNode> invocationNode = graph.findNode(inv.start(), inv.end());
    FeatureNode symbolNode = findSymbolNode(graph, invocationNode);
    assertThat(symbolNode.getContents()).isEqualTo("a");
  }

  @Test
  public void symbolScanner_attachesSymbolToVariableName() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  String a = null;",
            "}");
    SourceSpan inv = compilation.sourceSpan("a", " = ");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Set<FeatureNode> invocationNode = graph.findNode(inv.start(), inv.end());
    FeatureNode symbolNode = findSymbolNode(graph, invocationNode);
    assertThat(symbolNode.getContents()).isEqualTo("a");
  }

  private FeatureNode findSymbolNode(FeatureGraph graph, Set<FeatureNode> methodNodes) {
    return Iterables.getOnlyElement(
        methodNodes
            .stream()
            .map(n -> graph.predecessors(n, EdgeType.ASSOCIATED_SYMBOL))
            .filter(p -> !p.isEmpty())
            .findAny()
            .orElseThrow());
  }
}
