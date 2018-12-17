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
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FeatureGraph {

  private final MutableValueGraph<FeatureNode, EdgeType> graph;
  private final Map<Tree, FeatureNode> nodeMap;

  public FeatureGraph() {
    this.graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    this.nodeMap = new HashMap<>();
  }

  public FeatureNode getFeatureNode(Tree tree) {
    return nodeMap.get(tree);
  }

  public FeatureNode createFeatureNode(NodeType nodeType, String contents, Tree tree) {
    FeatureNode result = FeatureNode.create(nodeType, contents);
    nodeMap.put(tree, result);
    return result;
  }

  public FeatureNode createFeatureNode(NodeType nodeType, String contents) {
    return FeatureNode.create(nodeType, contents);
  }

  public void putEdgeValue(FeatureNode nodeU, FeatureNode nodeV, EdgeType value) {
    graph.putEdgeValue(nodeU, nodeV, value);
  }

  public void linkNodes(Collection<FeatureNode> nodes, EdgeType edgeType) {
    Iterator<FeatureNode> step = nodes.iterator();
    FeatureNode prev = step.next();
    while (step.hasNext()) {
      FeatureNode current = step.next();
      graph.putEdgeValue(prev, current, edgeType);
      prev = current;
    }
  }

  public ImmutableSet<FeatureNode> nodes(NodeType... nodeTypes) {
    ImmutableList<NodeType> nodes = ImmutableList.copyOf(nodeTypes);
    return graph
        .nodes()
        .stream()
        .filter(n -> nodes.contains(n.nodeType()))
        .collect(toImmutableSet());
  }

  public Set<FeatureNode> successors(FeatureNode node) {
    return graph.successors(node);
  }

  public EdgeType edgeValue(FeatureNode nodeU, FeatureNode nodeV) {
    return graph.edgeValueOrDefault(nodeU, nodeV, EdgeType.NONE);
  }

  public Set<EndpointPair<FeatureNode>> edges() {
    return graph.edges();
  }

  public ImmutableSet<FeatureNode> successors(FeatureNode node, NodeType nodeType) {
    return graph
        .successors(node)
        .stream()
        .filter(n -> n.nodeType() == nodeType)
        .collect(toImmutableSet());
  }

  public void pruneLeaves(NodeType nodeType) {
    while (pruneLeavesOnce(nodeType)) {
      // do nothing
    }
  }

  private boolean pruneLeavesOnce(NodeType nodeType) {
    Set<FeatureNode> toRemove = new HashSet<>();
    for (FeatureNode n : graph.nodes()) {
      if (n.nodeType() != nodeType) {
        if (graph.successors(n).isEmpty()) {
          toRemove.add(n);
        }
      }
    }
    toRemove.forEach(graph::removeNode);
    return !toRemove.isEmpty();
  }
}
