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
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreeScanner;
import java.util.List;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;

public class ReturnsToScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {
    ReturnsToScanner returnsToScanner = new ReturnsToScanner(featureGraph);
    compilationUnitTree.accept(returnsToScanner, null);
  }

  private ReturnsToScanner(FeatureGraph graph) {
    this.graph = graph;
  }

  private static class ReturnCollector extends TreeScanner<Void, Void> {

    List<IdentifierTree> identifiers;

    @Override
    public Void visitReturn(ReturnTree node, Void ignored) {
      if (node.getExpression() == null) {
        return null;
      }
      IdentifierCollector ic = new IdentifierCollector();
      node.getExpression().accept(ic, null);
      identifiers = ic.identifiers;
      return null;
    }
  }

  @Override
  public Void visitMethod(MethodTree node, Void ignored) {
    if (node.getBody() == null) {
      return null;
    }
    ReturnCollector returnCollector = new ReturnCollector();
    node.getBody().accept(returnCollector, null);

    if (returnCollector.identifiers != null) {
      FeatureNode dest = graph.lookupNode(node);
      for (IdentifierTree identifierTree : returnCollector.identifiers) {
        FeatureNode featureNode = graph.lookupNode(identifierTree);
        if (featureNode != null) {
          for (FeatureNode succ : graph.successors(featureNode, EdgeType.ASSOCIATED_TOKEN)) {
            graph.addEdge(succ, dest, EdgeType.RETURNS_TO);
          }
        }
      }
    }
    return super.visitMethod(node, ignored);
  }
}
