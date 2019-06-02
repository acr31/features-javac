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

import com.google.common.base.Joiner;
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
    String classLines = "public class Test {}";
    TestCompilation compilation = TestCompilation.compile("Test.java", classLines);
    SourceSpan clazz = compilation.sourceSpan(classLines);

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, clazz);
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
    FeatureNode symbolNode = findSymbolNode(graph, method);
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
    FeatureNode symbolNode = findSymbolNode(graph, inv);
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
    FeatureNode symbolNode = findSymbolNode(graph, inv);
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
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    assertThat(symbolNode.getContents()).isEqualTo("a");
  }

  @Test
  public void symbolScanner_sharesSymbolNode_betwenVarDeclAndUseInBlock() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void test() {",
            "    int a = 0;",
            "    a = 1;",
            "  }",
            "}");
    SourceSpan decl = compilation.sourceSpan("int ", "a", " = ");
    SourceSpan use = compilation.sourceSpan("a", " = 1");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode declSymbol = findSymbolNode(graph, decl);
    FeatureNode useSymbol = findSymbolNode(graph, use);
    assertThat(declSymbol).isEqualTo(useSymbol);
  }

  @Test
  public void symbolScanner_sharesSymbolNode_betwenClassDeclAndUse() {
    // ARRANGE
    String[] classLines = {
      "public class Test {", //
      "  void test() {",
      "    Test t;",
      "  }",
      "}"
    };
    TestCompilation compilation = TestCompilation.compile("Test.java", classLines);
    SourceSpan decl = compilation.sourceSpan(Joiner.on("\n").join(classLines));
    SourceSpan use = compilation.sourceSpan("Test", " t;");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode declSymbol = findSymbolNode(graph, decl);
    FeatureNode useSymbol = findSymbolNode(graph, use);
    assertThat(declSymbol).isEqualTo(useSymbol);
  }

  @Test
  public void symbolScanner_sharesSymbolNode_betwenMethodDeclAndUse() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  void foo() {}",
            "  void test() {",
            "    foo();",
            "  }",
            "}");
    SourceSpan decl = compilation.sourceSpan("void foo() {}");
    SourceSpan use = compilation.sourceSpan("foo()", ";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode declSymbol = findSymbolNode(graph, decl);
    FeatureNode useSymbol = findSymbolNode(graph, use);
    assertThat(declSymbol).isEqualTo(useSymbol);
  }

  @Test
  public void symbolScanner_differentSymbolNode_whenNameSameOnDifferentVars() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  int x = 1;",
            "  void test() {",
            "    int x = 2;",
            "  }",
            "}");
    SourceSpan declField = compilation.sourceSpan("int ", "x", " = 1;");
    SourceSpan declLocal = compilation.sourceSpan("int ", "x", " = 2;");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode declSymbol = findSymbolNode(graph, declField);
    FeatureNode useSymbol = findSymbolNode(graph, declLocal);
    assertThat(declSymbol).isNotEqualTo(useSymbol);
  }

  private FeatureNode findSymbolNode(FeatureGraph graph, SourceSpan sourceSpan) {
    Set<FeatureNode> nodes = graph.findNode(sourceSpan.start(), sourceSpan.end());
    return Iterables.getOnlyElement(
        nodes.stream()
            .map(n -> graph.predecessors(n, EdgeType.ASSOCIATED_SYMBOL))
            .filter(p -> !p.isEmpty())
            .findAny()
            .orElseThrow());
  }
}
