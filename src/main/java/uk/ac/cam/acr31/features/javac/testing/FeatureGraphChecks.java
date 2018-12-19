package uk.ac.cam.acr31.features.javac.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;

public class FeatureGraphChecks {

  private static FeatureNode findNode(FeatureGraph graph, String contents) {
    return Iterables.getOnlyElement(
        graph
            .nodes()
            .stream()
            .filter(n -> n.getContents().equals(contents))
            .collect(toImmutableList()));
  }

  public static boolean edgeBetween(
      FeatureGraph graph, String source, String destination, EdgeType edgeType) {
    FeatureNode sourceNode = findNode(graph, source);
    FeatureNode destinationNode = findNode(graph, destination);
    return graph
        .edges(sourceNode, destinationNode)
        .stream()
        .anyMatch(e -> e.getType().equals(edgeType));
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
}
