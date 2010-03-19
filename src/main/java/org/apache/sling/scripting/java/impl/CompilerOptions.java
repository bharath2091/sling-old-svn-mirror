/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.java.impl;

import java.util.Dictionary;

import org.apache.sling.commons.compiler.Options;

public class CompilerOptions extends Options {

    private String encoding;

    /**
     * Create an compiler options object using data available from
     * the component configuration.
     */
    public static CompilerOptions createOptions(final Dictionary<String, Object> props) {
        CompilerOptions opts = new CompilerOptions();

        final Boolean classDebugInfo = (Boolean)props.get(JavaScriptEngineFactory.PROPERTY_CLASSDEBUGINFO);
        opts.setGenerateDebugInfo(classDebugInfo != null ? classDebugInfo : true);

        final String sourceVM = (String) props.get(JavaScriptEngineFactory.PROPERTY_COMPILER_SOURCE_V_M);
        opts.setSourceVersion(sourceVM != null && sourceVM.length() > 0 ? sourceVM : JavaScriptEngineFactory.DEFAULT_VM_VERSION);

        final String encoding = (String) props.get(JavaScriptEngineFactory.PROPERTY_ENCODING);
        opts.encoding = encoding != null && encoding.length() > 0 ? encoding : "UTF-8";

        return opts;
    }

    public String getJavaEncoding() {
        return this.encoding;
    }
}
