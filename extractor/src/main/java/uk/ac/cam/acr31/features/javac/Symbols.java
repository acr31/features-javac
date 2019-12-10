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

package uk.ac.cam.acr31.features.javac;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;

public class Symbols {

  /** Return the symbol for this tree or empty if none is found. */
  public static Optional<Symbol> getSymbol(Tree node) {
    return Optional.ofNullable(ASTHelpers.getSymbol(node));
  }

  /** Return the method symbol for this method declaration or empty if none is found. */
  public static Optional<Symbol.MethodSymbol> getSymbol(MethodTree node) {
    return Optional.ofNullable(ASTHelpers.getSymbol(node));
  }

  /** Return the method symbol for this method invocation or empty if none is found. */
  public static Optional<Symbol.MethodSymbol> getSymbol(MethodInvocationTree node) {
    return Optional.ofNullable(ASTHelpers.getSymbol(node));
  }

  /** Return the method symbol for this constructor invocation or empty if none is found. */
  public static Optional<Symbol.MethodSymbol> getSymbol(NewClassTree node) {
    return Optional.ofNullable(ASTHelpers.getSymbol(node));
  }

  private Symbols() {
    // no instances
  }
}
