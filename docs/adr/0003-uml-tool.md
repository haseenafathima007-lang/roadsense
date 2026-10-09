# ADR 0003: Mermaid for UML and Architecture Diagrams
Status: accepted. Context: architecture, class relationships (OOP mapping), and pipeline lifecycles require visual diagrams in documentation. Tools considered: PlantUML and Mermaid.
Decision: standardize on Mermaid for all UML and sequence diagrams stored in docs/uml/ and design documentation.
Consequences: diagrams render natively in GitHub, markdown editors, and Antigravity artifacts without requiring external Graphviz binaries or Java rendering daemons; plain-text diagram sources remain easily diffable and maintainable in git.
