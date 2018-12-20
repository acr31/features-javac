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

package uk.ac.cam.acr31.features.javac.graph;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.Graph;

public class FeatureGraph {

  private final String sourceFileName;
  private final MutableNetwork<FeatureNode, FeatureEdge> graph;
  private final Map<Tree, FeatureNode> nodeMap;
  private int nodeIdCounter = 0;

  public FeatureGraph(String sourceFileName) {
    this.sourceFileName = sourceFileName;
    this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
    this.nodeMap = new HashMap<>();
  }

  public String getSourceFileName() {
    return sourceFileName;
  }

  public FeatureNode getFeatureNode(Tree tree) {
    return nodeMap.get(tree);
  }

  public FeatureNode createFeatureNode(
      NodeType nodeType, String contents, Tree tree, int startPosition, int endPosition) {
    FeatureNode result = createFeatureNode(nodeType, contents, startPosition, endPosition);
    nodeMap.put(tree, result);
    return result;
  }

  public FeatureNode createFeatureNode(
      NodeType nodeType, String contents, int startPosition, int endPosition) {
//    Preconditions.checkArgument(startPosition != -1);
//    Preconditions.checkArgument(endPosition != -1);
    return FeatureNode.newBuilder()
        .setId(nodeIdCounter++)
        .setType(nodeType)
        .setContents(contents)
        .setStartPosition(startPosition)
        .setEndPosition(endPosition)
        .build();
  }

  public Set<FeatureNode> nodes() {
    // returns an unmodifiable set
    return graph.nodes();
  }

  private Set<FeatureNode> nodes(NodeType... nodeTypes) {
    ImmutableList<NodeType> nodes = ImmutableList.copyOf(nodeTypes);
    return graph
        .nodes()
        .stream()
        .filter(n -> nodes.contains(n.getType()))
        .collect(toImmutableSet());
  }

  public FeatureNode root() {
    return Iterables.getOnlyElement(nodes(NodeType.AST_ROOT));
  }

  public Set<FeatureNode> tokens() {
    return nodes(NodeType.TOKEN, NodeType.IDENTIFIER_TOKEN);
  }

  public Set<FeatureNode> astNodes() {
    return nodes(NodeType.AST_ELEMENT);
  }

  public Set<FeatureNode> comments() {
    return nodes(NodeType.COMMENT_BLOCK, NodeType.COMMENT_JAVADOC, NodeType.COMMENT_LINE);
  }

  public Set<FeatureEdge> edges() {
    // returns an unmodifiable set
    return graph.edges();
  }

  public Set<FeatureEdge> edges(EdgeType edgeType) {
    return graph
        .edges()
        .stream()
        .filter(e -> e.getType().equals(edgeType))
        .collect(toImmutableSet());
  }

  public Set<FeatureEdge> edges(FeatureNode source, FeatureNode destination) {
    // returns an unmodifiable set
    return graph.edgesConnecting(source, destination);
  }

  public Set<FeatureNode> successors(FeatureNode node) {
    // returns an unmodifiable set
    return graph.successors(node);
  }

  public Set<FeatureNode> successors(FeatureNode node, EdgeType... edgeTypes) {
    ImmutableList<EdgeType> edgeTypeList = ImmutableList.copyOf(edgeTypes);
    return graph
        .successors(node)
        .stream()
        .filter(
            n ->
                graph
                    .edgesConnecting(node, n)
                    .stream()
                    .anyMatch(e -> edgeTypeList.contains(e.getType())))
        .collect(toImmutableSet());
  }

  public Set<FeatureNode> reachableIdentifierNodes(FeatureNode node) {
    ImmutableSet.Builder<FeatureNode> result = ImmutableSet.builder();
    Deque<FeatureNode> work = new ArrayDeque<>();
    Set<FeatureNode> visited = new HashSet<>();
    work.add(node);
    while (!work.isEmpty()) {
      FeatureNode next = work.poll();
      for (FeatureNode succ : successors(next, EdgeType.AST_CHILD, EdgeType.ASSOCIATED_TOKEN)) {
        if (visited.contains(succ)) {
          continue;
        }
        visited.add(succ);
        if (succ.getType().equals(NodeType.IDENTIFIER_TOKEN)) {
          result.add(succ);
        } else {
          work.add(succ);
        }
      }
    }
    return result.build();
  }

  public Set<FeatureNode> predecessors(FeatureNode node, EdgeType... edgeTypes) {
    ImmutableList<EdgeType> edgeTypeList = ImmutableList.copyOf(edgeTypes);
    return graph
        .predecessors(node)
        .stream()
        .filter(
            n ->
                graph
                    .edgesConnecting(n, node)
                    .stream()
                    .anyMatch(e -> edgeTypeList.contains(e.getType())))
        .collect(toImmutableSet());
  }

  /** Remove all nodes with the specified type that have no successors. */
  public void pruneLeaves(NodeType nodeType) {
    while (pruneLeavesOnce(nodeType)) {
      // do nothing
    }
  }

  private boolean pruneLeavesOnce(NodeType nodeType) {
    ImmutableSet<FeatureNode> toRemove =
        graph
            .nodes()
            .stream()
            .filter(n -> n.getType().equals(nodeType))
            .filter(n -> graph.successors(n).isEmpty())
            .collect(toImmutableSet());
    toRemove.forEach(graph::removeNode);
    return !toRemove.isEmpty();
  }

  public void addEdge(FeatureNode source, FeatureNode dest, EdgeType type) {
    graph.addEdge(
        source,
        dest,
        FeatureEdge.newBuilder()
            .setSourceId(source.getId())
            .setDestinationId(dest.getId())
            .setType(type)
            .build());
  }

  public void removeEdge(FeatureEdge edge) {
    graph.removeEdge(edge);
  }

  public Graph toProtobuf() {
    return Graph.newBuilder()
        .setSourceFile(sourceFileName)
        .addAllNode(nodes())
        .addAllEdge(edges())
        .build();
  }
}
