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
import uk.ac.cam.acr31.features.javac.graph.DotOutput;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.FeatureNode;
import uk.ac.cam.acr31.features.javac.graph.NodeType;
import uk.ac.cam.acr31.features.javac.lexical.Tokens;
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
