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

import com.google.common.truth.Correspondence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
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
    SourceSpan args = compilation.sourceSpan("args");
    SourceSpan variable = compilation.sourceSpan("String[] args");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(FeatureGraphChecks.edgeBetween(graph, variable, args, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
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
    assertThat(
            FeatureGraphChecks.edgeBetween(
                featureGraph, newClass, identifier, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
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
    SourceSpan variableNode = compilation.sourceSpan("@Deprecated\n  BAR() {}");
    SourceSpan identifier = compilation.sourceSpan("BAR");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
            FeatureGraphChecks.edgeBetween(
                graph, variableNode, identifier, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
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
    SourceSpan variableNode = compilation.sourceSpan("int a[] = new int[3];");
    SourceSpan identifier = compilation.sourceSpan("a", "[]");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
            FeatureGraphChecks.edgeBetween(
                graph, variableNode, identifier, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
  }

  @Test
  public void tokenAssociated_toSimultaneousInitialisationOfArrays() {
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  int arr[] = { 0 }, arr2[] = { 0 };",
            "}");
    SourceSpan variableNode1 = compilation.sourceSpan("int arr[] = { 0 },");
    SourceSpan identifier1 = compilation.sourceSpan("arr", "[]");
    SourceSpan variableNode2 = compilation.sourceSpan("int arr[] = { 0 }, arr2[] = { 0 };");
    SourceSpan identifier2 = compilation.sourceSpan("arr2", "[]");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
            FeatureGraphChecks.edgeBetween(
                graph, variableNode1, identifier1, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
    assertThat(
            FeatureGraphChecks.edgeBetween(
                graph, variableNode2, identifier2, EdgeType.ASSOCIATED_TOKEN))
        .isNotNull();
  }
}
