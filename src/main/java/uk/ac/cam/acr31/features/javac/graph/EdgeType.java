package uk.ac.cam.acr31.features.javac.graph;

public enum EdgeType {
  NEXT_TOKEN,
  CHILD,
  NONE,
  LAST_WRITE,
  LAST_USE,
  COMPUTED_FROM,
  RETURNS_TO,
  FORMAL_ARG_NAME,
  GUARDED_BY,
  GUARDED_BY_NEGATION,
  LAST_LEXICAL_USE
}
