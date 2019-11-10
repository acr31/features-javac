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
    SourceSpan clazz = compilation.sourceSpan("Test");

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
            "package foo.bar;",
            "public class Test {",
            "  static int method(double d) {",
            "    return 1;",
            "  }",
            "}");
    SourceSpan method = compilation.sourceSpan("method");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, method);
    assertThat(symbolNode.getContents()).isEqualTo("foo.bar.Test.method(double)");
  }

  @Test
  public void symbolScanner_attachesSymbolToMethodName_inDefaultPackage() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static int method(double d) {",
            "    return 1;",
            "  }",
            "}");
    SourceSpan method = compilation.sourceSpan("method");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, method);
    assertThat(symbolNode.getContents()).isEqualTo("Test.method(double)");
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
    SourceSpan inv = compilation.sourceSpan("valueOf");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    assertThat(symbolNode.getContents()).isEqualTo("java.lang.String.valueOf(int)");
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
    assertThat(symbolNode.getContents()).isEqualTo("Test.a");
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
    SourceSpan inv = compilation.sourceSpan("String ", "a", " = null;");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    assertThat(symbolNode.getContents()).isEqualTo("Test.a");
  }

  @Test
  public void symbolScanner_attachesSymbolToVariableName_inMethod() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void f() {",
            "    String a = null;",
            "  }",
            "}");
    SourceSpan inv = compilation.sourceSpan("String ", "a", " = null;");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    assertThat(symbolNode.getContents()).matches("\\QTest.f().a@\\E\\d+");
  }

  @Test
  public void symbolScanner_attachesDifferentNamesToVariables_withSameOwner() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void f() {",
            "    {",
            "      String a = \"\";",
            "    }",
            "    {",
            "      String a = null;",
            "    }",
            "  }",
            "}");
    SourceSpan inv = compilation.sourceSpan("String ", "a", " = null;");
    SourceSpan inv2 = compilation.sourceSpan("String ", "a", " = \"\";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    FeatureNode otherSymbolNode = findSymbolNode(graph, inv2);
    assertThat(symbolNode.getContents().equals(otherSymbolNode.getContents())).isFalse();
  }

  @Test
  public void symbolScanner_attachesDifferentNamesToVariables_withSameOwnerInStaticInitialiser() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static {",
            "    {",
            "      String a = \"\";",
            "    }",
            "    {",
            "      String a = null;",
            "    }",
            "  }",
            "}");
    SourceSpan inv = compilation.sourceSpan("String ", "a", " = null;");
    SourceSpan inv2 = compilation.sourceSpan("String ", "a", " = \"\";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    FeatureNode otherSymbolNode = findSymbolNode(graph, inv2);
    assertThat(symbolNode.getContents().equals(otherSymbolNode.getContents())).isFalse();
  }

  @Test
  public void symbolScanner_attachesDifferentNamesToClasses_withSameOwner() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void f() {",
            "    {",
            "      class A {} // 1",
            "    }",
            "    {",
            "      class A {} // 2",
            "    }",
            "  }",
            "}");
    SourceSpan inv = compilation.sourceSpan("class ", "A", " {} // 1");
    SourceSpan inv2 = compilation.sourceSpan("class ", "A", " {} // 2");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbolNode = findSymbolNode(graph, inv);
    FeatureNode otherSymbolNode = findSymbolNode(graph, inv2);
    assertThat(symbolNode.getContents().equals(otherSymbolNode.getContents())).isFalse();
  }

  @Test
  public void symbolScanner_attachesSameSymbol_toSameVariable() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void f() {",
            "    int x = 0;",
            "    {",
            "      x = 4;",
            "    }",
            "  }",
            "}");
    SourceSpan decl = compilation.sourceSpan("int ", "x", " = 0;");
    SourceSpan use = compilation.sourceSpan("x", " = 4;");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode declNode = findSymbolNode(graph, decl);
    FeatureNode useNode = findSymbolNode(graph, use);
    assertThat(declNode.equals(useNode)).isTrue();
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
    SourceSpan decl = compilation.sourceSpan("int ", "a", " = 0;");
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
    SourceSpan decl = compilation.sourceSpan("public class ", "Test", " {");
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
    SourceSpan decl = compilation.sourceSpan("void ", "foo", "() {}");
    SourceSpan use = compilation.sourceSpan("foo", "();");

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

  @Test
  public void symbolScanner_getsName_forImportedPackage() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "import java.util.List;", //
            "public class Test {}");
    SourceSpan pckge = compilation.sourceSpan("java.", "util", ".List");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode symbol = findSymbolNode(graph, pckge);
    assertThat(symbol.getContents()).isEqualTo("java.util");
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
