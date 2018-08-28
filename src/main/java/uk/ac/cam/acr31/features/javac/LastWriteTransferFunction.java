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

public class LastWriteTransferFunction
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
    PossibleTreeSet value = store.getInformation(node);
    return new RegularTransferResult<>(value, store);
  }

  @Override
  public TransferResult<PossibleTreeSet, PossibleTreeSetStore> visitNode(
      Node n, TransferInput<PossibleTreeSet, PossibleTreeSetStore> p) {
    return new RegularTransferResult<>(null, p.getRegularStore());
  }

  @Override
  public TransferResult<PossibleTreeSet, PossibleTreeSetStore> visitAssignment(
      AssignmentNode n, TransferInput<PossibleTreeSet, PossibleTreeSetStore> pi) {
    // read the possible nodes and return them
    // also update the store to set the possible nodes to this target

    PossibleTreeSetStore p = pi.getRegularStore();
    Node target = n.getTarget();
    if (target instanceof LocalVariableNode) {
      LocalVariableNode t = (LocalVariableNode) target;
      p.setInformation(t, new PossibleTreeSet(t.getTree()));
    }
    return new RegularTransferResult<>(null, p);
  }
}
