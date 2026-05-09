package org.jaust.signal.array;

import org.jaust.Signal;
import org.jaust.signal.SignalArray;
import java.util.Objects;
import java.util.function.Function;

public class DefaultArray implements SignalArray {
    private final Signal[] array;

    private DefaultArray(Signal[] array) {
        this.array = array.clone();
    }

    public static SignalArray a(Signal... signals) {
        return new DefaultArray(signals);
    }

    public int length() {
        return array.length;
    }

    public Signal at(int index) {
        return array[index];
    }

    public Signal[] toArray() {
        return array.clone();
    }

    public SignalArray prepend(Signal signal) {
        Signal[] out = new Signal[array.length + 1];
        out[0] = signal;
        System.arraycopy(array, 0, out, 1, array.length);
        return new DefaultArray(out);
    }

    public SignalArray prepend(SignalArray signal) {
        Signal[] prefix = Objects.requireNonNull(signal, "signal array cannot be null").toArray();
        Signal[] out = new Signal[prefix.length + array.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(array, 0, out, prefix.length, array.length);
        return new DefaultArray(out);
    }

    public SignalArray append(Signal signal) {
        Signal[] out = new Signal[array.length + 1];
        System.arraycopy(array, 0, out, 0, array.length);
        out[array.length] = signal;
        return new DefaultArray(out);
    }

    public SignalArray append(SignalArray signal) {
        Signal[] suffix = Objects.requireNonNull(signal, "signal array cannot be null").toArray();
        Signal[] out = new Signal[array.length + suffix.length];
        System.arraycopy(array, 0, out, 0, array.length);
        System.arraycopy(suffix, 0, out, array.length, suffix.length);
        return new DefaultArray(out);
    }

    public SignalArray map(Function<Signal, Signal> func) {
        Objects.requireNonNull(func, "mapping function cannot be null");
        Signal[] out = new Signal[array.length];
        for (int i = 0; i < array.length; i++) {
            out[i] = func.apply(array[i]);
        }
        return new DefaultArray(out);
    }
}
