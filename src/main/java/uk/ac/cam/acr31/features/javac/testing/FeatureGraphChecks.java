package uk.ac.cam.acr31.features.javac.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;

public class FeatureGraphChecks {

  public static ImmutableList<String> pathTo(FeatureGraph graph, String destination) {
    return pathTo(
        graph,
        Iterables.getOnlyElement(graph.nodes(GraphProtos.FeatureNode.NodeType.AST_ROOT)),
        destination,
        new HashSet<>());
  }

  private static ImmutableList<String> pathTo(
      FeatureGraph graph,
      GraphProtos.FeatureNode featureNode,
      String destination,
      Set<Long> visited) {
    if (visited.contains(featureNode.getId())) {
      return ImmutableList.of();
    }
    visited.add(featureNode.getId());
    if (featureNode.getContents().equals(destination)) {
      return ImmutableList.of(featureNode.getContents());
    }
    for (GraphProtos.FeatureNode node :
        graph.successors(
            featureNode,
            GraphProtos.FeatureEdge.EdgeType.AST_CHILD,
            GraphProtos.FeatureEdge.EdgeType.ASSOCIATED_TOKEN)) {
      ImmutableList<String> rest = pathTo(graph, node, destination, visited);
      if (!rest.isEmpty()) {
        return ImmutableList.<String>builder().add(featureNode.getContents()).addAll(rest).build();
      }
    }
    return ImmutableList.of();
  }

  public static ImmutableList<String> getNodeContents(
      FeatureGraph graph, GraphProtos.FeatureNode.NodeType nodeType) {
    return graph
        .nodes(nodeType)
        .stream()
        .map(GraphProtos.FeatureNode::getContents)
        .collect(toImmutableList());
  }
}
