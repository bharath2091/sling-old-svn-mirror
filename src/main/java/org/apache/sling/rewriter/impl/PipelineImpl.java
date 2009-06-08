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
package org.apache.sling.rewriter.impl;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.PipelineConfiguration;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.Transformer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Context for a pipeline invocation.
 * This contexts keeps track of the used pipeline components for later disposal.
 */
public class PipelineImpl implements Processor {

    /** Empty array of transformers. */
    private static final Transformer[] EMPTY_TRANSFORMERS = new Transformer[0];

    /** The starting point of the pipeline. */
    private Generator generator;

    /** The first component in the pipeline after the generator */
    private ContentHandler firstContentHandler;

    /** The factory cache. */
    private final FactoryCache factoryCache;

    /**
     * Setup this pipeline.
     */
    public PipelineImpl(final FactoryCache   factoryCache) {
        this.factoryCache = factoryCache;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessorConfiguration)
     */
    public void init(ProcessingContext processingContext,
                     ProcessorConfiguration c)
    throws IOException {
        final PipelineConfiguration config = (PipelineConfiguration)c;
        final ProcessingComponentConfiguration[] transformerConfigs = config.getTransformerConfigurations();

        // create components and initialize them

        // lets get custom rewriter transformers
        final Transformer[][] rewriters = this.factoryCache.getRewriterTransformers();

        final ProcessingComponentConfiguration generatorConfig = config.getGeneratorConfiguration();
        final Generator generator = this.getPipelineComponent(Generator.class, generatorConfig.getType());
        generator.init(processingContext, generatorConfig);

        final int transformerCount = (transformerConfigs == null ? 0 : transformerConfigs.length) + rewriters[0].length + rewriters[1].length;
        final Transformer[] transformers;
        if ( transformerCount > 0 ) {
            // add all pre rewriter transformers
            transformers = new Transformer[transformerCount];
            int index = 0;
            for(int i=0; i< rewriters[0].length; i++) {
                transformers[index] = rewriters[0][i];
                transformers[index].init(processingContext, ProcessingComponentConfigurationImpl.EMPTY);
                index++;
            }
            if ( transformerConfigs != null ) {
                for(int i=0; i< transformerConfigs.length;i++) {
                    transformers[index] = this.getPipelineComponent(Transformer.class, transformerConfigs[i].getType());
                    transformers[index].init(processingContext, transformerConfigs[i]);
                    index++;
                }
            }
            for(int i=0; i< rewriters[1].length; i++) {
                transformers[index] = rewriters[1][i];
                transformers[index].init(processingContext, ProcessingComponentConfigurationImpl.EMPTY);
                index++;
            }
        } else {
            transformers = EMPTY_TRANSFORMERS;
        }

        final ProcessingComponentConfiguration serializerConfig = config.getSerializerConfiguration();
        final Serializer serializer = this.getPipelineComponent(Serializer.class, serializerConfig.getType());
        serializer.init(processingContext, serializerConfig);

        ContentHandler pipelineComponent = serializer;
        // now chain pipeline
        for(int i=transformers.length; i>0; i--) {
            transformers[i-1].setContentHandler(pipelineComponent);
            pipelineComponent = transformers[i-1];
        }

        this.firstContentHandler = pipelineComponent;
        generator.setContentHandler(this.firstContentHandler);
    }

    /**
     * Lookup a pipeline component.
     */
    @SuppressWarnings("unchecked")
    private <ComponentType> ComponentType getPipelineComponent(Class<ComponentType> typeClass,
                                                               String type)
    throws IOException {
        final ComponentType component;
        if ( typeClass == Generator.class ) {
            component = (ComponentType)this.factoryCache.getGenerator(type);
            // we keep the generator
            this.generator = (Generator)component;
        } else if ( typeClass == Transformer.class ) {
            component = (ComponentType)this.factoryCache.getTransformer(type);
        } else if ( typeClass == Serializer.class ) {
            component = (ComponentType)this.factoryCache.getSerializer(type);
        } else {
            component = null;
        }

        if ( component == null ) {
            throw new IOException("Unable to get component of class '" + typeClass + "' with type '" + type + "'.");
        }

        return component;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getWriter()
     */
    public PrintWriter getWriter() {
        return this.generator.getWriter();
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return this.firstContentHandler;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#finished()
     */
    public void finished() throws IOException {
        try {
            this.generator.finished();
        } catch (SAXException se) {
            if ( se.getCause() != null && se.getCause() instanceof IOException ) {
                throw (IOException)se.getCause();
            }
            final IOException ioe = new IOException("Pipeline exception.");
            ioe.initCause(se);
            throw ioe;
        }
    }
}
