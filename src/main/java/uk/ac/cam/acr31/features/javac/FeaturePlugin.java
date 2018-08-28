package uk.ac.cam.acr31.features.javac;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
            try {
              process(e, context);
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        });
  }

  private static void process(TaskEvent e, Context context) throws IOException {
    JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);

    ImmutableRangeMap<Integer, Tokens.Token> tokens = getTokens(e, context);

    ImmutableMap<ClassTree, ImmutableMap<MethodTree, AnalysisOutputs>> analysisResults =
        getAnalysisResults(e.getCompilationUnit(), processingEnvironment);

    TreeNode n = new TreeNode(null);
    JavacTreePathScanner treePathScanner = new JavacTreePathScanner();
    e.getCompilationUnit().accept(treePathScanner, n);

    // walk the tree
    // add each node to the graph
    // get the tokens covered by the node position
    // add all the tokens in the gaps between the ast children
    // link the tokens with NextToken edges

    System.out.println(n.toString());
    System.out.println(tokens);
    System.out.println(analysisResults);
  }

  private static ImmutableMap<ClassTree, ImmutableMap<MethodTree, AnalysisOutputs>>
      getAnalysisResults(
          CompilationUnitTree compilationUnitTree, ProcessingEnvironment processingEnvironment) {

    ImmutableMap.Builder<ClassTree, ImmutableMap<MethodTree, AnalysisOutputs>> result =
        ImmutableMap.builder();
    for (ClassTree classTree : getClasses(compilationUnitTree)) {
      ImmutableMap.Builder<MethodTree, AnalysisOutputs> methodResult = ImmutableMap.builder();
      for (MethodTree methodTree : getMethods(classTree)) {
        methodResult.put(
            methodTree,
            doAnalysis(compilationUnitTree, classTree, methodTree, processingEnvironment));
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

  private static AnalysisOutputs doAnalysis(
      CompilationUnitTree compilationUnitTree,
      ClassTree classTree,
      MethodTree methodTree,
      ProcessingEnvironment processingEnvironment) {

    ControlFlowGraph controlFlowGraph =
        CFGBuilder.build(compilationUnitTree, methodTree, classTree, processingEnvironment);

    LastWriteTransferFunction lastWriteTransferFunction = new LastWriteTransferFunction();
    Analysis<PossibleTreeSet, PossibleTreeSetStore, LastWriteTransferFunction> lastWriteAnalysis =
        new Analysis<>(lastWriteTransferFunction, processingEnvironment);
    lastWriteAnalysis.performAnalysis(controlFlowGraph);
    AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites =
        lastWriteAnalysis.getResult();
    writeToDot(controlFlowGraph, lastWriteAnalysis);

    LastUseTransferFunction lastUseTransferFunction = new LastUseTransferFunction();
    Analysis<PossibleTreeSet, PossibleTreeSetStore, LastUseTransferFunction> lastUseAnalysis =
        new Analysis<>(lastUseTransferFunction, processingEnvironment);
    lastUseAnalysis.performAnalysis(controlFlowGraph);
    AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses = lastUseAnalysis.getResult();
    writeToDot(controlFlowGraph, lastUseAnalysis);

    return new AnalysisOutputs(lastWrites, lastUses);
  }

  private static ImmutableRangeMap<Integer, Tokens.Token> getTokens(TaskEvent e, Context context)
      throws IOException {
    ScannerFactory scannerFactory = ScannerFactory.instance(context);
    Scanner tokenScanner = scannerFactory.newScanner(e.getSourceFile().getCharContent(true), true);
    ImmutableRangeMap.Builder<Integer, Tokens.Token> tokenMap = ImmutableRangeMap.builder();
    while (true) {
      tokenScanner.nextToken();
      Tokens.Token token = tokenScanner.token();
      if (token.kind == Tokens.TokenKind.EOF) {
        break;
      }
      tokenMap.put(Range.closedOpen(token.pos, token.endPos), token);
    }
    return tokenMap.build();
  }

  private static <A extends AbstractValue<A>, B extends Store<B>, C extends TransferFunction<A, B>>
      void writeToDot(ControlFlowGraph controlFlowGraph, Analysis<A, B, C> analysis) {
    Map<String, Object> args = new HashMap<>();
    args.put("outdir", "/Users/acr31/ggg");
    args.put("checkerName", analysis.getTransferFunction().getClass().getSimpleName());
    CFGVisualizer<A, B, C> viz = new DOTCFGVisualizer<>();
    viz.init(args);
    Map<String, Object> res =
        viz.visualize(controlFlowGraph, controlFlowGraph.getEntryBlock(), analysis);
    viz.shutdown();

    String dotFile = (String) res.get("dotFileName");
    try {
      String command = "/usr/local/bin/dot -Tpdf \"" + dotFile + "\" -o \"" + dotFile + ".pdf\"";
      System.out.println(command);
      Process child = Runtime.getRuntime().exec(command);
      child.waitFor();
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
