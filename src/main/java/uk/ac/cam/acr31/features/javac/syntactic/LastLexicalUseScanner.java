package uk.ac.cam.acr31.features.javac.syntactic;

import com.google.common.collect.ImmutableMultimap;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.Collection;
import java.util.Iterator;
import uk.ac.cam.acr31.features.javac.DataflowOutputsScanner;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;

public class LastLexicalUseScanner extends TreeScanner<Void, Void> {

  private ImmutableMultimap.Builder<Symbol, Tree> symbolMap = ImmutableMultimap.builder();

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {

    var lastLexicalUseScanner = new LastLexicalUseScanner();
    compilationUnitTree.accept(lastLexicalUseScanner, null);
    var symbolMap = lastLexicalUseScanner.symbolMap.build().asMap();

    for (Collection<Tree> commonIds : symbolMap.values()) {
      Iterator<Tree> idIterator = commonIds.iterator();
      Tree prevItem = idIterator.next();
      while (idIterator.hasNext()) {
        Tree next = idIterator.next();
        DataflowOutputsScanner.linkTokens(
            featureGraph.getFeatureNode(prevItem),
            featureGraph.getFeatureNode(next),
            EdgeType.LAST_LEXICAL_USE,
            featureGraph);
        prevItem = next;
      }
    }
  }

  @Override
  public Void visitVariable(VariableTree node, Void aVoid) {
    var variableDecl = (JCTree.JCVariableDecl) node;
    symbolMap.put(variableDecl.sym, node);
    return super.visitVariable(node, aVoid);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void aVoid) {
    var ident = (JCTree.JCIdent) node;
    symbolMap.put(ident.sym, node);
    return super.visitIdentifier(node, aVoid);
  }
}
