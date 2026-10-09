# ADR 0002: Java is the decision engine, Python is perception
Status: accepted. Decision: Python exposes one stateless detection endpoint; all decisions, persistence, lifecycle and UI are Java.
Consequences: more Java logic, simpler Python, pure-Java ONNX inference possible later behind `DamageDetector` (decision gate, design §8).
