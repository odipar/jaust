package org.jaust.processor.resample;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.BooleanSignal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.IntSignal;
import org.jaust.signal.LongSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

// Upsamples every output signal of a source processor from its context frequency to the target context frequency.
// Uses rational arithmetic to correctly place source samples at their exact target positions for any ratio,
// including coprime frequencies. For each target time t, the nearest source index
// i = round(t * srcFreq / tgtFreq) is computed; it is emitted only when the round-trip
// round(i * tgtFreq / srcFreq) == t, ensuring each source sample appears exactly once.
// All other target times produce a zero value (zero-stuffing).
public record UpProcessor(Context context, Processor source) implements DefaultProcessor {

    public Signal.Type[] inType() { return new Signal.Type[]{}; }

    public Signal.Type[] outType() { return source.outType(); }

    public SignalArray apply(SignalArray signal) {
        SignalArray sourceOutput = source.apply();
        long srcFreq = source.context().frequency();
        long tgtFreq = context.frequency();
        // Half-step offsets for rounding (integer arithmetic): round(a/b) = (a + b/2) / b
        long halfSrc = srcFreq / 2;
        long halfTgt = tgtFreq / 2;
        Signal[] adapted = new Signal[sourceOutput.length()];
        for (int i = 0; i < sourceOutput.length(); i++) {
            Signal s = sourceOutput.at(i);
            adapted[i] = switch (s.type()) {
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public double doubleAt(long t) {
                        long si = (t * srcFreq + halfTgt) / tgtFreq;  // round(t*srcFreq/tgtFreq)
                        return ((si * tgtFreq + halfSrc) / srcFreq == t) ? s.doubleAt(si) : 0.0;
                    }
                };
                case INT -> new IntSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public int intAt(long t) {
                        long si = (t * srcFreq + halfTgt) / tgtFreq;
                        return ((si * tgtFreq + halfSrc) / srcFreq == t) ? s.intAt(si) : 0;
                    }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public long longAt(long t) {
                        long si = (t * srcFreq + halfTgt) / tgtFreq;
                        return ((si * tgtFreq + halfSrc) / srcFreq == t) ? s.longAt(si) : 0L;
                    }
                };
                case BOOL -> new BooleanSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public boolean boolAt(long t) {
                        long si = (t * srcFreq + halfTgt) / tgtFreq;
                        return ((si * tgtFreq + halfSrc) / srcFreq == t) && s.boolAt(si);
                    }
                };
            };
        }
        return DefaultArray.a(adapted);
    }
}
