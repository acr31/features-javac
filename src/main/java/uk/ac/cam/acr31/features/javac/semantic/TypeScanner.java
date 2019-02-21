/*
 * Copyright © 2019 Henry Mercer (henry.mercer@me.com)
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

package uk.ac.cam.acr31.features.javac.semantic;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;

public class TypeScanner extends TreeScanner<Void, Void> {
  private final FeatureGraph graph;
  private final TypeAnalysis typeAnalysis;

  public TypeScanner(FeatureGraph graph, TypeAnalysis typeAnalysis) {
    this.graph = graph;
    this.typeAnalysis = typeAnalysis;
  }

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph graph, TypeAnalysis typeAnalysis) {
    TypeScanner typeScanner = new TypeScanner(graph, typeAnalysis);
    compilationUnitTree.accept(typeScanner, null);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void ignored) {
    for (ExpressionTree argTree : tree.getArguments()) {
      GraphProtos.FeatureNode argNode = graph.lookupNode(argTree);
      if (argNode != null) {
        // Do we need TypeAnalysis?
        GraphProtos.FeatureNode typeNode = graph.createFeatureNodeForType(
            typeAnalysis.getTypes(),
            GraphProtos.FeatureNode.NodeType.TYPE,
            typeAnalysis.getTypeMirror(argTree)
        );
        if (typeNode != null) {
          graph.addEdge(argNode, typeNode, GraphProtos.FeatureEdge.EdgeType.HAS_TYPE);
        }
      }
    }
    return super.visitMethodInvocation(tree, ignored);
  }
}
