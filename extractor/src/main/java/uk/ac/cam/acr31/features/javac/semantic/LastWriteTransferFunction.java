/*
 * Copyright © 2018 The Authors (see NOTICE file)
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
      UnderlyingAST underlyingAst, List<LocalVariableNode> parameters) {
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
