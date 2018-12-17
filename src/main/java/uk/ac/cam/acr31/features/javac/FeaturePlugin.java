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

package uk.ac.cam.acr31.features.javac;

import com.google.common.collect.ImmutableRangeMap;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import uk.ac.cam.acr31.features.javac.graph.DotOutput;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.ProtoOutput;
import uk.ac.cam.acr31.features.javac.lexical.Tokens;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.semantic.DataflowOutputs;
import uk.ac.cam.acr31.features.javac.syntactic.ComputedFromScanner;
import uk.ac.cam.acr31.features.javac.syntactic.FormalArgScanner;
import uk.ac.cam.acr31.features.javac.syntactic.GuardedByScanner;
import uk.ac.cam.acr31.features.javac.syntactic.LastLexicalUseScanner;
import uk.ac.cam.acr31.features.javac.syntactic.ReturnsToScanner;

public class FeaturePlugin implements Plugin {

  @Override
  public String getName() {
    return "FeaturePlugin";
  }

  @Override
  public void init(JavacTask task, String... args) {

    Context context = ((BasicJavacTask) task).getContext();

    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.ANALYZE) {
              return;
            }

            process(e, context);
          }
        });
  }

  private static void process(TaskEvent e, Context context) {
    JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
    JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) e.getCompilationUnit();

    FeatureGraph featureGraph = new FeatureGraph();

    var compilerTokens = Tokens.getTokens(e.getSourceFile(), context);
    var tokens = Tokens.addToFeatureGraph(compilerTokens, featureGraph);
    addAstAndLinkToTokens(compilationUnit, tokens, featureGraph);

    var analysisResults = DataflowOutputs.create(e.getCompilationUnit(), processingEnvironment);
    DataflowOutputsScanner.addToGraph(compilationUnit, analysisResults, featureGraph);

    ComputedFromScanner.addToGraph(compilationUnit, featureGraph);
    LastLexicalUseScanner.addToGraph(compilationUnit, featureGraph);
    ReturnsToScanner.addToGraph(compilationUnit, featureGraph);
    FormalArgScanner.addToGraph(compilationUnit, featureGraph);
    GuardedByScanner.addToGraph(compilationUnit, featureGraph);

    // prune all ast nodes with no successors (these are leaves not connected to tokens)
    featureGraph.pruneLeaves(NodeType.AST_ELEMENT);

    File outputFile = new File(e.getSourceFile().getName() + ".dot");
    DotOutput.writeToDot(outputFile, featureGraph);
    try {
      ProtoOutput.write(new File(e.getSourceFile().getName() + ".proto"), featureGraph);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  private static void addAstAndLinkToTokens(
      JCTree.JCCompilationUnit compilationUnitTree,
      ImmutableRangeMap<Integer, FeatureNode> tokens,
      FeatureGraph featureGraph) {
    var graphScanner = new GraphScanner(featureGraph, tokens, compilationUnitTree.endPositions);
    compilationUnitTree.accept(
        graphScanner, featureGraph.createFeatureNode(NodeType.AST_ROOT, "root"));
  }
}
