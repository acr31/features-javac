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

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Predicate;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class DotOutput {

  public static void writeToDot(File outputFile, FeatureGraph graph) {
    try (PrintWriter w = new PrintWriter(new FileWriter(outputFile))) {
      w.println("digraph {");
      w.println(" rankdir=LR;");

      ImmutableSet<FeatureNode> nodeSet = graph.nodes(NodeType.AST_ROOT);
      while (!nodeSet.isEmpty()) {
        writeSubgraph(w, nodeSet, "same");
        nodeSet = getSuccessors(nodeSet, graph, e -> e == EdgeType.AST_CHILD);
      }

      ImmutableSet<FeatureNode> commentSet =
          graph.nodes(NodeType.COMMENT_BLOCK, NodeType.COMMENT_JAVADOC, NodeType.COMMENT_LINE);
      writeSubgraph(w, commentSet, "same");

      ImmutableSet<FeatureNode> tokenSet = graph.nodes(NodeType.TOKEN);
      writeSubgraph(w, tokenSet, "max");

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
        node.getId(),
        node.getId(),
        node.getContents().isEmpty() ? node.getType() : node.getContents());
  }

  private static String dotEdge(EndpointPair<FeatureNode> edge, FeatureGraph graph) {
    EdgeType edgeType = graph.edgeValue(edge.nodeU(), edge.nodeV());
    String ports;
    switch (edgeType) {
      case NEXT_TOKEN:
        ports = "headport=n, tailport=s, weight=1000";
        break;
      case AST_CHILD:
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
    return String.format("%d -> %d [ %s];\n", edge.nodeU().getId(), edge.nodeV().getId(), ports);
  }

  private static void writeSubgraph(PrintWriter w, ImmutableSet<FeatureNode> nodeSet, String rank) {
    w.println(" subgraph {");
    w.println(String.format("  rank=%s;", rank));
    nodeSet.forEach(n -> w.println(dotNode(n)));
    w.println(" }");
  }

  private static ImmutableSet<FeatureNode> getSuccessors(
      ImmutableSet<FeatureNode> nodeSet, FeatureGraph graph, Predicate<EdgeType> edgeFilter) {
    ImmutableSet.Builder<FeatureNode> result = ImmutableSet.builder();
    for (FeatureNode node : nodeSet) {
      graph
          .successors(node)
          .stream()
          .filter(n -> edgeFilter.test(graph.edgeValue(node, n)))
          .forEach(result::add);
    }
    return result.build();
  }
}
