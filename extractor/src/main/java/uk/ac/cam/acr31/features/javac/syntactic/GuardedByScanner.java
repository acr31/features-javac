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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;

/**
 * Add GUARDED_BY and GUARDED_BY_NEGATION edges between variable usages and the expression guarding
 * them in an if-statement.
 *
 * <p>e.g. {@code if (x>y) { ... x ...} else { ... y ....}} results in GUARDED_BY edge from x to
 * (x>y) and a GUARDED_BY_NEGATION edge from y to (x>y).
 */
public class GuardedByScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {
    GuardedByScanner guardedByVisitor = new GuardedByScanner(featureGraph);
    compilationUnitTree.accept(guardedByVisitor, null);
  }

  private GuardedByScanner(FeatureGraph graph) {
    this.graph = graph;
  }

  @Override
  public Void visitIf(IfTree node, Void ignored) {
    findIdentifiers(node.getCondition(), node.getThenStatement(), EdgeType.GUARDED_BY);
    findIdentifiers(node.getCondition(), node.getElseStatement(), EdgeType.GUARDED_BY_NEGATION);

    return super.visitIf(node, ignored);
  }

  private void findIdentifiers(Tree root, Tree node, EdgeType edgeType) {
    if (node == null) {
      return;
    }
    IdentifierCollector ic = new IdentifierCollector();
    node.accept(ic, null);
    for (IdentifierTree identifierTree : ic.identifiers) {
      graph.addEdge(identifierTree, root, edgeType);
    }
  }
}
