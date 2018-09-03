package uk.ac.cam.acr31.features.javac.syntactic;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreeScanner;
import uk.ac.cam.acr31.features.javac.DataflowOutputsScanner;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.FeatureNode;

public class ComputedFromScanner extends TreeScanner<Void, Void> {

  private final FeatureGraph graph;

  public static void addToGraph(CompilationUnitTree tree, FeatureGraph graph) {
    var computedFromScanner = new ComputedFromScanner(graph);
    tree.accept(computedFromScanner, null);
  }

  private ComputedFromScanner(FeatureGraph graph) {
    this.graph = graph;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void aVoid) {

    IdentifierCollector rhsCollector = new IdentifierCollector();
    node.getExpression().accept(rhsCollector, null);

    IdentifierCollector lhsCollector = new IdentifierCollector();
    node.getVariable().accept(lhsCollector, null);

    for (IdentifierTree lhs : lhsCollector.identifiers) {
      FeatureNode lhsFeatureNode = graph.getFeatureNode(lhs);
      for (IdentifierTree rhs : rhsCollector.identifiers) {
        DataflowOutputsScanner.linkTokens(
            lhsFeatureNode, graph.getFeatureNode(rhs), EdgeType.COMPUTED_FROM, graph);
      }
    }
    return null;
  }
}
