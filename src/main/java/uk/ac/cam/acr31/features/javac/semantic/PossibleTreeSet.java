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

package uk.ac.cam.acr31.features.javac.semantic;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.dataflow.analysis.AbstractValue;

public class PossibleTreeSet implements AbstractValue<PossibleTreeSet> {

  private final Set<Tree> nodes;

  private PossibleTreeSet(Set<Tree> nodes) {
    this.nodes = nodes;
  }

  PossibleTreeSet(Tree node) {
    this(ImmutableSet.of(node));
  }

  PossibleTreeSet() {
    this(ImmutableSet.of());
  }

  public Set<Tree> nodes() {
    return nodes;
  }

  boolean isEmpty() {
    return nodes.isEmpty();
  }

  @Override
  public PossibleTreeSet leastUpperBound(PossibleTreeSet other) {
    Set<Tree> newNodes = Collections.newSetFromMap(new IdentityHashMap<>());
    newNodes.addAll(nodes);
    newNodes.addAll(other.nodes);
    return new PossibleTreeSet(newNodes);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PossibleTreeSet)) {
      return false;
    }
    PossibleTreeSet other = (PossibleTreeSet) obj;
    return nodes.equals(other.nodes);
  }

  @Override
  public int hashCode() {
    return nodes.hashCode();
  }

  @Override
  public String toString() {
    return nodes.stream()
        .map(n -> n.toString() + ":" + (((JCTree) n).pos))
        .collect(Collectors.joining(","));
  }
}
