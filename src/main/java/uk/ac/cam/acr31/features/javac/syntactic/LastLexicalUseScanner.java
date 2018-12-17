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
  public Void visitVariable(VariableTree node, Void ignored) {
    var variableDecl = (JCTree.JCVariableDecl) node;
    symbolMap.put(variableDecl.sym, node);
    return super.visitVariable(node, ignored);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void ignored) {
    var ident = (JCTree.JCIdent) node;
    if (ident.sym != null) {
      symbolMap.put(ident.sym, node);
    }
    return super.visitIdentifier(node, ignored);
  }
}
