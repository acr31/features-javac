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

package uk.ac.cam.acr31.features.javac.dot;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType.IDENTIFIER_TOKEN;
import static uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType.METHOD_SIGNATURE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.Graph;
import uk.ac.cam.acr31.features.javac.proto.NodeTypes;

public class DotOutput {

  /** Write this graph to a dot file. */
  public static void writeToDot(File outputFile, Graph graph, boolean verboseDot)
      throws IOException {
    try (FileWriter w = new FileWriter(outputFile)) {
      w.write(createDot(graph, indexGraph(graph), verboseDot));
    }
  }

  private static ImmutableNetwork<FeatureNode, FeatureEdge> indexGraph(Graph graph) {
    MutableNetwork<FeatureNode, FeatureEdge> result =
        NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
    ImmutableMap<Long, FeatureNode> nodeMap =
        Maps.uniqueIndex(graph.getNodeList(), FeatureNode::getId);
    for (FeatureEdge edge : graph.getEdgeList()) {
      FeatureNode src = nodeMap.get(edge.getSourceId());
      FeatureNode dest = nodeMap.get(edge.getDestinationId());
      result.addEdge(src, dest, edge);
    }
    return ImmutableNetwork.copyOf(result);
  }

  /** Write this graph to a dot file. */
  private static String createDot(
      Graph graph, ImmutableNetwork<FeatureNode, FeatureEdge> index, boolean verboseDot) {
    StringWriter result = new StringWriter();
    try (PrintWriter w = new PrintWriter(result)) {
      w.println("digraph {");
      w.println(" rankdir=LR;");

      Set<FeatureNode> nodeSet = ImmutableSet.of(graph.getAstRoot());
      while (!nodeSet.isEmpty()) {
        writeSubgraph(w, nodeSet, "same", verboseDot);
        nodeSet = getAstChildren(nodeSet, graph, index);
      }

      Set<FeatureNode> commentSet =
          graph.getNodeList().stream()
              .filter(n -> NodeTypes.isComment(n.getType()))
              .collect(toImmutableSet());
      writeSubgraph(w, commentSet, null, verboseDot);

      Set<FeatureNode> symbolSet =
          graph.getNodeList().stream()
              .filter(n -> NodeTypes.isSymbol(n.getType()))
              .collect(toImmutableSet());
      writeSubgraph(w, symbolSet, "max", verboseDot);

      Set<FeatureNode> signatureSet =
          graph.getNodeList().stream()
              .filter(n -> n.getType().equals(METHOD_SIGNATURE))
              .collect(toImmutableSet());
      writeSubgraph(w, signatureSet, null, verboseDot);

      Set<FeatureNode> tokenSet =
          graph.getNodeList().stream()
              .filter(n -> NodeTypes.isToken(n.getType()))
              .collect(toImmutableSet());
      writeSubgraph(w, tokenSet, "same", verboseDot);

      for (FeatureEdge edge : graph.getEdgeList()) {
        w.println(dotEdge(edge, index.incidentNodes(edge)));
      }

      w.println("}");
    }
    return result.toString();
  }

  private static String dotNode(FeatureNode node, boolean verbose) {
    String format = "";
    if (node.getType().equals(IDENTIFIER_TOKEN)) {
      format = ", color=blue";
    }

    if (verbose) {
      return String.format(
          "%d [ label=\"%d:%s\\n%s\\nPos:%d - %d\" %s];\n",
          node.getId(),
          node.getId(),
          node.getType(),
          escapeContents(node),
          node.getStartPosition(),
          node.getEndPosition(),
          format);
    } else {
      return String.format("%d [ label=\"%s\" %s];\n", node.getId(), escapeContents(node), format);
    }
  }

  private static String escapeContents(FeatureNode node) {
    if (node.getContents().isEmpty()) {
      return node.getType().toString();
    }
    return StringEscapeUtils.escapeJava(node.getContents());
  }

  private static String dotEdge(FeatureEdge edge, EndpointPair<FeatureNode> incidentNodes) {
    EdgeType edgeType = edge.getType();
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
      case ASSOCIATED_TOKEN:
        if (incidentNodes.target().getType().equals(IDENTIFIER_TOKEN)) {
          ports = "color=blue";
        } else {
          ports = "";
        }
        break;
      case HAS_TYPE:
        return "";
      case ASSIGNABLE_TO:
        ports = "headport=e, tailport=w, color=brown, weight=0";
        break;
      default:
        ports = "";
    }
    return String.format("%d -> %d [ %s];\n", edge.getSourceId(), edge.getDestinationId(), ports);
  }

  private static void writeSubgraph(
      PrintWriter w, Set<FeatureNode> nodeSet, String rank, boolean verbose) {
    w.println(" subgraph {");
    if (rank != null) {
      w.println(String.format("  rank=%s;", rank));
    }
    nodeSet.forEach(n -> w.println(dotNode(n, verbose)));
    w.println(" }");
  }

  private static Set<FeatureNode> getAstChildren(
      Set<FeatureNode> nodeSet, Graph graph, ImmutableNetwork<FeatureNode, FeatureEdge> index) {
    ImmutableSet.Builder<FeatureNode> result = ImmutableSet.builder();
    for (FeatureNode node : nodeSet) {
      index.successors(node).stream()
          .filter(
              n ->
                  index.edgesConnecting(node, n).stream()
                      .anyMatch(e -> e.getType().equals(EdgeType.AST_CHILD)))
          .forEach(result::add);
    }
    return result.build();
  }
}
