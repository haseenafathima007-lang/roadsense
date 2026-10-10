package com.roadai.analysis;

import com.roadai.config.Thresholds;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Defect;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.Observation;
import com.roadai.domain.Report;
import com.roadai.geo.Haversine;
import com.roadai.geo.SpatialIndex;
import com.roadai.lifecycle.MachineState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class DefectAssembler {

  private final double matchRadiusM;
  private final double accuracyFactor;
  private final double maxRadiusM;
  private final double manualPinRadiusM;
  private final double minSimilarity;
  private final ImageSimilarity similarityCalculator;
  private final CrackOrientationClassifier orientationClassifier;
  private final Function<String, Optional<Path>> imagePathResolver;
  private final BestViewSelector viewSelector;

  public DefectAssembler(
      Thresholds thresholds,
      ImageSimilarity similarityCalculator,
      CrackOrientationClassifier orientationClassifier) {
    this(thresholds, similarityCalculator, orientationClassifier, hash -> Optional.empty());
  }

  public DefectAssembler(
      Thresholds thresholds,
      ImageSimilarity similarityCalculator,
      CrackOrientationClassifier orientationClassifier,
      Function<String, Optional<Path>> imagePathResolver) {
    Objects.requireNonNull(thresholds, "Thresholds cannot be null");
    this.matchRadiusM = thresholds.dedupMatchRadiusM();
    this.accuracyFactor = thresholds.dedupAccuracyFactor();
    this.maxRadiusM = thresholds.dedupMaxRadiusM();
    this.manualPinRadiusM = thresholds.dedupManualPinRadiusM();
    this.minSimilarity = thresholds.dedupMinSimilarity();
    this.similarityCalculator =
        Objects.requireNonNull(similarityCalculator, "ImageSimilarity cannot be null");
    this.orientationClassifier =
        Objects.requireNonNull(orientationClassifier, "CrackOrientationClassifier cannot be null");
    this.imagePathResolver =
        Objects.requireNonNull(imagePathResolver, "ImagePathResolver cannot be null");
    this.viewSelector = new BestViewSelector(thresholds);
  }

  public AssemblerResult assemble(Report report, List<Defect> existingDefects) {
    Objects.requireNonNull(report, "Report cannot be null");
    Objects.requireNonNull(existingDefects, "Existing defects cannot be null");

    SpatialIndex index = new SpatialIndex();
    index.addAll(existingDefects);

    AssemblerResult result = new AssemblerResult();

    for (Observation obs : report.getObservations()) {
      boolean handled = false;

      if (report.getLocation().isPresent()) {
        LocationFix loc = report.getLocation().get();

        // 1. Calculate allowable radius
        double allowableRadius = calculateAllowableRadius(loc);

        // 2. Query candidates via spatial index
        List<Defect> candidates = index.findNearby(loc.point(), allowableRadius);

        for (Defect candidate : candidates) {
          // Branch 1: Family Match
          if (!isSameFamily(obs, candidate)) {
            continue;
          }

          // Branch 2: Distance check (inclusive boundary)
          double dist = Haversine.distanceM(loc.point(), candidate.getLocation().point());
          if (dist > allowableRadius + 1e-6) {
            continue;
          }

          // Check visual similarity if images exist
          Optional<Path> incomingPath = resolveImage(report.getFileHash());
          Optional<Path> existingPath = resolveCandidateImage(candidate);

          if (incomingPath.isPresent() && existingPath.isPresent()) {
            double sim = similarityCalculator.score(incomingPath.get(), existingPath.get());
            if (sim >= minSimilarity) {
              // Branch 3: MERGE
              candidate.addObservation(obs);
              result.addMerged(candidate);
              handled = true;
              break;
            } else {
              // Branch 4: Visually distinct, do not merge with this candidate
              continue;
            }
          } else {
            // Branch 5: Doubt / similarity unavailable -> NEEDS_REVIEW link (not a merge, not a
            // split)
            Defect reviewDefect = createNewDefect(obs, report);
            reviewDefect.setMachineState(MachineState.NEEDS_REVIEW);
            result.addReviewLink(
                new ReviewLink(
                    reviewDefect,
                    candidate,
                    "Image similarity unavailable for candidate within radius"));
            handled = true;
            break;
          }
        }
      }

      if (!handled) {
        // No match / distinct defect -> clean new defect
        Defect newDefect = createNewDefect(obs, report);
        result.addNewDefect(newDefect);
      }
    }

    return result;
  }

  public double calculateAllowableRadius(LocationFix loc) {
    if (loc.source() == LocationSource.MANUAL_PIN) {
      return manualPinRadiusM;
    }
    if (loc.accuracyM() != null) {
      return Math.min(maxRadiusM, matchRadiusM + (accuracyFactor * loc.accuracyM()));
    }
    return matchRadiusM;
  }

  private boolean isSameFamily(Observation obs, Defect candidate) {
    DamageClass obsClass = obs.damageClass();
    DamageClass candidateClass = candidate.getDamageClass();

    if (obsClass == DamageClass.CRACK_LINEAR && candidateClass == DamageClass.CRACK_LINEAR) {
      return true; // D00 and D10 belong to the same linear crack family
    }

    return obsClass == candidateClass;
  }

  private Optional<Path> resolveImage(String fileHash) {
    if (fileHash == null || fileHash.isEmpty()) {
      return Optional.empty();
    }
    return imagePathResolver.apply(fileHash).filter(Files::exists);
  }

  private Optional<Path> resolveCandidateImage(Defect candidate) {
    if (candidate.getObservations().isEmpty()) {
      return Optional.empty();
    }
    Optional<BestViewResult> best = viewSelector.selectBest(candidate.getObservations());
    String hash =
        best.map(b -> b.observation().fileHash())
            .orElseGet(() -> candidate.getObservations().get(0).fileHash());
    return resolveImage(hash);
  }

  private Defect createNewDefect(Observation obs, Report report) {
    LocationFix loc = report.getLocation().orElse(null);
    String id = UUID.randomUUID().toString();
    Defect defect;

    switch (obs.damageClass()) {
      case POTHOLE -> defect = new com.roadai.domain.Pothole(id, loc);
      case ALLIGATOR -> defect = new com.roadai.domain.AlligatorCrack(id, loc);
      case CRACK_LINEAR -> {
        CrackOrientation orientation = orientationClassifier.decide(obs);
        if (orientation == CrackOrientation.TRANSVERSE) {
          defect = new com.roadai.domain.TransverseCrack(id, loc);
        } else {
          defect = new com.roadai.domain.LongitudinalCrack(id, loc);
        }
      }
      default -> defect = new com.roadai.domain.Pothole(id, loc);
    }

    defect.addObservation(obs);
    return defect;
  }
}
