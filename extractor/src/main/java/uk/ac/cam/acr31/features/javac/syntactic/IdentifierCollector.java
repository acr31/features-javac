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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Kinds;
import java.util.ArrayList;
import java.util.List;
import uk.ac.cam.acr31.features.javac.Symbols;

/** Collector for variable and field names. */
class IdentifierCollector extends TreeScanner<Void, Void> {

  List<IdentifierTree> identifiers = new ArrayList<>();

  @Override
  public Void visitNewClass(NewClassTree node, Void ignored) {
    // dont visit the body of anonymous inner class constructors
    scan(node.getEnclosingExpression(), null);
    scan(node.getIdentifier(), null);
    scan(node.getTypeArguments(), null);
    scan(node.getArguments(), null);
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void ignored) {
    if (Symbols.getSymbol(node).map(sym -> sym.kind == Kinds.Kind.VAR).orElse(false)) {
      identifiers.add(node);
    }
    return super.visitIdentifier(node, ignored);
  }
}
