/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.slf4j.impl;

import java.io.PrintStream;

/**
 */
public class PrintStreamHolder {
    private static ThreadLocal<PrintStream> threadLocal = new ThreadLocal<>();
    private static PrintStream defaultPrintStream = System.out;

    public static PrintStream getPrintStream() {
        PrintStream printStream = threadLocal.get();
        if (printStream == null) {
            System.out.println("PrintStreamHolder: No PrintStream on thread " + Thread.currentThread() + " so using default");
            printStream = defaultPrintStream;
        }
        return printStream;
    }

    public static void setPrintStream(PrintStream out) {
        if (out != null) {
            threadLocal.set(out);
            defaultPrintStream = out;
        }
    }
}
