package uk.ac.cam.acr31.features.javac;

import java.util.List;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

public class LastUseTransferFunction
    extends AbstractNodeVisitor<
        TransferResult<PossibleTreeSet, PossibleTreeSetStore>,
        TransferInput<PossibleTreeSet, PossibleTreeSetStore>>
    implements TransferFunction<PossibleTreeSet, PossibleTreeSetStore> {

  @Override
  public PossibleTreeSetStore initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    PossibleTreeSetStore store = new PossibleTreeSetStore();
    for (LocalVariableNode n : parameters) {
      store.setInformation(n, new PossibleTreeSet(n.getTree()));
    }
    return store;
  }

  @Override
  public TransferResult<PossibleTreeSet, PossibleTreeSetStore> visitLocalVariable(
      LocalVariableNode node, TransferInput<PossibleTreeSet, PossibleTreeSetStore> before) {
    PossibleTreeSetStore store = before.getRegularStore();
    PossibleTreeSet result = store.getInformation(node);
    PossibleTreeSet p = new PossibleTreeSet(node.getTree());
    store.setInformation(node, p);
    return new RegularTransferResult<>(result, store);
  }

  @Override
  public TransferResult<PossibleTreeSet, PossibleTreeSetStore> visitNode(
      Node n, TransferInput<PossibleTreeSet, PossibleTreeSetStore> p) {
    return new RegularTransferResult<>(null, p.getRegularStore());
  }
}
