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

import java.util.Optional;
import java.util.function.BiConsumer;

public class Optionals {

  /** Execute the given function if both optionals are present. */
  public static <U, V> void ifBothPresent(
      Optional<U> first, Optional<V> second, BiConsumer<U, V> consumer) {
    if (first.isPresent() && second.isPresent()) {
      consumer.accept(first.get(), second.get());
    }
  }

  private Optionals() {
    // no instances
  }
}
