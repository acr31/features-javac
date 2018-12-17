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

package uk.ac.cam.acr31.features.javac;

import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.FeatureNode;
import uk.ac.cam.acr31.features.javac.graph.NodeType;

/**
 * Pretends to be a JCTree for the purposes of normlising the processing of nodes and lists of
 * nodes.
 */
class PseudoTreeForList extends JCTree {

  private final List<Tree> childList;
  private final FeatureGraph graph;

  PseudoTreeForList(List<?> childList, FeatureGraph graph) {
    this.childList = new ArrayList<>();
    this.graph = graph;

    if (childList == null) {
      return;
    }

    for (Object child : childList) {
      if (child instanceof List) {
        this.childList.add(new PseudoTreeForList((List) child, graph));
      } else {
        this.childList.add((Tree) child);
      }
    }
  }

  @Override
  public Tag getTag() {
    return null;
  }

  @Override
  public void accept(Visitor v) {}

  @Override
  public <R, D> R accept(TreeVisitor<R, D> v, D d) {
    if (d instanceof FeatureNode && v instanceof GraphScanner) {
      acceptScanner((GraphScanner) v, (FeatureNode) d);
    }
    return null;
  }

  @Override
  public Kind getKind() {
    return null;
  }

  @Override
  public int getStartPosition() {
    if (childList == null) {
      return Position.NOPOS;
    }
    for (Tree child : childList) {
      if (child != null && !GraphScanner.isSynthetic((JCTree) child)) {
        return (((JCTree) child).getStartPosition());
      }
    }
    return Position.NOPOS;
  }

  @Override
  public int getEndPosition(EndPosTable endPosTable) {
    if (childList == null) {
      return Position.NOPOS;
    }

    Tree lastNonNull = null;
    for (Tree child : childList) {
      if (child != null && !GraphScanner.isSynthetic((JCTree) child)) {
        lastNonNull = child;
      }
    }
    if (lastNonNull != null) {
      return (((JCTree) lastNonNull).getEndPosition(endPosTable));
    }
    return Position.NOPOS;
  }

  private void acceptScanner(GraphScanner v, FeatureNode graphParent) {
    if (childList == null || childList.isEmpty()) {
      return;
    }
    FeatureNode listNode = graph.createFeatureNode(NodeType.AST_ELEMENT, "list");
    graph.putEdgeValue(graphParent, listNode, EdgeType.AST_CHILD);
    Optional<Integer> previousEnd = Optional.empty();
    for (Tree element : childList) {
      Optional<Integer> start = GraphScanner.getStartPosition((JCTree) element);
      if (previousEnd.isPresent() && start.isPresent() && previousEnd.get() < start.get()) {
        v.addTokens(listNode, previousEnd.get(), start.get());
      }
      Optional<Integer> end = v.getEndPosition((JCTree) element);
      if (end.isPresent()) {
        previousEnd = end;
      }
      element.accept(v, listNode);
    }
  }
}
