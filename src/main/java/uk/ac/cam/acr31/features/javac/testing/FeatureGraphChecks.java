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

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;

public class FeatureGraphChecks {

  public static FeatureEdge edgeBetween(
      FeatureGraph graph, SourceSpan source, SourceSpan destination, EdgeType edgeType) {
    Set<FeatureNode> sourceNodes = graph.findNode(source.start(), source.end());
    Set<FeatureNode> destinationNodes = graph.findNode(destination.start(), destination.end());
    for (FeatureNode sourceNode : sourceNodes) {
      for (FeatureNode destinationNode : destinationNodes) {
        Optional<FeatureEdge> edge =
            graph
                .edges(sourceNode, destinationNode)
                .stream()
                .filter(e -> e.getType().equals(edgeType))
                .findAny();
        if (edge.isPresent()) {
          return edge.get();
        }
      }
    }
    throw new AssertionError(
        "Failed to find an edge from " + source + " to " + destination + " with type " + edgeType);
  }

  public static boolean isAcyclic(FeatureGraph graph, EdgeType edgeType) {

    HashSet<FeatureNode> nodes =
        graph
            .nodes()
            .stream()
            .filter(
                node ->
                    !graph.successors(node, edgeType).isEmpty()
                        || !graph.predecessors(node, edgeType).isEmpty())
            .collect(Collectors.toCollection(HashSet::new));

    while (true) {

      if (nodes.isEmpty()) {
        // graph is empty
        return true;
      }

      Optional<FeatureNode> possibleLeaf =
          nodes
              .stream()
              .filter(node -> Sets.intersection(graph.successors(node, edgeType), nodes).isEmpty())
              .findAny();

      if (!possibleLeaf.isPresent()) {
        // no leaves - must be cyclic
        return false;
      }

      // remove one leaf and repeat
      nodes.remove(possibleLeaf.get());
    }
  }
}
