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
package uk.ac.cam.acr31.features.javac.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;

public class FeatureGraphChecks {

  public static FeatureEdge edgeBetween(
      FeatureGraph graph, String source, String destination, EdgeType edgeType) {
    FeatureNode sourceNode = findNode(graph, source);
    FeatureNode destinationNode = findNode(graph, destination);
    return graph
        .edges(sourceNode, destinationNode)
        .stream()
        .filter(e -> e.getType().equals(edgeType))
        .findAny()
        .orElseThrow(
            () ->
                new AssertionError(
                    "Failed to find an edge from "
                        + source
                        + " to "
                        + destination
                        + " with type "
                        + edgeType));
  }

  public static ImmutableList<String> astPathToToken(FeatureGraph graph, String destination) {
    FeatureNode destinationNode = findNode(graph, destination);
    return astPathToToken(
        graph,
        Iterables.getOnlyElement(graph.nodes(FeatureNode.NodeType.AST_ROOT)),
        destinationNode,
        new HashSet<>());
  }

  private static ImmutableList<String> astPathToToken(
      FeatureGraph graph, FeatureNode featureNode, FeatureNode destinationNode, Set<Long> visited) {
    if (visited.contains(featureNode.getId())) {
      return ImmutableList.of();
    }
    visited.add(featureNode.getId());
    if (featureNode.equals(destinationNode)) {
      return ImmutableList.of(featureNode.getContents());
    }
    for (FeatureNode node :
        graph.successors(featureNode, EdgeType.AST_CHILD, EdgeType.ASSOCIATED_TOKEN)) {
      ImmutableList<String> rest = astPathToToken(graph, node, destinationNode, visited);
      if (!rest.isEmpty()) {
        return ImmutableList.<String>builder().add(featureNode.getContents()).addAll(rest).build();
      }
    }
    return ImmutableList.of();
  }

  public static ImmutableList<String> getNodeContents(
      FeatureGraph graph, FeatureNode.NodeType nodeType) {
    return graph.nodes(nodeType).stream().map(FeatureNode::getContents).collect(toImmutableList());
  }

  private static FeatureNode findNode(FeatureGraph graph, String contents) {
    ImmutableList<String> path = ImmutableList.copyOf(Splitter.on(",").splitToList(contents));
    FeatureNode result = findNode(graph, null, path, contents);
    if (result == null) {
      throw new AssertionError("Failed to find node: " + contents);
    }
    return result;
  }

  private static FeatureNode findNode(
      FeatureGraph graph,
      FeatureNode searchPoint,
      ImmutableList<String> path,
      String originalContents) {
    if (path.isEmpty()) {
      return searchPoint;
    }

    Set<FeatureNode> successors =
        searchPoint == null ? graph.nodes() : graph.successors(searchPoint);
    FeatureNode found = null;
    for (FeatureNode node : successors) {
      if (node.getContents().equals(path.get(0))) {
        FeatureNode result = findNode(graph, node, path.subList(1, path.size()), originalContents);
        if (result != null) {
          if (found != null) {
            throw new AssertionError("Found more than one matching node for: " + originalContents);
          }
          found = result;
        }
      }
    }
    return found;
  }
}
