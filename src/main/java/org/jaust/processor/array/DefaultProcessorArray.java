package org.jaust.processor.array;

import org.jaust.Processor;
import org.jaust.processor.ProcessorArray;
import java.util.Objects;

public class DefaultProcessorArray implements ProcessorArray {
    private final Processor[] array;

    private DefaultProcessorArray(Processor[] array) {
        this.array = array.clone();
    }

    public static ProcessorArray of(Processor... processors) {
        return new DefaultProcessorArray(processors);
    }

    public int length() {
        return array.length;
    }

    public Processor at(int index) {
        return array[index];
    }

    public Processor[] toArray() {
        return array.clone();
    }

    public ProcessorArray prepend(Processor p) {
        Objects.requireNonNull(p, "processor cannot be null");
        Processor[] out = new Processor[array.length + 1];
        out[0] = p;
        System.arraycopy(array, 0, out, 1, array.length);
        return new DefaultProcessorArray(out);
    }

    public ProcessorArray append(Processor p) {
        Objects.requireNonNull(p, "processor cannot be null");
        Processor[] out = new Processor[array.length + 1];
        System.arraycopy(array, 0, out, 0, array.length);
        out[array.length] = p;
        return new DefaultProcessorArray(out);
    }

    public ProcessorArray append(ProcessorArray pa) {
        Processor[] suffix = Objects.requireNonNull(pa, "processor array cannot be null").toArray();
        Processor[] out = new Processor[array.length + suffix.length];
        System.arraycopy(array, 0, out, 0, array.length);
        System.arraycopy(suffix, 0, out, array.length, suffix.length);
        return new DefaultProcessorArray(out);
    }
}
