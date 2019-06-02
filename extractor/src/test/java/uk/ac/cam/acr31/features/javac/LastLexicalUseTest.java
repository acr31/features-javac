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
public class LastLexicalUseTest {

  @Test
  public void lastLexicalUse_chainsInOrder() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int x = 0;",
            "    if (x > 4) {",
            "      x++;",
            "    }",
            "    else {",
            "      x += 1;",
            "    }",
            "  }",
            "}");
    SourceSpan firstUse = compilation.sourceSpan("x", " = 0;");
    SourceSpan secondUse = compilation.sourceSpan("x", " > 4");
    SourceSpan thirdUse = compilation.sourceSpan("x", "++");
    SourceSpan fourthUse = compilation.sourceSpan("x", " += 1");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.LAST_LEXICAL_USE))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(graph, firstUse, secondUse, EdgeType.LAST_LEXICAL_USE),
            FeatureGraphChecks.edgeBetween(graph, secondUse, thirdUse, EdgeType.LAST_LEXICAL_USE),
            FeatureGraphChecks.edgeBetween(graph, thirdUse, fourthUse, EdgeType.LAST_LEXICAL_USE));
  }

  @Test
  public void lastLexicalUse_followsInnerClass() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void test() {",
            "    int a = 0;",
            "    Object o = new Object() {",
            "       int b = a;",
            "    };",
            "  }",
            "}");
    SourceSpan firstA = compilation.sourceSpan("a", " = 0;");
    SourceSpan secondA = compilation.sourceSpan("a", ";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.LAST_LEXICAL_USE))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(graph, firstA, secondA, EdgeType.LAST_LEXICAL_USE));
  }

  @Test
  public void lastLexicalUse_doesntCrash_withImplicitArguments() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  Test(int arg0) {}",
            "  void test() {",
            "    int a = 0;",
            "    Test o = new Test(4) {",
            "       int b = a;",
            "    };",
            "  }",
            "}");
    SourceSpan firstA = compilation.sourceSpan("a", " = 0;");
    SourceSpan secondA = compilation.sourceSpan("a", ";");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.LAST_LEXICAL_USE))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(graph, firstA, secondA, EdgeType.LAST_LEXICAL_USE));
  }

  @Test
  public void lastLexicalUse_createsEdge_ThroughSuperAndThis() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "class Super {",
            "   void method(int superParam) {}",
            "}",
            "public class Test extends Super {",
            "  void test() {",
            "    int a = 0;",
            "    super.method(a);",
            "    this.method(a);",
            "  }",
            "}");
    SourceSpan firstA = compilation.sourceSpan("a", " = 0;");
    SourceSpan secondA = compilation.sourceSpan("super.method(", "a", ");");
    SourceSpan thirdA = compilation.sourceSpan("this.method(", "a", ");");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.LAST_LEXICAL_USE))
        .containsExactly(
            FeatureGraphChecks.edgeBetween(graph, firstA, secondA, EdgeType.LAST_LEXICAL_USE),
            FeatureGraphChecks.edgeBetween(graph, secondA, thirdA, EdgeType.LAST_LEXICAL_USE));
  }
}
