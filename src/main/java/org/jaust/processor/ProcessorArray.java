package org.jaust.processor;

import org.jaust.Processor;
import java.util.function.BinaryOperator;

public interface ProcessorArray {
    int length();
    Processor at(int index);
    Processor[] toArray();

    ProcessorArray prepend(Processor p);
    ProcessorArray append(Processor p);
    ProcessorArray append(ProcessorArray pa);

    default Processor reduce(BinaryOperator<Processor> op) {
        if (length() == 0) throw new IllegalArgumentException("Cannot reduce empty ProcessorArray: at least one element required");
        Processor result = at(0);
        for (int i = 1; i < length(); i++) {
            result = op.apply(result, at(i));
        }
        return result;
    }
}
