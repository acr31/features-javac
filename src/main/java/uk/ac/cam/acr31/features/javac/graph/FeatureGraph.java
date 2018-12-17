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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.sun.source.tree.Tree;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class FeatureGraph {

  private final MutableNetwork<FeatureNode, FeatureEdge> graph;
  private final Map<Tree, FeatureNode> nodeMap;

  public FeatureGraph() {
    this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
    this.nodeMap = new HashMap<>();
  }

  public FeatureNode getFeatureNode(Tree tree) {
    return nodeMap.get(tree);
  }

  public FeatureNode createFeatureNode(NodeType nodeType, String contents, Tree tree) {
    FeatureNode result = FeatureNodes.create(nodeType, contents);
    nodeMap.put(tree, result);
    return result;
  }

  public FeatureNode createFeatureNode(NodeType nodeType, String contents) {
    return FeatureNodes.create(nodeType, contents);
  }

  public ImmutableSet<FeatureNode> nodes() {
    return ImmutableSet.copyOf(graph.nodes());
  }

  public ImmutableSet<FeatureNode> nodes(NodeType... nodeTypes) {
    ImmutableList<NodeType> nodes = ImmutableList.copyOf(nodeTypes);
    return graph
        .nodes()
        .stream()
        .filter(n -> nodes.contains(n.getType()))
        .collect(toImmutableSet());
  }

  public Set<FeatureEdge> edges() {
    return graph.edges();
  }

  public Set<FeatureNode> successors(FeatureNode node) {
    return graph.successors(node);
  }

  public ImmutableSet<FeatureNode> successors(FeatureNode node, NodeType nodeType) {
    return graph
        .successors(node)
        .stream()
        .filter(n -> n.getType().equals(nodeType))
        .collect(toImmutableSet());
  }

  public ImmutableSet<FeatureNode> successors(FeatureNode node, EdgeType edgeType) {
    return graph
        .successors(node)
        .stream()
        .filter(
            n ->
                graph
                    .edgesConnecting(node, n)
                    .stream()
                    .anyMatch(edge -> edge.getType().equals(edgeType)))
        .collect(toImmutableSet());
  }

  public ImmutableSet<FeatureNode> predecessors(FeatureNode node, NodeType nodeType) {
    return graph
        .predecessors(node)
        .stream()
        .filter(n -> n.getType().equals(nodeType))
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
}
