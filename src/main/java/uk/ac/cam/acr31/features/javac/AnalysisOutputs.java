package uk.ac.cam.acr31.features.javac;

import org.checkerframework.dataflow.analysis.AnalysisResult;

class AnalysisOutputs {

  final AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites;
  final AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses;

  AnalysisOutputs(
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites,
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastUses) {
    this.lastWrites = lastWrites;
    this.lastUses = lastUses;
  }
}
