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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.node.Node;

public class PossibleTreeSetStore implements Store<PossibleTreeSetStore> {

  private Map<Node, PossibleTreeSet> contents;

  private PossibleTreeSetStore(Map<Node, PossibleTreeSet> contents) {
    this.contents = contents;
  }

  PossibleTreeSetStore() {
    this(new HashMap<>());
  }

  PossibleTreeSet getInformation(Node n) {
    return contents.getOrDefault(n, new PossibleTreeSet());
  }

  void setInformation(Node n, PossibleTreeSet val) {
    if (val.isEmpty()) {
      contents.remove(n);
    } else {
      contents.put(n, val);
    }
  }

  @Override
  public PossibleTreeSetStore copy() {
    return new PossibleTreeSetStore(new HashMap<>(contents));
  }

  @Override
  public PossibleTreeSetStore leastUpperBound(PossibleTreeSetStore other) {
    Map<Node, PossibleTreeSet> newContents = new HashMap<>();

    for (Entry<Node, PossibleTreeSet> e : other.contents.entrySet()) {
      Node n = e.getKey();
      PossibleTreeSet otherVal = e.getValue();
      if (contents.containsKey(n)) {
        newContents.put(n, otherVal.leastUpperBound(contents.get(n)));
      } else {
        newContents.put(n, otherVal);
      }
    }

    for (Entry<Node, PossibleTreeSet> e : contents.entrySet()) {
      Node n = e.getKey();
      PossibleTreeSet thisVal = e.getValue();
      if (!other.contents.containsKey(n)) {
        newContents.put(n, thisVal);
      }
    }

    return new PossibleTreeSetStore(newContents);
  }

  @Override
  public PossibleTreeSetStore widenedUpperBound(PossibleTreeSetStore previous) {
    return leastUpperBound(previous);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PossibleTreeSetStore)) {
      return false;
    }
    PossibleTreeSetStore other = (PossibleTreeSetStore) o;
    return contents.equals(other.contents);
  }

  @Override
  public int hashCode() {
    return contents.hashCode();
  }

  @Override
  public String toString() {
    return contents.toString();
  }

  @Override
  public boolean canAlias(FlowExpressions.Receiver a, FlowExpressions.Receiver b) {
    return true;
  }

  @Override
  public void visualize(CFGVisualizer<?, PossibleTreeSetStore, ?> viz) {
    // Do nothing since ConstantPropagationStore doesn't support visualize
  }
}
