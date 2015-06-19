package com.splunk.logging;
/*
 * Copyright 2013-2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "Http", category = "Core", elementType = "appender", printObject = true)
public final class HttpEventCollectorLog4jAppender extends AbstractAppender
{
    private HttpEventCollectorLog4jAppender(final String name,
                         final String url,
                         final String token,
                         final String source,
                         final String sourcetype,
                         final String index,
                         final Filter filter,
                         final Layout<? extends Serializable> layout,
                         final boolean ignoreExceptions,
                         long batchInterval,
                         long batchCount,
                         long batchSize,
                         long retriesOnError,
                         final String disableCertificateValidation)
    {
        super(name, filter, layout, ignoreExceptions);

        if (!HttpEventCollectorMiddleware.hasMiddleware()) {
            Dictionary<String, String> metadata = new Hashtable<String, String>();
            metadata.put(HttpEventCollectorSender.MetadataIndexTag, index != null ? index : "");
            metadata.put(HttpEventCollectorSender.MetadataSourceTag, source != null ? source : "");
            metadata.put(HttpEventCollectorSender.MetadataSourceTypeTag, sourcetype != null ? sourcetype : "");

            HttpEventCollectorSender sender = new HttpEventCollectorSender(url, token, batchInterval, batchCount, batchSize, retriesOnError, metadata);

            if (disableCertificateValidation.equalsIgnoreCase("true")) {
                sender.disableCertificateValidation();
            }

            HttpEventCollectorMiddleware.setMiddleware(sender);
        }
    }

    /**
     * Create a Http Appender.
     * @return The Http Appender.
     */
    @PluginFactory
    public static HttpEventCollectorLog4jAppender createAppender(
            // @formatter:off
            @PluginAttribute("url") final String url,
            @PluginAttribute("token") final String token,
            @PluginAttribute("name") final String name,
            @PluginAttribute("source") final String source,
            @PluginAttribute("sourcetype") final String sourcetype,
            @PluginAttribute("index") final String index,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginAttribute("batch_size_bytes") final String batchSize,
            @PluginAttribute("batch_size_count") final String batchCount,
            @PluginAttribute("batch_interval") final String batchInterval,
            @PluginAttribute("retries_on_error") final String retriesOnError,
            @PluginAttribute("disableCertificateValidation") final String disableCertificateValidation,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter
    )
    {
        if (name == null)
        {
            LOGGER.error("No name provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (url == null)
        {
            LOGGER.error("No Splunk URL provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (token == null)
        {
            LOGGER.error("No token provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (layout == null)
        {
            layout = PatternLayout.createLayout("%m", null, null, Charset.forName("UTF-8"), true, false, null, null);
        }

        final boolean ignoreExceptions = true;

        return new HttpEventCollectorLog4jAppender(
                name, url, token,
                source, sourcetype, index,
                filter, layout, ignoreExceptions,
                parseInt(batchInterval, 0), parseInt(batchCount, 0), parseInt(batchSize, 0),
                parseInt(retriesOnError, 0),
                disableCertificateValidation);
    }


    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event)
    {
        HttpEventCollectorMiddleware.send(
                event.getLevel().toString(),
                event.getMessage().getFormattedMessage()
        );
    }

    @Override
    public void stop() {
        HttpEventCollectorMiddleware.flush();
        super.stop();
    }
}
