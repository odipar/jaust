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

// Downsamples every output signal of a source processor from its context frequency to the target context frequency.
// Each target sample is computed by averaging (smoothing) all source samples that fall within the corresponding
// target sample window [t*srcFreq/tgtFreq, (t+1)*srcFreq/tgtFreq). Boolean signals use majority voting.
public record DownProcessor(Context context, Processor source) implements DefaultProcessor {

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
                    public Context context() { return DownProcessor.this.context; }
                    public double doubleAt(long t) {
                        long srcStart = t * srcFreq / tgtFreq;
                        long srcEnd = (t + 1) * srcFreq / tgtFreq;
                        if (srcStart >= srcEnd) return s.doubleAt(srcStart);
                        double sum = 0.0;
                        for (long st = srcStart; st < srcEnd; st++) sum += s.doubleAt(st);
                        return sum / (srcEnd - srcStart);
                    }
                };
                case INT -> new IntSignal() {
                    public Context context() { return DownProcessor.this.context; }
                    public int intAt(long t) {
                        long srcStart = t * srcFreq / tgtFreq;
                        long srcEnd = (t + 1) * srcFreq / tgtFreq;
                        if (srcStart >= srcEnd) return s.intAt(srcStart);
                        long sum = 0;
                        for (long st = srcStart; st < srcEnd; st++) sum += s.intAt(st);
                        return (int) (sum / (srcEnd - srcStart));
                    }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return DownProcessor.this.context; }
                    public long longAt(long t) {
                        long srcStart = t * srcFreq / tgtFreq;
                        long srcEnd = (t + 1) * srcFreq / tgtFreq;
                        if (srcStart >= srcEnd) return s.longAt(srcStart);
                        long sum = 0;
                        for (long st = srcStart; st < srcEnd; st++) sum += s.longAt(st);
                        return (long) ((double) sum / (srcEnd - srcStart));
                    }
                };
                case BOOL -> new BooleanSignal() {
                    public Context context() { return DownProcessor.this.context; }
                    public boolean boolAt(long t) {
                        long srcStart = t * srcFreq / tgtFreq;
                        long srcEnd = (t + 1) * srcFreq / tgtFreq;
                        if (srcStart >= srcEnd) return s.boolAt(srcStart);
                        long count = srcEnd - srcStart;
                        long trueCount = 0;
                        for (long st = srcStart; st < srcEnd; st++) if (s.boolAt(st)) trueCount++;
                        return trueCount * 2 > count; // majority vote
                    }
                };
            };
        }
        return DefaultArray.a(adapted);
    }
}
