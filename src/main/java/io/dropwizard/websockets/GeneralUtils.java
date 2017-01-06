/**
 * The MIT License
 * Copyright (c) 2017 LivePerson, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.dropwizard.websockets;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneralUtils {
    
    @FunctionalInterface
    public interface ConsumerCheckException<T> {
        void accept(T elem) throws Exception;
    }

    @FunctionalInterface
    public interface RunnableCheckException {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface SupplierCheckException<T> {
        T get() throws Exception;
    }

    public static <T> Consumer<T> rethrow(ConsumerCheckException<T> c) {
        return t -> {
            try {
                c.accept(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
    public static <T> Supplier<T> rethrow(SupplierCheckException<T> c) {
        return () -> {
            try {
                return c.get();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
    public static Runnable rethrow(RunnableCheckException r) {
        return () -> {
            try {
                r.run();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}
