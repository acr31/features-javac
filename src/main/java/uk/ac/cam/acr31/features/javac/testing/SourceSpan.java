package uk.ac.cam.acr31.features.javac.testing;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SourceSpan {

  abstract int start();

  abstract int end();

  public static SourceSpan create(int start, int end) {
    return new AutoValue_SourceSpan(start, end);
  }
}
