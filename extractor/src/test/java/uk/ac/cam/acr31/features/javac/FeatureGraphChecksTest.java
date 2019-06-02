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
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;

@RunWith(JUnit4.class)
public class FeatureGraphChecksTest {

  private static final EndPosTable FAKE_END_POS_TABLE =
      new EndPosTable() {
        @Override
        public int getEndPos(JCTree tree) {
          return 0;
        }

        @Override
        public void storeEnd(JCTree tree, int endpos) {}

        @Override
        public int replaceTree(JCTree oldtree, JCTree newtree) {
          return 0;
        }
      };

  private static final LineMap FAKE_LINE_MAP =
      new LineMap() {
        @Override
        public long getStartPosition(long line) {
          return 0;
        }

        @Override
        public long getPosition(long line, long column) {
          return 0;
        }

        @Override
        public long getLineNumber(long pos) {
          return 0;
        }

        @Override
        public long getColumnNumber(long pos) {
          return 0;
        }
      };

  private FeatureGraph graph;
  private ImmutableList<FeatureNode> nodes;

  @Before
  public void setup() {
    graph = new FeatureGraph("Test.java", FAKE_END_POS_TABLE, FAKE_LINE_MAP);
    nodes =
        IntStream.range(0, 8)
            .mapToObj(i -> graph.createFeatureNode(NodeType.AST_ELEMENT, "Node", 0, 0))
            .collect(toImmutableList());
  }

  @Test
  public void isAcyclic_returnsTrueWithTree() {
    // ARRANGE
    FeatureGraph g = graph;
    g.addEdge(nodes.get(0), nodes.get(1), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(0), nodes.get(2), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(1), nodes.get(3), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(3), nodes.get(4), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(4), nodes.get(5), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(4), nodes.get(6), EdgeType.AST_CHILD);
    g.addEdge(nodes.get(4), nodes.get(7), EdgeType.AST_CHILD);

    // ACT
    boolean acylic = FeatureGraphChecks.isAcyclic(g, EdgeType.AST_CHILD);

    // ASSERT
    assertThat(acylic).isTrue();
  }

  @Test
  public void isAcyclic_returnsTrueWithDag() {
    // ARRANGE
    graph.addEdge(nodes.get(0), nodes.get(1), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(0), nodes.get(1), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(1), nodes.get(2), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(1), nodes.get(3), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(2), nodes.get(4), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(3), nodes.get(4), EdgeType.AST_CHILD);

    // ACT
    boolean acylic = FeatureGraphChecks.isAcyclic(graph, EdgeType.AST_CHILD);

    // ASSERT
    assertThat(acylic).isTrue();
  }

  @Test
  public void isAcyclic_returnsFalseWithLoop() {
    // ARRANGE
    graph.addEdge(nodes.get(0), nodes.get(1), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(1), nodes.get(2), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(2), nodes.get(3), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(3), nodes.get(0), EdgeType.AST_CHILD);

    // ACT
    boolean acylic = FeatureGraphChecks.isAcyclic(graph, EdgeType.AST_CHILD);

    // ASSERT
    assertThat(acylic).isFalse();
  }

  @Test
  public void isAcyclic_returnsFalseWithLoopWithLeaves() {
    // ARRANGE
    graph.addEdge(nodes.get(0), nodes.get(1), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(1), nodes.get(2), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(2), nodes.get(3), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(3), nodes.get(0), EdgeType.AST_CHILD);

    graph.addEdge(nodes.get(0), nodes.get(4), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(1), nodes.get(5), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(2), nodes.get(6), EdgeType.AST_CHILD);
    graph.addEdge(nodes.get(3), nodes.get(7), EdgeType.AST_CHILD);

    // ACT
    boolean acylic = FeatureGraphChecks.isAcyclic(graph, EdgeType.AST_CHILD);

    // ASSERT
    assertThat(acylic).isFalse();
  }
}
