## Jaust

Jaust is a Java implementation of the concepts from the [Faust Programming Language](https://faust.grame.fr/).
Faust (**F**unctional **Au**dio **St**ream) is a functional programming language for real-time signal processing and synthesis,
described in detail on [Wikipedia](https://en.wikipedia.org/wiki/FAUST_(programming_language)).

## Faust Design

Faust models audio processing as a set of **block diagrams** composed together using five composition operators:

| Operator | Symbol | Description |
|----------|--------|-------------|
| Parallel | `,`    | Place two blocks side by side, combining their inputs and outputs |
| Sequential | `:`  | Connect the outputs of the first block to the inputs of the second |
| Split | `<:`   | Fan out the outputs of the first block to the inputs of the second |
| Merge | `>:`   | Sum the outputs of the first block into the inputs of the second |
| Recursive | `~` | Connect (some of) the outputs of a block back to its inputs with a one-sample delay |

In Faust, these operators are expressed using a domain-specific language (DSL) that is compiled to highly optimized C++.
Each Faust program ultimately describes a pure mathematical function from a stream of input samples to a stream of output samples.

## Jaust Design

Jaust brings the same block-diagram algebra into Java as a fluent, embedded API — no separate DSL or compiler required.
Each `Processor` represents a Faust block, and the five composition operators are first-class Java methods:

```java
// par  (,)  — parallel composition
Processor par(Processor... processors);

// seq  (:)  — sequential composition
Processor seq(Processor... processors);

// div  (<:) — split
Processor div(Processor p2);

// avg  (>:) — merge
Processor avg(Processor p2);

// rec  (~)  — recursive composition (one-sample feedback)
Processor rec(Processor p2);
```

Signals are **lazy** and **index-based**: a `Signal` is queried at any arbitrary sample index `t` via `doubleAt(t)`,
`intAt(t)`, etc., with no hidden mutable state. This makes the model purely functional in spirit.

### Example

```java
Context c = new DefaultContext(44100);

// Feedback counter: out(t) = out(t-1) + 1.0
Processor p1      = c.addD();
Processor p2      = c.cache(c.wire(Signal.Type.DOUBLE));
Processor rec     = c.rec(p1, p2);
Processor counter = c.seq(c.valD(1.0), rec);

Signal s = counter.apply().at(0);
System.out.println(s.doubleAt(9));  // → 10.0
```

## Jaust vs. Faust — Pros and Cons

### Jaust pros
- **Pure Java** — no external toolchain, compiler, or C++ build step; works anywhere the JVM runs.
- **Embedded DSL** — block diagrams are composed using ordinary Java method calls and benefit from IDE auto-complete, refactoring, and type checking.
- **Arbitrary-index access** — signals can be evaluated at any sample index without computing all preceding samples, enabling random access and parallelism.
- **Easy integration** — Jaust processors are plain Java objects that fit naturally into existing Java/Kotlin/Scala projects.
- **Multi-type signals** — supports `BOOL`, `INT`, `LONG`, and `DOUBLE` signal types natively.

### Jaust cons
- **Performance** — Faust compiles to highly optimized C++ code; Jaust runs on the JVM, which may still lag behind native C++ for the most demanding real-time audio workloads, though modern JVMs (including the Vector API) continue to narrow this gap.
- **No GUI or hardware bindings** — Faust can generate standalone applications with sliders/knobs and direct JACK/CoreAudio/ALSA bindings; Jaust provides none of these out of the box.
- **No DSP standard library** — Faust ships a large curated library of ready-made building blocks (filters, oscillators, effects, etc.); Jaust currently provides only the core composition primitives, so common DSP algorithms must be hand-coded in Java.

## Attribution

**Design** by the author.  
**Implementation** by GitHub Copilot (GHCP) and the author.
