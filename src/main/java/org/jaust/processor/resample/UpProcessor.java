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
// At target time t, a source sample is returned only when t maps exactly to a source sample boundary;
// all other target times produce a zero value (zero-stuffing).
public record UpProcessor(Context context, Processor source) implements DefaultProcessor {

    public Signal.Type[] inType() { return new Signal.Type[]{}; }

    public Signal.Type[] outType() { return source.outType(); }

    public SignalArray apply(SignalArray signal) {
        SignalArray sourceOutput = source.apply();
        long srcFreq = source.context().frequency();
        long tgtFreq = context.frequency();
        Signal[] adapted = new Signal[sourceOutput.length()];
        for (int i = 0; i < sourceOutput.length(); i++) {
            Signal s = sourceOutput.at(i);
            adapted[i] = switch (s.type()) {
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public double doubleAt(long t) {
                        long num = t * srcFreq;
                        return (num % tgtFreq == 0) ? s.doubleAt(num / tgtFreq) : 0.0;
                    }
                };
                case INT -> new IntSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public int intAt(long t) {
                        long num = t * srcFreq;
                        return (num % tgtFreq == 0) ? s.intAt(num / tgtFreq) : 0;
                    }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public long longAt(long t) {
                        long num = t * srcFreq;
                        return (num % tgtFreq == 0) ? s.longAt(num / tgtFreq) : 0L;
                    }
                };
                case BOOL -> new BooleanSignal() {
                    public Context context() { return UpProcessor.this.context; }
                    public boolean boolAt(long t) {
                        long num = t * srcFreq;
                        return (num % tgtFreq == 0) && s.boolAt(num / tgtFreq);
                    }
                };
            };
        }
        return DefaultArray.a(adapted);
    }
}
