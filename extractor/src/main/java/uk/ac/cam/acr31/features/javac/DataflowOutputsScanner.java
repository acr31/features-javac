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

package uk.ac.cam.acr31.features.javac;

import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.semantic.DataflowOutputs;
import uk.ac.cam.acr31.features.javac.semantic.PossibleTreeSet;
import uk.ac.cam.acr31.features.javac.semantic.PossibleTreeSetStore;
import uk.ac.cam.acr31.features.javac.syntactic.ScanContext;

public class DataflowOutputsScanner extends TreeScanner<Void, ScanContext> {

  private final ImmutableMap<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> analysisResults;
  private final FeatureGraph graph;

  static void addToGraph(
      CompilationUnitTree tree,
      ImmutableMap<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> analysisResults,
      FeatureGraph graph) {
    DataflowOutputsScanner analysisOutputVariableScanner =
        new DataflowOutputsScanner(analysisResults, graph);
    tree.accept(analysisOutputVariableScanner, new ScanContext(null, null));
  }

  private DataflowOutputsScanner(
      ImmutableMap<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> analysisResults,
      FeatureGraph graph) {
    this.analysisResults = analysisResults;
    this.graph = graph;
  }

  @Override
  public Void visitClass(ClassTree node, ScanContext scanContext) {
    return super.visitClass(node, scanContext.withClassTree(node));
  }

  @Override
  public Void visitMethod(MethodTree node, ScanContext scanContext) {
    return super.visitMethod(node, scanContext.withMethodTree(node));
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, ScanContext scanContext) {
    apply(node, scanContext);
    return super.visitIdentifier(node, scanContext);
  }

  @Override
  public Void visitVariable(VariableTree node, ScanContext scanContext) {
    apply(node, scanContext);
    return super.visitVariable(node, scanContext);
  }

  private void apply(Tree node, ScanContext context) {
    if (context.classTree == null || context.methodTree == null) {
      return;
    }
    ImmutableMap<MethodTree, DataflowOutputs> map = analysisResults.get(context.classTree);
    if (map == null) {
      return;
    }
    DataflowOutputs a = map.get(context.methodTree);
    if (a == null) {
      return;
    }
    applyAnalysisResult(node, a.lastWrites, EdgeType.LAST_WRITE);
    applyAnalysisResult(node, a.lastUses, EdgeType.LAST_USE);
  }

  private void applyAnalysisResult(
      Tree node, AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> r, EdgeType edgeType) {
    PossibleTreeSet possibles = r.getValue(node);
    if (possibles != null) {
      for (Tree tree : possibles.nodes()) {
        graph.addIdentifierEdge(tree, node, edgeType);
      }
    }
  }
}
