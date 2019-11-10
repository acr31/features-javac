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
import static uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;

import com.google.common.collect.Iterables;
import com.google.common.truth.Correspondence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class TokenAssociationTest {

  @Test
  public void tokenizer_collectsBasicTokens() {
    // ARRANGE
    TestCompilation compilation = TestCompilation.compile("Test.java", "public class Test {}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.tokens())
        .comparingElementsUsing(
            new Correspondence<FeatureNode, String>() {
              @Override
              public boolean compare(FeatureNode actual, String expected) {
                return actual.getContents().equals(expected);
              }

              @Override
              public String toString() {
                return "content equals";
              }
            })
        .containsExactly("PUBLIC", "CLASS", "Test", "LBRACE", "RBRACE");
  }

  @Test
  public void linkAstToTokens_associatesTokensWithNodes() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {}",
            "}");
    SourceSpan identifierSpan = compilation.sourceSpan("args");
    SourceSpan variableNodeSpan = compilation.sourceSpan("String[] args");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode variableNode =
        FeatureGraphChecks.findNode(graph, variableNodeSpan, "VARIABLE", "NAME");
    FeatureNode identifierToken =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                graph, identifierSpan, FeatureNode.NodeType.IDENTIFIER_TOKEN));
    assertThat(graph.edges(variableNode, identifierToken)).isNotEmpty();
  }

  @Test
  public void tokenAssociated_withNewClassTree() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java",
            "public class Test {", //
            "  static Test t = new Test();",
            "}");
    SourceSpan newClass = compilation.sourceSpan("new Test()");
    SourceSpan identifier = compilation.sourceSpan("Test", "();");

    // ACT
    FeatureGraph featureGraph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode newClassNode =
        FeatureGraphChecks.findNode(featureGraph, newClass, "NEW_CLASS", "IDENTIFIER", "NAME");
    FeatureNode identifierToken =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                featureGraph, identifier, FeatureNode.NodeType.IDENTIFIER_TOKEN));
    assertThat(featureGraph.edges(newClassNode, identifierToken)).isNotEmpty();
  }

  @Test
  public void tokenAssociated_withDeprecatedEnum() {
    // This odd case causes javac to create an NewClassTree with Kind ENUM that has a span that
    // swallows the BAR. If you drop the Deprecated it doesn't happen.
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "enum Foo {",
            "  @Deprecated",
            "  BAR() {}",
            "}");
    SourceSpan variableNodeSpan = compilation.sourceSpan("@Deprecated\n  BAR() {}");
    SourceSpan identifierSpan = compilation.sourceSpan("BAR");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode variableNode =
        FeatureGraphChecks.findNode(graph, variableNodeSpan, "VARIABLE", "NAME");
    FeatureNode identifierToken =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                graph, identifierSpan, FeatureNode.NodeType.IDENTIFIER_TOKEN));
    assertThat(graph.edges(variableNode, identifierToken)).isNotEmpty();
  }

  @Test
  public void tokanAssociated_toArrayAfterVariableUsage() {
    // When the array type is after the identifier then the ArrayTypeTree dominates the identifier
    // token causing it to be misassociated.
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void method() {",
            "    int a[] = new int[3];",
            "  }",
            "}");
    SourceSpan variableNodeSpan = compilation.sourceSpan("int a[] = new int[3];");
    SourceSpan identifierSpan = compilation.sourceSpan("a", "[]");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode variableNode =
        FeatureGraphChecks.findNode(graph, variableNodeSpan, "VARIABLE", "NAME");
    FeatureNode identifierToken =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                graph, identifierSpan, FeatureNode.NodeType.IDENTIFIER_TOKEN));
    assertThat(graph.edges(variableNode, identifierToken)).isNotEmpty();
  }

  @Test
  public void tokenAssociated_toSimultaneousInitialisationOfArrays() {
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  int arr[] = { 0 }, arr2[] = { 0 };",
            "}");
    SourceSpan variableNode1Span = compilation.sourceSpan("int arr[] = { 0 },");
    SourceSpan identifier1Span = compilation.sourceSpan("arr", "[]");
    SourceSpan variableNode2Span = compilation.sourceSpan("int arr[] = { 0 }, arr2[] = { 0 };");
    SourceSpan identifier2Span = compilation.sourceSpan("arr2", "[]");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    FeatureNode variableNode1 =
        FeatureGraphChecks.findNode(graph, variableNode1Span, "VARIABLE", "NAME");
    FeatureNode identifier1 =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                graph, identifier1Span, FeatureNode.NodeType.IDENTIFIER_TOKEN));
    FeatureNode variableNode2 =
        FeatureGraphChecks.findNode(graph, variableNode2Span, "VARIABLE", "NAME");
    FeatureNode identifier2 =
        Iterables.getOnlyElement(
            FeatureGraphChecks.findNodes(
                graph, identifier2Span, FeatureNode.NodeType.IDENTIFIER_TOKEN));

    assertThat(graph.edges(variableNode1, identifier1)).isNotEmpty();
    assertThat(graph.edges(variableNode2, identifier2)).isNotEmpty();
  }

  @Test
  public void tokenAssociated_literalTokenWithStringLiteralAstNode() {
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
    FeatureNode stringLiteral =
        Iterables.getOnlyElement(graph.predecessors(token, EdgeType.ASSOCIATED_TOKEN));
    assertThat(stringLiteral.getContents()).isEqualTo("STRING_LITERAL");
  }
}
