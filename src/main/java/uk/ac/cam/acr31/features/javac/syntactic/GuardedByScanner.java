package uk.ac.cam.acr31.features.javac.syntactic;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.FeatureNode;
import uk.ac.cam.acr31.features.javac.graph.NodeType;

public class GuardedByScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;

  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {
    var guardedByVisitor = new GuardedByScanner(featureGraph);
    compilationUnitTree.accept(guardedByVisitor, null);
  }

  private GuardedByScanner(FeatureGraph graph) {
    this.graph = graph;
  }

  @Override
  public Void visitIf(IfTree node, Void aVoid) {
    findIdentifiers(node.getCondition(), node.getThenStatement(), EdgeType.GUARDED_BY);
    findIdentifiers(node.getCondition(), node.getElseStatement(), EdgeType.GUARDED_BY_NEGATION);

    return super.visitIf(node, aVoid);
  }

  private void findIdentifiers(Tree root, Tree node, EdgeType edgeType) {
    if (node == null) {
      return;
    }
    IdentifierCollector ic = new IdentifierCollector();
    node.accept(ic, null);
    FeatureNode dest = graph.getFeatureNode(root);
    for (IdentifierTree identifierTree : ic.identifiers) {
      FeatureNode featureNode = graph.getFeatureNode(identifierTree);
      if (featureNode != null) {
        for (FeatureNode succ : graph.successors(featureNode, NodeType.TOKEN)) {
          graph.putEdgeValue(succ, dest, edgeType);
        }
      }
    }
  }
}
