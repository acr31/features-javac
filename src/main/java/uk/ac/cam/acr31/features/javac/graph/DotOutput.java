package uk.ac.cam.acr31.features.javac.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DotOutput {

  public static void writeToDot(File outputFile, FeatureGraph graph) {
    try (PrintWriter w = new PrintWriter(new FileWriter(outputFile))) {
      w.println("digraph {");
      w.println(" rankdir=LR;");

      ImmutableSet<FeatureNode> nodeSet = graph.nodes(NodeType.AST_ROOT);
      while (!nodeSet.isEmpty()) {
        writeSubgraph(w, nodeSet, "same");
        nodeSet = getSuccessors(nodeSet, graph);
      }

      nodeSet = graph.nodes(NodeType.TOKEN);
      writeSubgraph(w, nodeSet, "max");

      for (EndpointPair<FeatureNode> edge : graph.edges()) {
        w.println(dotEdge(edge, graph));
      }

      w.println("}");
    } catch (IOException e) {
      throw new RuntimeException("Failed to write to " + outputFile);
    }
  }

  private static String dotNode(FeatureNode node) {
    return String.format(
        "%d [ label=\"%d:%s\"];\n",
        node.nodeId(),
        node.nodeId(),
        node.contents().isEmpty() ? node.nodeType() : node.contents());
  }

  private static String dotEdge(EndpointPair<FeatureNode> edge, FeatureGraph graph) {
    EdgeType edgeType = graph.edgeValue(edge.nodeU(), edge.nodeV());
    String ports;
    switch (edgeType) {
      case NEXT_TOKEN:
        ports = "headport=n, tailport=s, weight=1000";
        break;
      case CHILD:
        ports = "headport=w, tailport=e";
        break;
      case LAST_WRITE:
        ports = "headport=e, tailport=e, color=red, weight=0";
        break;
      case LAST_USE:
        ports = "headport=e, tailport=e, color=green, weight=0";
        break;
      case COMPUTED_FROM:
        ports = "headport=e, tailport=e, color=purple, weight=0";
        break;
      case LAST_LEXICAL_USE:
        ports = "headport=w, tailport=w, color=orange, weight=0";
        break;
      case RETURNS_TO:
        ports = "headport=e, tailport=w, color=blue, weight=0";
        break;
      case FORMAL_ARG_NAME:
        ports = "headport=w, tailport=w, color=yellow, weight=0";
        break;
      case GUARDED_BY:
        ports = "headport=e, tailport=w, color=pink, weight=0";
        break;
      case GUARDED_BY_NEGATION:
        ports = "headport=e, tailport=w, color=pink, weight=0, style=dashed";
        break;
      default:
        ports = "";
    }
    return String.format("%d -> %d [ %s];\n", edge.nodeU().nodeId(), edge.nodeV().nodeId(), ports);
  }

  private static void writeSubgraph(PrintWriter w, ImmutableSet<FeatureNode> nodeSet, String rank) {
    w.println(" subgraph {");
    w.println(String.format("  rank=%s;", rank));
    nodeSet.forEach(n -> w.println(dotNode(n)));
    w.println(" }");
  }

  private static ImmutableSet<FeatureNode> getSuccessors(
      ImmutableSet<FeatureNode> nodeSet, FeatureGraph graph) {
    ImmutableSet.Builder<FeatureNode> result = ImmutableSet.builder();
    for (FeatureNode node : nodeSet) {
      for (FeatureNode succ : graph.successors(node)) {
        EdgeType edgeType = graph.edgeValue(node, succ);
        if (edgeType.equals(EdgeType.CHILD) && !succ.nodeType().equals(NodeType.TOKEN)) {
          result.add(succ);
        }
      }
    }
    return result.build();
  }
}
