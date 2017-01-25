/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.locator;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpSeedProvider implements SeedProvider
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleSeedProvider.class);

    public HttpSeedProvider(Map<String, String> args) {}

    private String[] getUrlList(Config conf)
    {
        String[] urls = conf.seed_provider.parameters.get("urls").split(",", -1);
        for (int i = 0; i < urls.length; i++)
        {
            urls[i] = urls[i].trim();
        }
        return urls;
    }

    private String getUrlContent(Config conf, String url) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try
        {
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200)
            {
                logger.warn(String.format("getSeeds from %s returned %d", url, conn.getResponseCode()));
                return null;
            }

            BufferedReader breader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = breader.readLine()) != null)
            {
                response.append(inputLine);
            }
            breader.close();

            return response.toString();
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
        finally
        {
            conn.disconnect();
        }
    }

    private Config loadConfig() throws AssertionError
    {
        Config conf;
        try
        {
            conf = DatabaseDescriptor.loadConfig();
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
        return conf;
    }

    public List<InetAddress> getSeeds()
    {

        Config conf = loadConfig();

        String[] urls = getUrlList(conf);

        // Check each URL for content
        String content = "";
        for (int i = 0; i < urls.length; i++)
        {
            try
            {
                content = getUrlContent(conf, urls[i]);
            }
            catch (IOException e) {}

            if (content != null)
            {
                break;
            }
        }

        String[] hosts = content.split(",", -1);
        List<InetAddress> seeds = new ArrayList<InetAddress>(hosts.length);
        for (String host : hosts)
        {
            try
            {
                seeds.add(InetAddress.getByName(host.trim()));
            }
            catch (UnknownHostException ex)
            {
                // not fatal... DD will bark if there end up being zero seeds.
                logger.warn("Seed provider couldn't lookup host {}", host);
            }
        }
        return Collections.unmodifiableList(seeds);
    }
}

