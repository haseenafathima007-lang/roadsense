# Image-Only Redesign (Java-Centred): Features, Strategy and Connections 

Place in repo: `docs/03_image_only_java_redesign.md` Supersedes the video parts of: `01_upgrade_blueprint.md` , the PDF Feature & Implementation Strategy , and 

`02_java_centred_redesign.md` . Keeps from them: the Java-as-decision-engine principle, the OOP design, the priority score, the lifecycle state machines, and the foundation data fixes. Status discipline: everything below is a proposal. Nothing is implemented or measured yet. Library names are candidates to verify before use. 

## 1. The decision: input is still images only 

Video is removed. This changes what is possible, so the design changes with it rather than just deleting a feature. 

|Removed(video-only)|Whyitgoes|Replaced by|
|---|---|---|
|Video upload,GPX/CSVtrace,<br>time interpolation|No timeline|Locationfrom photoEXIF GPS,device GPS at<br>capture, or manualmap pin|
|ByteTrack/<br>`track_id` /|One frameper|Observations:each detectioninaphoto,grouped|
|`tracker.py`|photo|intodefectsbylocation + visualmatch|
|Closest-approach frame inside<br>atrack|No track|Best-view selectionacross thephotos ofthe<br>same defect, pluscaptureprotocol|
|Routetrace,distance-along-|No route|OSMroad linksand an optionalsystematic|
|routesegments||photo survey mode|
|"Coverage"froma driven trace|No trace|Proof-of-visit rule for verification photos|
|km-surveyed-per-hour metric|Not meaningful|Photo-to-ticket time, reports processedper<br>minute|



### What the image-only choice gives you 

- Matches the training data: RDD2022 and your Chennai set are single images, so there is no distribution shift from video frames. 

- Works with any phone and any photographer (engineer, contractor, citizen). 

- Far less engineering risk: no tracker, no time sync, no long jobs. Python shrinks to one endpoint. 

- Pure-Java inference becomes realistic (no tracker to port), see §8. 

- Easier to demo, to test with `FakeDetector` , and to explain. 

What it costs (state these honestly in the report) 

- No continuous coverage. Photos tend to be taken where damage is, so counts and condition are biased toward bad spots unless the systematic mode is used. 

- EXIF GPS can be missing, stripped by messaging apps, or wrong. 

- No frame-to-frame confirmation, so one-off false positives matter more. 

## 2. Roles of each layer (unchanged principle) 

### Python is the eyes, Java is the brain. 

|Layer|Language|Responsibility|
|---|---|---|
|Perception|Python (existing<br>YOLO+FastAPI)|`POST /v1/detect/image` →boxes,classes,confidence.<br>Stateless.|
|Decision<br>engine|Java|EXIF/location, qualitygate, observations →defects,dedup,<br>severity,condition, priority,OSM context, verification,lifecycle,<br>persistence, privacy, reports|
|Presentation|Java(JavaFX)|Upload, map, queue,defectgallery, verification,administration|
|Training/<br>evaluation|Python|Offlineonly|



3. Unique features (planned) 

|ID|Feature|Whatitis|Whyit matters|
|---|---|---|---|
|I1|Multi-photodedup<br>(replacesF1 tracking)|Severalphotos ofthesame defectbecome<br>one defect, using location toleranceplus<br>visualsimilarity|Countsdefects, not<br>photos; supports many<br>reporters|
|I2|Location pipeline|EXIF GPS→device GPS→ manualpin,<br>with<br>`location_source`and accuracy<br>stored|Works whenGPS is<br>missing; trustlevel is<br>explicit|
|I3|Imagequalitygate|Reject or flag blurred,dark, tiny, truncated<br>or wronglyframedphotosbefore analysis|Protects severityand<br>verificationfrombad<br>inputs|
|I4|Captureprotocol+<br>best-view selection|Standardshooting guide;best photo per<br>defectchosenby rule|Makesbox-areaseverity<br>more comparable(F5,<br>stillnodepth claims)|
|I5|Explainablepriority<br>(F3)|`PriorityFactor` product withper-<br>factorbreakdown, optional corroboration<br>factor|Engineer sees whyan<br>item rankshigh|
|I6|Proof-of-visit repair<br>verification (replaces<br>coverage-based F4)|Atargetedafter photo must passlocation,<br>scene-match andqualitychecks, thenan<br>engineer signs off|Honest verification<br>without videocoverage|
|I7|Two statemachines|Machinestate(from photos) separate<br>from workflow state(setby officers)|Cleanlifecycle,audit trail|
|I8|Road-linksummaries<br>(replaces segment<br>condition)|Snapdefects toOSMroad links; report<br>defects perlink.Condition ratingonlyin<br>thesystematicmode|Nofalse"road isGood"<br>frombiasedphotos|
|I9|Systematicphoto<br>survey mode(second<br>priority)|Surveyor photographsa chosen road link<br>atatarget spacing; segmentcondition<br>computedonly where enoughphotosexist|Gives real coveragewhen<br>needed|
|I10|Roles,audit, ranking-<br>stabilitycheck|`Surveyor` /<br>`Engineer` /<br>`Admin` ;every<br>transitionlogged; priority weights<br>perturbedto test top-Nstability|Accountabilityandtested<br>rankings|
|F6|Human-in-the-loop|Rejected detectionsbecome hard|Addressesfalsepositives|
||review (stretch)|negativesfor retraining|and domain shift|



Foundation fixes still apply: sequence-grouped split, excluded-class label audit, D10 mitigation (3-class + orientation), union-area damage index, `patch` hard-negative class, Chennai out-of-distribution set (now photos, with EXIF). 

## 4. Domain model (OOP) 

Four core concepts replace the video-era "track": 

|Concept|Meaning|
|---|---|
|`Report`|Oneuploadedphoto: file hash,capturetime,location (+source,accuracy),<br>reporter, quality result|
|`Observation`|One detectioninareport:class,confidence,box,framesize,crop|
|`Defect`|Areal-world defect:aggregates observations, owns severity, priority,lifecycle|
|`Verification`|An after photo tiedtoa defect:checks, outcome, who signedoff|



4.1 Package map 



<!-- Start of picture text -->
com.roadai<br>  domain/       Defect (abstract), Pothole, LongitudinalCrack,<br>TransverseCrack, AlligatorCrack,<br>                Report, Observation, Verification, Severity, DamageClass,<br>                value records: GeoPoint, BoundingBox, LocationFix(source,<br>accuracy), CaptureMeta<br>  perception/   DamageDetector (interface), RemoteYoloDetector, OnnxDetector<br>(stretch), FakeDetector<br>  imaging/      ExifReader (interface), MetadataExtractorReader, QualityGate<br>(interface),<br>                BlurGate, ExposureGate, SizeGate, FramingGate,<br>ImageSimilarity (interface),<br>                OrbSimilarity, Redactor (interface), BlurRedactor,<br>ExifStripper<br>  geo/          LocationResolver (chain: ExifLocation -> DeviceLocation -><br>ManualPin),<br>                Haversine, SpatialIndex, RoadLinkSnapper<br>  analysis/     DefectAssembler (observations -> defects), BestViewSelector,<br>                SeverityStrategy (interface), AreaRatioSeverity,<br>BestViewSeverity,<br>                UnionAreaCalculator, LinkSummaryBuilder, ConditionClassifier<br>(survey mode)<br>  priority/     PriorityFactor (interface), SeverityFactor, RoadClassFactor,<br>ExposureFactor,<br>                RecurrenceFactor, CorroborationFactor (optional),<br>PriorityCalculator,<br>                OsmContext (interface), CachedOsmContext<br>  lifecycle/    MachineState, WorkflowState, DefectStateMachine,<br>VerificationService,<br>                ProofOfVisitChecker, IllegalTransitionException<br>  persistence/  Repository<T,ID>, ReportRepository, DefectRepository,<br>VerificationRepository,<br>                AuditRepository, SqliteDatabase<br>  service/      ReportPipeline (template method), BatchUploadService,<br>ReportService<br>  security/     User (abstract), Surveyor, Engineer, Admin, Permission<br>  config/       Thresholds (immutable, loaded once)<br>  ui/           JavaFX views and view-models (no domain logic)<br><!-- End of picture text -->

4.2 Where each OOP concept appears 

|Concept|Location|
|---|---|
|Interfaces /<br>abstraction|`DamageDetector` ,<br>`ExifReader` ,<br>`QualityGate` ,<br>`ImageSimilarity` ,<br>`SeverityStrategy` ,<br>`PriorityFactor` ,<br>`Redactor` ,<br>`OsmContext` ,<br>`Repository<T,ID>`|
|Inheritance+<br>polymorphism|`Defect` subclasses (<br>`escalation()` ,<br>`weight()` );<br>`User` subclasses<br>(<br>`permissions()` )|
|Strategy|severity, priorityfactors,location sources, qualitygates|
|Chain of<br>Responsibility|`LocationResolver` (EXIF→device→ manual);a list of<br>`QualityGate` s<br>runin order|
|State|`DefectStateMachine`|
|Template Method|`ReportPipeline` fixedstageorder; variants (e.g. survey-modepipeline)<br>overridestages|
|Factory /Builder|`DetectorFactory` ,<br>`ReportBuilder`|
|Observer|`ProgressListener`forbatchuploads to the UI|
|Repository|`persistence/`|
|Encapsulation /<br>immutability|recordsfor valueobjects;guardedstate in<br>`Defect`|
|Generics,<br>collections, streams|`Repository<T,ID>` ,comparatorchainsfor ranking,<br>`groupingBy`forlink<br>summaries|
|Concurrency|`ExecutorService` /<br>`CompletableFuture`forbatchuploads|
|I/O,exceptions|image and EXIFreading,JSON,JDBCtransactions;customexception<br>hierarchy|



### 4.3 Skeletons (illustrative) 

java 

```
publicinterfaceDamageDetector{
List<Observation>detect(Pathimage)throwsApiException;// stateless pe
}
publicinterfaceQualityGate{
GateResultcheck(ImageInfoimage,List<Observation>observations);
}
publicinterfaceImageSimilarity{
doublescore(PathimageA,PathimageB);// 0..1 scene/o
}
publicinterfacePriorityFactor{
Stringname();
doublevalue(Defectdefect,Contextctx);// explains its
}
```

## 5. Implementation strategy 

### 5.1 Python contract (one endpoint) 

```
GET  /health                 -> {status, model, scheme}
POST /v1/detect/image        multipart: file
     -> {image:{w,h}, detections:[{class, conf, bbox:[x1,y1,x2,y2]}]}
POST /v1/detect/batch        (optional) multipart: files[] -> [{filename,
image, detections}]
```

Auth header, upload size/type limits and error shape stay as in the scaffold. Everything else is Java. D00 vs D10 orientation is decided in Java ( `CrackOrientationClassifier` ), working from the detector's `crack` label. 

### 5.2 Pipeline in Java (template method) 

- `Photo(s) upload -> ExifStripper reads metadata first (time, GPS, camera), then marks image for stripped storage -> QualityGate chain         blur, exposure, resolution, framing (flag, don't silently drop) -> DamageDetector.detect()   Python service (or ONNX stretch) -> LocationResolver          EXIF -> device -> manual pin; store source + accuracy -> Redactor                  blur faces/plates in stored crops and images -> Observation(s) created -> DefectAssembler           match to nearby existing defects (location + visual); else new defect -> BestViewSelector          pick best observation per defect -> SeverityStrategy          from best view (Low/Medium/High) -> PriorityCalculator        product of factors with breakdown -> RoadLinkSnapper           attach OSM road link -> Repository                persist + audit` 

### 5.3 Dedup rule (I1) 

Match an incoming observation to an existing defect only if all hold: 

1. same crack family (D00/D10 are one family; alligator and pothole exact); 

2. distance ≤ `dedup.match_radius_m` widened by location accuracy ( `dedup.accuracy_factor` × reported accuracy, capped at `dedup.max_radius_m` ); manual pins use a larger, separate radius; 

3. visual similarity ≥ `dedup.min_similarity` when both photos exist and pass the quality gate; 

4. if similarity is unavailable or ambiguous → do not auto-merge; create `NEEDS_REVIEW` link for an engineer. 

Wrong merges hide defects and wrong splits double-count them, so the default for doubt is "needs review". All thresholds live in config and stay unvalidated until measured. 

### 5.4 Capture protocol and best view (I4) 

Starting assumptions to test, not standards: photographer stands at a consistent distance, phone at a consistent height and pitch, whole defect inside frame, defect in the lower-centre area, one extra wide shot for context. The app records distance bucket, height and pitch as optional `CaptureMeta` . 

Best-view rule: among a defect's observations, prefer non-truncated boxes that pass quality, with the largest box area and the highest position in the near zone; flag when only poor views exist. Severity uses the same area-ratio thresholds and escalation as the main 

document, applied to the best view. No physical depth or true size claims. A scale reference object is a stretch idea, not part of the base design. 

### 5.5 Location handling (I2) 

|Source|Notes|
|---|---|
|`EXIF`|Preferred.Candidate libraryfor reading:<br>`metadata-extractor` (verify).Often|
||missing if location tagging is off or thephoto went through amessenger.|
|`DEVICE`|If a futuremobile client sendscoordinates withtheupload.|
|`MANUAL_PIN`|Clickon map.Lowest trust; wider matchingradius; flagged inUI.|



Each report stores `location_source` and `location_accuracy_m` (nullable). Missing location is allowed; those defects are listed as "location needed" and cannot enter the ranked map queue. 

### 5.6 Verification by proof-of-visit (I6) 

A defect never becomes "fixed" because it was missing from a photo. Instead, an officer submits an after photo at the defect's location. The system checks: 

|Check|Passcondition|
|---|---|
|Time|capturetime later than therepair-claimed date(EXIFtime canbe edited;evidence, not<br>proof)|
|Location|within<br>`verify.visit_radius_m` ofthe defect,accuracy takenintoaccount|
|Scene|`ImageSimilarity` withthe beforephoto ≥<br>`verify.min_scene_similarity`|
|match|(samesurroundings)|
|Quality|passes thequalitygate andthe defectarea is plausiblyinframe|
|Detector|nodamageofthatfamilydetected in the frame at ≥<br>`verify.clear_conf` (patch class<br>usedsofresh asphaltis not flagged)|



Result → machine state: `AFTER_PHOTO_CLEAR` , `AFTER_PHOTO_STILL_DAMAGED` , or `AFTER_PHOTO_REJECTED` (failed location, scene or quality; reasons listed). `VERIFIED_FIXED` requires an engineer's workflow sign-off with a note. A later report at the same place after verification sets `REAPPEARED` and raises the recurrence factor. 

5.7 State machines (I7) 

Workflow state (engineer, audited) 

##### Machine state (system) 

|`NEW`|`UNREVIEWED`|
|---|---|
|`REPORTED_AGAIN` (corroborating|`CONFIRMED` /<br>`REJECTED`|
|photo)||
|`NEEDS_REVIEW` (ambiguous merge)|`REPAIR_ORDERED`|
|`AFTER_PHOTO_CLEAR`|`REPAIR_IN_PROGRESS`|
|`AFTER_PHOTO_STILL_DAMAGED`|`REPAIRED_CLAIMED`|
|`AFTER_PHOTO_REJECTED`|`VERIFIED_FIXED` (needsclearafter photo or sign-off<br>withnote)|



#### `REAPPEARED` 

Illegal transitions throw `IllegalTransitionException` ; every transition writes an audit row (who, when, from, to, note). 

### 5.8 Road links and survey mode (I8, I9) 

- Spot mode (default): snap each defect to the nearest cached OSM road link; show defects per link and on the map. No Good/Fair/Poor for a link, because photos are not systematic. 

- Survey mode (second priority): the surveyor picks a road link and photographs it at a target spacing. A segment gets a condition (union-area index and Good/Fair/Poor as in the main document) only if it has at least `survey.min_photos_per_segment` qualitypassing photos; otherwise it shows Not surveyed. Photos cover one lane only; say so in the UI. 

### 5.9 Priority (I5, I10) 

Unchanged formula from the PDF: severity × road weight × exposure × recurrence, implemented as composable `PriorityFactor` s. Optional `CorroborationFactor` (number of independent reports) shows how new factors plug in without editing existing code. Add a `PriorityStabilityTest` that perturbs weights and reports how stable the top-N ranking is. Weights are starting assumptions. 

### 5.10 Privacy 

Read EXIF first, then store images without EXIF; blur faces and plates before any image or crop is stored or exported; default to blurring more, not less; retention and delete options in the app. 

6. Revised phases 

|Phase|Goal|Change from thevideo-eraplan|Language|
|---|---|---|---|
|0|Repo standards|add Java build,UML folder|both|
|1|Data hardening|unchanged|Python|
|2|Training|unchanged;add<br>`patch`classfrom own photos|Python|
|3|Evaluation +<br>Chennai OOD|collection protocol becomesaphoto protocolwith EXIF<br>kept|Python|
|4|Perception +<br>inputs|Python:image endpoint only.Java:<br>`domain` ,<br>`perception` ,<br>`imaging` (EXIF, qualitygates),<br>`geo`<br>(LocationResolver)|Python +<br>Java|
|5|Analysis|Java:<br>`DefectAssembler` ,<br>`BestViewSelector` ,<br>severity, unionarea,dedup (imagesimilarity)|Java|
|6|Priority +OSM|Java:factors, road-linksnapper,cached OSM|Java|
|7|Persistence+<br>roles|repositories,audit, roles,config|Java|
|8|UI|batchupload, map, queue,defectgallery, "location needed"<br>list; survey modescreens (secondpriority)|Java|
|9|Verification|Java: proof-of-visitchecker, statemachines|Java|
|10|Hardening|privacy,expertkappa, priority stability,ONNX gate|both|
|11|Demo + report|UML,OOP doc, results sheet|Java +docs|



Removed from the plan: `tracker.py` , `gps.py` trace logic, `run_video.py` , video endpoints, GPX/CSV traces, time interpolation, coverage-by-trace. 

## 7. Connections to existing documents 

|Source|Item|Connection|
|---|---|---|
|Maindocument §3|Stages 1-4|Stage1+2 =detector<br>(Python);Stage3 =<br>severityfrombest view;<br>Stage4 =linksummary<br>/ survey-mode<br>condition (Java)|
|Maindocument §5|Severityand condition rules|Samethresholdsand<br>escalation;appliedto<br>best viewand,in<br>survey mode, to<br>segments|
|Maindocument §9|Limitations 1-5|Fixedorevidenced by<br>foundation fixes, union<br>area,expertkappa|
|Blueprint /PDF F1|Tracking+GPS+dedup|Replaced byI1 +I2<br>(multi-photodedup,<br>location pipeline)|
|PDF F2|Segmentcondition|BecomesI8/I9|
|PDF F3|Priority|Kept (I5)|
|PDF F4|Verification|BecomesI6 (proof-of-<br>visit)|
|PDF F5|Calibratedseverity|Kept;best view +<br>captureprotocol+<br>expert ratings|
|`02_java_centred_redesign.md`|Java engine, patterns, state<br>machines|Kept; video-specific<br>classes removed|
|Scaffold|`ARCHITECTURE` ,<br>`CONTRACTS` ,<br>`ROADMAP` , prompts 3-9 + 11,<br>`thresholds.yaml` ,|Needupdating(§9)|
||`make_demo_data.py` ,<br>`sample-`<br>`data/`||



## 8. Decision gate: pure-Java inference (now more realistic) 

Without a tracker, running YOLO in Java needs: ONNX export + parity check (Python, small), image letterboxing, output decoding and NMS in Java (moderate, well defined), and the `OnnxDetector` class behind the existing interface. Candidate runtime: ONNX Runtime Java API; verify platform and Apple Silicon support, and measure speed before committing. Gate: start after Phases 4-9 pass verification. If it does not match the Python detector's output on a parity set by the gate date, keep `RemoteYoloDetector` . No other code changes either way. 

## 9. Impact on existing scaffold files (to do) 

|File|Update|
|---|---|
|`docs/ARCHITECTURE.md` ,<br>`CONTRACTS.md` ,<br>`ROADMAP.md`|Replacevideo pipeline/APIwith§5; newdomain<br>tables (reports, observations,defects,<br>verifications,audit)|
|`docs/prompts/phase-03`|Photocaptureprotocol insteadofvideo +GPX|
|`docs/prompts/phase-04` to<br>`09` ,<br>`11`|Rewrite forimage-only,Java-centred design|
|`ai-service/app/`|Keep<br>`detector.py` (image),<br>`main.py` ,<br>`schemas.py` ;drop tracker/gps trace;add<br>endpointlimits only|
|`ai-`|Remove<br>`tracking.*` ,<br>`segments.length_m`|
|`service/app/config/thresholds.yaml`|(survey modeonly), trace GPS keys;add|
||`dedup.*` ,<br>`quality.*` ,<br>`verify.*` ,<br>`survey.*`|
|`ai-service/scripts/make_demo_data.py` ,<br>`sample-data/`|Regenerate(see§10)|
|`docs/DEMO_PLAYBOOK.md`|Replaceroute/tracestory withphoto-report story|
|`AGENTS.md` ,<br>`.agents/rules/java-app.md`|Add OOPrules; removevideo wording|
|New|`docs/OOP_DESIGN.md` ,<br>`docs/uml/`|



## 10. Demo plan (image-only) 

|Mode|Data|Honesty rule|
|---|---|---|
|Live|15-30 ofyour ownChennaiphotos with EXIF|Quoteonly numbersfrom<br>`RESULTS.md`|
||GPS, run throughtherealmodel||
|Offline|Real RDD2022 test-splitimages withsynthetic|Banner "SYNTHETIC LOCATIONS/|
||coordinatesandreportdatesassigned along a|DEMO DATA"always visible;imagesare|
||made-up route;before/after pairsfor verification|real,locationsand datesarenot|



Story: reports arrive from several people; duplicates merge into one defect; ranked queue; an after photo passes proof-of-visit; one repaired defect reappears in a later report and its priority rises. For the verification pair, use a before photo and, if you cannot photograph a real repair, label the "after" as staged or simulated. Never present staged pairs as measured. 

## 11. How this makes the project better 

|Dimension|Videodesign|Image-onlydesign|
|---|---|---|
|Fit withtraining<br>data|Framesdifferfrom<br>RDD2022images|Same kindof inputas training|
|Engineeringrisk|Tracker, timesync,long<br>jobs|One image call; simple jobs|
|Whocancontribute|Needsamountedphone<br>and GPS log|Any photofromany phone|
|Dedup|Tracks +GPS|Location + visualmatch+ reviewfallback|
|Verification|Coverage fromatrace|Proof-of-visitchecks plusengineer sign-off|
|Honestyabout<br>coverage|Implicit|Explicit: spot modeshowsdefects only; ratings<br>only wheresurveyed|
|Javafit|HeavyPython tracking|More Java logic, simplerPython,feasible Java<br>inference|
|Testability|Needs video fixtures|Imagefixtures +<br>`FakeDetector`|
|Weakerarea|Throughput metric|Nocontinuouscoverage;EXIF GPS canbe<br>missingor spoofed|



## 12. Risks 

|Risk|Mitigation|
|---|---|
|Photobias toward badspots|Spot modereportsdefects only; survey mode for ratings; stated<br>in report|
|Missingor wrong GPS|Location sources with accuracy; manualpin; "location needed"<br>list|
|Wrongmerges or splits|Reviewfallback; thresholds measuredonlabelledphoto pairs|
|Spoofedoredited EXIF|Treatasevidence; scenematch;engineer sign-off;audit trail|
|Falsepositives withone frame|Qualitygate, review workflow, patch class, optional F6|
|Repairafter photo notavailable for<br>demo|Labelstagedpairsclearly;collect onerealpairifpossible|
|Over-engineeredpatterns|Eachpattern must serve a listed extension point|
|Rubricunknown|Asksupervisorfor the OOPmarkingscheme andmap §4.2 toit|



## 13. Measurement plan (all NOT YET MEASURED) 

|Metric|Source|Status|
|---|---|---|
|Per-class precision/recall/mAPon|`docs/test_metrics.json`|NOT YET|
|groupedtest split||MEASURED|
|SameonChennaiphoto set|`docs/chennai_ood_metrics.json`|NOT YET<br>MEASURED|
|Shareofown photos withusable EXIF<br>GPS|own photo set|NOT YET<br>MEASURED|
|Quality-gaterejection rate and<br>agreement with humanjudgment|own photo set|NOT YET<br>MEASURED|
|Dedup precision/recallonlabelled|hand-labelledpairs|NOT YET|
|same/differentdefect photo pairs||MEASURED|
|Severityagreement vsexperts (Cohen's|ratingsheets|NOT YET|
|kappa)||MEASURED|
|Priority top-Nstability under weight|`PriorityStabilityTest`|NOT YET|
|perturbation||MEASURED|
|Proof-of-visitaccuracy on real|fieldpairs|NOT YET|
|before/after pairs||MEASURED|
|Time from photo uploadto ranked|stopwatch/benchmark|NOT YET|
|ticket||MEASURED|
|JUnit testcountand coverage|`mvn verify`|NOT YET<br>MEASURED|



## 14. One-line pitch (updated) 

Anyone can photograph a road defect; a Java decision engine locates it, counts it once, ranks what to fix first with an explainable score, and checks a later photo before calling it repaired. 

