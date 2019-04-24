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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.Graph;

public class FeatureGraph {

  private final String sourceFileName;
  private final MutableNetwork<FeatureNode, FeatureEdge> graph;
  private final BiMap<Tree, FeatureNode> treeToNodeMap;
  private final EndPosTable endPosTable;
  private final LineMap lineMap;
  private final BiMap<Symbol, FeatureNode> symbolToNodeMap;
  private final Map<TypeMirror, FeatureNode> typeToNodeMap;
  /**
   * Many TypeMirrors may map to the same feature node. This maps nodes to an arbitrary one of these
   * TypeMirrors.
   */
  private final Map<FeatureNode, TypeMirror> nodeToSomeTypeMap;

  private int nodeIdCounter = 0;
  private FeatureNode firstToken = null;
  private FeatureNode astRoot = null;

  public FeatureGraph(String sourceFileName, EndPosTable endPosTable, LineMap lineMap) {
    this.sourceFileName = sourceFileName;
    this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
    this.treeToNodeMap = HashBiMap.create();
    this.symbolToNodeMap = HashBiMap.create();
    this.typeToNodeMap = new HashMap<>();
    this.nodeToSomeTypeMap = new HashMap<>();
    this.endPosTable = endPosTable;
    this.lineMap = lineMap;
  }

  public String getSourceFileName() {
    return sourceFileName;
  }

  public FeatureNode lookupNode(Tree tree) {
    return treeToNodeMap.get(tree);
  }

  public Tree lookupTree(FeatureNode node) {
    return treeToNodeMap.inverse().get(node);
  }

  public void replaceNodeInNodeMap(FeatureNode original, FeatureNode replacement) {
    Tree tree = treeToNodeMap.inverse().get(original);
    if (tree != null) {
      treeToNodeMap.put(tree, replacement);
    }
  }

  public FeatureNode createFeatureNode(NodeType nodeType, String contents, Tree tree) {
    // If your code says: String a = "a", b = "b", then javac synths up some extra ast nodes along
    // the lines of String a = "a"; String b = "b";  some of the extra nodes will be clones, some
    // (leaves) will just be the same node reused.  In this case we will try to create a node twice
    // when we visit the reused node for the second time.
    if (treeToNodeMap.containsKey(tree)) {
      return treeToNodeMap.get(tree);
    } else {
      int startPosition = ((JCTree) tree).getStartPosition();
      int endPosition = ((JCTree) tree).getEndPosition(endPosTable);
      FeatureNode result = createFeatureNode(nodeType, contents, startPosition, endPosition);
      treeToNodeMap.put(tree, result);
      return result;
    }
  }

  public FeatureNode createFeatureNode(
      NodeType nodeType, String contents, int startPosition, int endPosition) {
    int startLine = (int) lineMap.getLineNumber(startPosition);
    int endLine = (int) lineMap.getLineNumber(endPosition);
    return FeatureNode.newBuilder()
        .setId(nodeIdCounter++)
        .setType(nodeType)
        .setContents(contents)
        .setStartPosition(startPosition)
        .setEndPosition(endPosition)
        .setStartLineNumber(startLine)
        .setEndLineNumber(endLine)
        .build();
  }

  public FeatureNode createFeatureNode(NodeType nodeType, Symbol symbol) {
    if (symbolToNodeMap.containsKey(symbol)) {
      return symbolToNodeMap.get(symbol);
    } else {
      FeatureNode result = createFeatureNode(nodeType, symbol.toString(), -1, -1);
      symbolToNodeMap.put(symbol, result);
      return result;
    }
  }

  /**
   * Returns an arbitrary member of the equivalence class of type mirrors associated with the node.
   */
  public TypeMirror lookupTypeMirror(FeatureNode node) {
    return nodeToSomeTypeMap.get(node);
  }

  public FeatureNode createFeatureNodeForType(Types types, NodeType nodeType, TypeMirror type) {
    if (typeToNodeMap.containsKey(type)) {
      return typeToNodeMap.get(type);
    }
    // First check we haven't already got a feature node for an equal type.
    for (TypeMirror existingType : typeToNodeMap.keySet()) {
      if (types.isSameType(type, existingType)) {
        FeatureNode result = typeToNodeMap.get(existingType);
        typeToNodeMap.put(type, result);
        return result;
      }
    }
    // If we don't, then create a new feature node.
    FeatureNode result = createFeatureNode(nodeType, type.toString(), -1, -1);
    typeToNodeMap.put(type, result);
    nodeToSomeTypeMap.put(result, type);
    return result;
  }

  public Set<FeatureNode> nodes() {
    // returns an unmodifiable set
    return graph.nodes();
  }

  private Set<FeatureNode> nodes(NodeType... nodeTypes) {
    ImmutableList<NodeType> nodes = ImmutableList.copyOf(nodeTypes);
    return graph.nodes().stream()
        .filter(n -> nodes.contains(n.getType()))
        .collect(toImmutableSet());
  }

  public FeatureNode root() {
    return getAstRoot();
  }

  public Set<FeatureNode> tokens() {
    return nodes(NodeType.TOKEN, NodeType.IDENTIFIER_TOKEN);
  }

  public Set<FeatureNode> astNodes() {
    return nodes(NodeType.AST_ELEMENT, NodeType.FAKE_AST);
  }

  public Set<FeatureNode> comments() {
    return nodes(NodeType.COMMENT_BLOCK, NodeType.COMMENT_JAVADOC, NodeType.COMMENT_LINE);
  }

  public Set<FeatureNode> symbols() {
    return nodes(NodeType.SYMBOL, NodeType.SYMBOL_MTH, NodeType.SYMBOL_TYP, NodeType.SYMBOL_VAR);
  }

  public Set<FeatureNode> types() {
    return nodes(NodeType.TYPE);
  }

  public Set<FeatureEdge> edges() {
    // returns an unmodifiable set
    return graph.edges();
  }

  public Set<FeatureEdge> edges(EdgeType edgeType) {
    return graph.edges().stream()
        .filter(e -> e.getType().equals(edgeType))
        .collect(toImmutableSet());
  }

  public Set<FeatureEdge> edges(FeatureNode node) {
    return graph.incidentEdges(node);
  }

  public Set<FeatureEdge> edges(FeatureNode source, FeatureNode destination) {
    // returns an unmodifiable set
    return graph.edgesConnecting(source, destination);
  }

  public EndpointPair<FeatureNode> incidentNodes(FeatureEdge edge) {
    return graph.incidentNodes(edge);
  }

  public void removeNode(FeatureNode node) {
    graph.removeNode(node);
  }

  public Set<FeatureNode> successors(FeatureNode node) {
    // returns an unmodifiable set
    return graph.successors(node);
  }

  public Set<FeatureNode> successors(FeatureNode node, EdgeType... edgeTypes) {
    ImmutableList<EdgeType> edgeTypeList = ImmutableList.copyOf(edgeTypes);
    return graph.successors(node).stream()
        .filter(
            n ->
                graph.edgesConnecting(node, n).stream()
                    .anyMatch(e -> edgeTypeList.contains(e.getType())))
        .collect(toImmutableSet());
  }

  public Set<FeatureNode> predecessors(FeatureNode node) {
    return graph.predecessors(node);
  }

  public Set<FeatureNode> predecessors(FeatureNode node, EdgeType... edgeTypes) {
    ImmutableList<EdgeType> edgeTypeList = ImmutableList.copyOf(edgeTypes);
    return graph.predecessors(node).stream()
        .filter(
            n ->
                graph.edgesConnecting(n, node).stream()
                    .anyMatch(e -> edgeTypeList.contains(e.getType())))
        .collect(toImmutableSet());
  }

  /** Remove all ast nodes that have no successors. */
  public void pruneAstNodes() {
    //noinspection StatementWithEmptyBody
    while (pruneLeavesOnce()) {
      // do nothing
    }
  }

  public FeatureNode toIdentifierNode(FeatureNode node) {
    if (node.getType().equals(NodeType.IDENTIFIER_TOKEN)) {
      return node;
    }

    Optional<FeatureNode> any =
        successors(node, EdgeType.ASSOCIATED_TOKEN).stream()
            .filter(n -> n.getType().equals(NodeType.IDENTIFIER_TOKEN))
            .findAny();
    if (!any.isPresent()) {
      throw new AssertionError();
    }
    return any.get();
  }

  public void addEdge(Tree source, Tree dest, EdgeType type) {
    FeatureNode sourceNode = lookupNode(source);
    FeatureNode destNode = lookupNode(dest);
    if (sourceNode == null || destNode == null) {
      return;
    }
    addEdge(sourceNode, destNode, type);
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

  public void addIdentifierEdge(Tree source, Tree dest, EdgeType type) {
    FeatureNode sourceNode = lookupNode(source);
    FeatureNode destNode = lookupNode(dest);
    if (sourceNode == null || destNode == null) {
      return;
    }

    addEdge(toIdentifierNode(sourceNode), toIdentifierNode(destNode), type);
  }

  public void removeEdge(FeatureEdge edge) {
    graph.removeEdge(edge);
  }

  Graph toProtobuf() {
    return Graph.newBuilder()
        .setSourceFile(sourceFileName)
        .addAllNode(nodes())
        .addAllEdge(edges())
        .setFirstToken(firstToken)
        .setAstRoot(astRoot)
        .build();
  }

  public Set<FeatureNode> findNode(int start, int end) {
    return nodes().stream()
        .filter(node -> node.getStartPosition() == start)
        .filter(node -> node.getEndPosition() == end)
        .collect(toImmutableSet());
  }

  public void setFirstToken(FeatureNode firstToken) {
    this.firstToken = firstToken;
  }

  public FeatureNode getFirstToken() {
    return firstToken;
  }

  public void setAstRoot(FeatureNode astRoot) {
    this.astRoot = astRoot;
  }

  public FeatureNode getAstRoot() {
    return astRoot;
  }

  private boolean pruneLeavesOnce() {
    ImmutableSet<FeatureNode> toRemove =
        graph.nodes().stream()
            .filter(
                n ->
                    n.getType().equals(NodeType.AST_ELEMENT)
                        || n.getType().equals(NodeType.FAKE_AST))
            .filter(n -> graph.successors(n).isEmpty())
            .collect(toImmutableSet());
    toRemove.forEach(graph::removeNode);
    toRemove.forEach(n -> treeToNodeMap.inverse().remove(n));
    return !toRemove.isEmpty();
  }
}
