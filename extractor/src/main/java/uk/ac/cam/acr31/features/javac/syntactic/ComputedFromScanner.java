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

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;

/** Add edges connecting v to all variables in expr when we see an assignment v = expr. */
public class ComputedFromScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;

  public static void addToGraph(CompilationUnitTree tree, FeatureGraph graph) {
    ComputedFromScanner computedFromScanner = new ComputedFromScanner(graph);
    tree.accept(computedFromScanner, null);
  }

  private ComputedFromScanner(FeatureGraph graph) {
    this.graph = graph;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void ignored) {

    IdentifierCollector rhsCollector = new IdentifierCollector();
    node.getExpression().accept(rhsCollector, null);

    IdentifierCollector lhsCollector = new IdentifierCollector();
    node.getVariable().accept(lhsCollector, null);

    for (IdentifierTree lhs : lhsCollector.identifiers) {
      for (IdentifierTree rhs : rhsCollector.identifiers) {
        graph.addIdentifierEdge(lhs, rhs, EdgeType.COMPUTED_FROM);
      }
    }
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, Void ignored) {
    ExpressionTree initializer = node.getInitializer();
    // if the initialiser is a NewClassTree then we are creating an anonymous inner class of some
    // sort. so it doesn't make sense to try and flow variables out of it.
    if (initializer == null) { // || initializer instanceof NewClassTree) {
      return null;
    }
    IdentifierCollector rhsCollector = new IdentifierCollector();
    initializer.accept(rhsCollector, null);

    for (IdentifierTree rhs : rhsCollector.identifiers) {
      graph.addIdentifierEdge(node, rhs, EdgeType.COMPUTED_FROM);
    }
    return null;
  }
}
