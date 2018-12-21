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
import uk.ac.cam.acr31.features.javac.testing.Visualizer;

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
}
