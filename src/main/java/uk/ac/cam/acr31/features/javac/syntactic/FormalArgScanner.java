package uk.ac.cam.acr31.features.javac.syntactic;

import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.Map;
import uk.ac.cam.acr31.features.javac.DataflowOutputsScanner;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;

public class FormalArgScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;
  private final Map<Symbol.MethodSymbol, MethodTree> methodSymbols;

  private static class MethodSymbolVisitor extends TreeScanner<Void, Void> {

    Symbol.MethodSymbol sym = null;

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void aVoid) {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) node;
      if (fieldAccess.sym instanceof Symbol.MethodSymbol) {
        sym = (Symbol.MethodSymbol) fieldAccess.sym;
        return null;
      }
      return super.visitMemberSelect(node, aVoid);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void aVoid) {
      JCTree.JCIdent ident = (JCTree.JCIdent) node;
      if (ident.sym instanceof Symbol.MethodSymbol) {
        sym = (Symbol.MethodSymbol) ident.sym;
        return null;
      }
      return super.visitIdentifier(node, aVoid);
    }

    private static Symbol.MethodSymbol collect(Tree compilationUnitTree) {
      var methodSymbolVisitor = new MethodSymbolVisitor();
      compilationUnitTree.accept(methodSymbolVisitor, null);
      return methodSymbolVisitor.sym;
    }
  }

  private static class MethodSymbolCollector extends TreeScanner<Void, Void> {

    private ImmutableMap.Builder<Symbol.MethodSymbol, MethodTree> methodSymbols =
        ImmutableMap.builder();

    @Override
    public Void visitMethod(MethodTree node, Void aVoid) {
      var methodDecl = (JCTree.JCMethodDecl) node;
      methodSymbols.put(methodDecl.sym, node);
      return super.visitMethod(node, aVoid);
    }

    private static ImmutableMap<Symbol.MethodSymbol, MethodTree> collect(
        CompilationUnitTree compilationUnitTree) {
      var methodSymbolCollector = new MethodSymbolCollector();
      compilationUnitTree.accept(methodSymbolCollector, null);
      return methodSymbolCollector.methodSymbols.build();
    }
  }

  public static void addToGraph(CompilationUnitTree compilationUnitTree, FeatureGraph graph) {
    ImmutableMap<Symbol.MethodSymbol, MethodTree> methodSymbols =
        MethodSymbolCollector.collect(compilationUnitTree);
    FormalArgScanner formalArgScanner = new FormalArgScanner(graph, methodSymbols);
    compilationUnitTree.accept(formalArgScanner, null);
  }

  private FormalArgScanner(FeatureGraph graph, Map<Symbol.MethodSymbol, MethodTree> methodSymbols) {
    this.graph = graph;
    this.methodSymbols = methodSymbols;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
    Symbol.MethodSymbol sym = MethodSymbolVisitor.collect(node);
    if (sym != null && methodSymbols.containsKey(sym)) {
      MethodTree methodTree = methodSymbols.get(sym);
      var argumentIterator = node.getArguments().iterator();
      var parameterIterator = methodTree.getParameters().iterator();
      while (argumentIterator.hasNext() && parameterIterator.hasNext()) {
        ExpressionTree argument = argumentIterator.next();
        VariableTree parameter = parameterIterator.next();
        IdentifierCollector c = new IdentifierCollector();
        argument.accept(c, null);
        for (IdentifierTree identifierTree : c.identifiers) {
          DataflowOutputsScanner.linkTokens(
              graph.getFeatureNode(identifierTree),
              graph.getFeatureNode(parameter),
              EdgeType.FORMAL_ARG_NAME,
              graph);
        }
      }
    }
    return super.visitMethodInvocation(node, aVoid);
  }
}
