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

package uk.ac.cam.acr31.features.javac.syntactic;

import static uk.ac.cam.acr31.features.javac.Optionals.ifBothPresent;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;
import uk.ac.cam.acr31.features.javac.Symbols;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

/** Attaches symbol nodes to all elements which javac has resolved a symbol for. */
public class SymbolScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph featureGraph;

  public SymbolScanner(FeatureGraph featureGraph) {
    this.featureGraph = featureGraph;
  }

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {
    compilationUnitTree.accept(new SymbolScanner(featureGraph), null);
  }

  @Override
  public Void visitClass(ClassTree node, Void ignored) {
    addNode(node);
    return super.visitClass(node, ignored);
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void ignored) {
    addNode(node);
    return super.visitNewClass(node, ignored);
  }

  @Override
  public Void visitMethod(MethodTree node, Void ignored) {
    addNode(node);
    return super.visitMethod(node, ignored);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void ignored) {
    addNode(node);
    return super.visitMethodInvocation(node, ignored);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void ignored) {
    addNode(node);
    return super.visitIdentifier(node, ignored);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Void ignored) {
    addNode(node);
    return super.visitMemberSelect(node, ignored);
  }

  @Override
  public Void visitVariable(VariableTree node, Void ignored) {
    addNode(node);
    return super.visitVariable(node, ignored);
  }

  @Override
  public Void visitPackage(PackageTree node, Void ignored) {
    addNode(node);
    return super.visitPackage(node, ignored);
  }

  private void addNode(Tree node) {
    Optional<FeatureNode> featureNode =
        Symbols.getSymbol(node).map(sym -> featureGraph.createFeatureNode(toSymbolType(sym), sym));
    Optional<FeatureNode> target =
        Optional.ofNullable(featureGraph.lookupNode(node)).map(featureGraph::toIdentifierNode);
    // If your code says: String a = "a", b = "b", then javac synths up some extra ast nodes along
    // the lines of String a = "a"; String b = "b";  some of the extra nodes will be clones, some
    // (leaves) will just be the same node reused.  In this case we will visit String twice even
    // though both times point to the same token so just check that there is no edge before adding
    // another.
    ifBothPresent(
        featureNode,
        target,
        (f, t) -> {
          if (featureGraph.predecessors(t, EdgeType.ASSOCIATED_SYMBOL).isEmpty()) {
            featureGraph.addEdge(f, t, EdgeType.ASSOCIATED_SYMBOL);
          }
        });
  }

  private static NodeType toSymbolType(Symbol symbol) {
    switch (symbol.kind) {
      case VAR:
        return NodeType.SYMBOL_VAR;
      case MTH:
        return NodeType.SYMBOL_MTH;
      case TYP:
        return NodeType.SYMBOL_TYP;
      default:
        return NodeType.SYMBOL;
    }
  }
}
