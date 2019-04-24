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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class FeatureGraphInvariantTests {

  private FeatureGraph featureGraph;

  @Before
  public void setup() {
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int a = 5;",
            "  }",
            "}");
    featureGraph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());
  }

  @Test
  public void featureGraph_tokensHaveSingleRelatedAstNode() {
    assertThat(
            featureGraph.tokens().stream()
                .map(
                    node ->
                        featureGraph.predecessors(node, EdgeType.ASSOCIATED_TOKEN).stream().count())
                .anyMatch(count -> count != 1))
        .isFalse();
  }

  @Test
  public void featureGraph_singleStartTokenIsFirstToken() {
    ImmutableList<FeatureNode> nodes =
        featureGraph.tokens().stream()
            .filter(node -> featureGraph.predecessors(node, EdgeType.NEXT_TOKEN).isEmpty())
            .collect(toImmutableList());
    assertThat(Iterables.getOnlyElement(nodes)).isEqualTo(featureGraph.getFirstToken());
  }

  @Test
  public void featureGraph_tokensHaveZeroOrOneNextToken() {
    Map<Integer, Long> counts =
        featureGraph.tokens().stream()
            .map(node -> featureGraph.successors(node, EdgeType.NEXT_TOKEN).size())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    assertThat(counts.keySet()).containsExactly(0, 1);
  }

  @Test
  public void featureGraph_nextTokenFormsASequence() {
    Optional<FeatureNode> token =
        featureGraph.tokens().stream()
            .filter(node -> featureGraph.predecessors(node, EdgeType.NEXT_TOKEN).isEmpty())
            .findAny();
    ImmutableList.Builder<FeatureNode> tokensBuilder = ImmutableList.builder();
    while (token.isPresent()) {
      tokensBuilder.add(token.get());
      token = featureGraph.successors(token.get(), EdgeType.NEXT_TOKEN).stream().findFirst();
    }
    ImmutableList<FeatureNode> tokens = tokensBuilder.build();

    assertThat(featureGraph.tokens()).containsAllIn(tokens);
  }

  @Test
  public void featureGraph_singleAstNodeWithNoPredecessorsIsAstRoot() {
    ImmutableList<FeatureNode> nodes =
        featureGraph.astNodes().stream()
            .filter(node -> featureGraph.predecessors(node, EdgeType.AST_CHILD).isEmpty())
            .collect(toImmutableList());
    assertThat(Iterables.getOnlyElement(nodes)).isEqualTo(featureGraph.getAstRoot());
  }

  @Test
  public void featureGraph_astChild_isAcyclic() {
    assertThat(FeatureGraphChecks.isAcyclic(featureGraph, EdgeType.AST_CHILD)).isTrue();
  }

  @Test
  public void featureGraph_nextToken_isAcyclic() {
    assertThat(FeatureGraphChecks.isAcyclic(featureGraph, EdgeType.NEXT_TOKEN)).isTrue();
  }

  @Test
  public void featureGraph_astToken_neverHasAstChildEdge() {
    ImmutableList<FeatureNode> nodes =
        featureGraph.tokens().stream()
            .filter(node -> !featureGraph.predecessors(node, EdgeType.AST_CHILD).isEmpty())
            .collect(toImmutableList());
    assertThat(nodes).isEmpty();
  }
}
