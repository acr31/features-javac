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

package uk.ac.cam.acr31.features.javac.semantic;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.DOTCFGVisualizer;

public class DataflowOutputs {

  public final AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites;
  public final AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses;

  private DataflowOutputs(
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites,
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses) {
    this.lastWrites = lastWrites;
    this.lastUses = lastUses;
  }

  public static ImmutableMap<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> create(
      CompilationUnitTree compilationUnitTree, ProcessingEnvironment processingEnvironment) {

    ImmutableMap.Builder<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> result =
        ImmutableMap.builder();
    for (ClassTree classTree : getClasses(compilationUnitTree)) {
      ImmutableMap.Builder<MethodTree, DataflowOutputs> methodResult = ImmutableMap.builder();
      for (MethodTree methodTree : getMethods(classTree)) {
        Optional<DataflowOutputs> results =
            DataflowOutputs.createForMethod(
                compilationUnitTree, classTree, methodTree, processingEnvironment);
        results.ifPresent(r -> methodResult.put(methodTree, r));
      }
      result.put(classTree, methodResult.build());
    }
    return result.build();
  }

  private static ImmutableList<ClassTree> getClasses(CompilationUnitTree t) {
    return t.getTypeDecls()
        .stream()
        .filter(typeDecl -> typeDecl.getKind().equals(Tree.Kind.CLASS))
        .map(typeDecl -> (ClassTree) typeDecl)
        .collect(toImmutableList());
  }

  private static ImmutableList<MethodTree> getMethods(ClassTree classTree) {
    return classTree
        .getMembers()
        .stream()
        .filter(member -> member.getKind().equals(Tree.Kind.METHOD))
        .map(member -> (MethodTree) member)
        .collect(toImmutableList());
  }

  private static Optional<DataflowOutputs> createForMethod(
      CompilationUnitTree compilationUnitTree,
      ClassTree classTree,
      MethodTree methodTree,
      ProcessingEnvironment processingEnvironment) {

    ControlFlowGraph controlFlowGraph;
    try {
      controlFlowGraph =
          CFGBuilder.build(compilationUnitTree, methodTree, classTree, processingEnvironment);
    } catch (NullPointerException e) {
      return Optional.empty();
    }

    LastWriteTransferFunction lastWriteTransferFunction = new LastWriteTransferFunction();
    Analysis<PossibleTreeSet, PossibleTreeSetStore, LastWriteTransferFunction> lastWriteAnalysis =
        new Analysis<>(lastWriteTransferFunction, processingEnvironment);
    lastWriteAnalysis.performAnalysis(controlFlowGraph);
    AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites =
        lastWriteAnalysis.getResult();
    //    writeToDot(controlFlowGraph, lastWriteAnalysis);

    LastUseTransferFunction lastUseTransferFunction = new LastUseTransferFunction();
    Analysis<PossibleTreeSet, PossibleTreeSetStore, LastUseTransferFunction> lastUseAnalysis =
        new Analysis<>(lastUseTransferFunction, processingEnvironment);
    lastUseAnalysis.performAnalysis(controlFlowGraph);
    AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses = lastUseAnalysis.getResult();
    //    writeToDot(controlFlowGraph, lastUseAnalysis);

    return Optional.of(new DataflowOutputs(lastWrites, lastUses));
  }

  private static <A extends AbstractValue<A>, B extends Store<B>, C extends TransferFunction<A, B>>
      void writeToDot(ControlFlowGraph controlFlowGraph, Analysis<A, B, C> analysis) {
    Map<String, Object> args = new HashMap<>();
    args.put("outdir", "/Users/acr31/ggg");
    args.put("checkerName", analysis.getTransferFunction().getClass().getSimpleName());
    CFGVisualizer<A, B, C> viz = new DOTCFGVisualizer<>();
    viz.init(args);
    viz.visualize(controlFlowGraph, controlFlowGraph.getEntryBlock(), analysis);
    viz.shutdown();
  }
}
