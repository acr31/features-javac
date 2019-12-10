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

package uk.ac.cam.acr31.features.javac.syntactic;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import java.util.Collection;
import java.util.Iterator;
import uk.ac.cam.acr31.features.javac.Symbols;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;

/** Adds edges between each usage of a variable in lexical order in the file. */
public class LastLexicalUseScanner extends TreeScanner<Void, Void> {

  private ImmutableMultimap.Builder<Symbol, Tree> symbolMap = ImmutableListMultimap.builder();

  /** Scan this tree and add all the features to the graph. */
  public static void addToGraph(
      CompilationUnitTree compilationUnitTree, FeatureGraph featureGraph) {

    LastLexicalUseScanner lastLexicalUseScanner = new LastLexicalUseScanner();
    compilationUnitTree.accept(lastLexicalUseScanner, null);
    ImmutableMap<Symbol, Collection<Tree>> symbolMap =
        lastLexicalUseScanner.symbolMap.build().asMap();

    for (Collection<Tree> commonIds : symbolMap.values()) {
      Iterator<Tree> idIterator = commonIds.iterator();
      Tree prevItem = idIterator.next();
      while (idIterator.hasNext()) {
        Tree next = idIterator.next();
        featureGraph.addIdentifierEdge(prevItem, next, EdgeType.LAST_LEXICAL_USE);
        prevItem = next;
      }
    }
  }

  @Override
  public Void visitVariable(VariableTree node, Void ignored) {
    Symbols.getSymbol(node).ifPresent(sym -> symbolMap.put(sym, node));
    return super.visitVariable(node, ignored);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void ignored) {
    Symbols.getSymbol(node)
        .filter(sym -> sym.kind == Kinds.Kind.VAR)
        .ifPresent(sym -> symbolMap.put(sym, node));
    return super.visitIdentifier(node, ignored);
  }
}
