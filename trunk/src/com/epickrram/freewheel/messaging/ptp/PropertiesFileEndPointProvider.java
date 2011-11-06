package com.epickrram.freewheel.messaging.ptp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.epickrram.freewheel.util.IoUtil.close;

public final class PropertiesFileEndPointProvider implements EndPointProvider
{
    private final Map<String, EndPoint> endPointsByClassnameMap = new HashMap<String, EndPoint>();
    private final String resourceName;
    private boolean initialised;

    public PropertiesFileEndPointProvider(final String resourceName)
    {
        this.resourceName = resourceName;
    }

    @Override
    public synchronized EndPoint resolveEndPoint(final Class descriptor)
    {
        if(!initialised)
        {
            initialise();
        }
        return endPointsByClassnameMap.get(descriptor.getName());
    }

    private void initialise()
    {
        final InputStream inputStream = Thread.currentThread().getContextClassLoader().
                getResourceAsStream(resourceName);

        if(inputStream == null)
        {
            throw new IllegalArgumentException("Cannot find classpath resource: " + resourceName);
        }
        final Map<String, EndPointBuilder> classnameToEndPointBuilderMap =
                new HashMap<String, EndPointBuilder>();

        parseResource(inputStream, classnameToEndPointBuilderMap);

        try
        {
            for (Map.Entry<String, EndPointBuilder> entry : classnameToEndPointBuilderMap.entrySet())
            {
                endPointsByClassnameMap.put(entry.getKey(), entry.getValue().newInstance());
            }
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("Cannot resolve endpoint host", e);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Cannot parse endpoint port", e);
        }

        initialised = true;
    }

    private void parseResource(final InputStream inputStream, final Map<String, EndPointBuilder> classnameToEndPointBuilderMap)
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while((line = reader.readLine()) != null)
            {
                if(line.startsWith("endPoint"))
                {
                    if(line.contains(".port="))
                    {
                        addPortConfig(line, getBuilder(line, classnameToEndPointBuilderMap));
                    }
                    else if(line.contains(".host="))
                    {
                        addHostConfig(line, getBuilder(line, classnameToEndPointBuilderMap));
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read from classpath resource", e);
        }
        finally
        {
            close(reader);
        }
    }

    private void addPortConfig(final String line, final EndPointBuilder endPointBuilder)
    {
        endPointBuilder.port(line.substring(line.indexOf('=') + 1));
    }

    private void addHostConfig(final String line, final EndPointBuilder endPointBuilder)
    {
        endPointBuilder.host(line.substring(line.indexOf('=') + 1));
    }

    private EndPointBuilder getBuilder(final String line, final Map<String, EndPointBuilder> classnameToEndPointBuilderMap)
    {
        final String classname = getClassname(line);
        EndPointBuilder builder = classnameToEndPointBuilderMap.get(classname);
        if(builder == null)
        {
            builder = new EndPointBuilder();
            classnameToEndPointBuilderMap.put(classname, builder);
        }
        return builder;
    }

    private String getClassname(final String line)
    {
        return line.substring("endPoint.".length(), line.indexOf('=') - 5);
    }

    private static final class EndPointBuilder
    {
        private String host;
        private String port;

        void host(final String hostname)
        {
            this.host = hostname;
        }

        void port(final String port)
        {
            this.port = port;
        }

        EndPoint newInstance() throws NumberFormatException, UnknownHostException
        {
            validate();
            return new EndPoint(InetAddress.getByName(host), Integer.parseInt(port));
        }

        private void validate()
        {
            if(port == null || host == null)
            {
                throw new IllegalArgumentException("Both host and port must be specified");
            }
        }
    }
}
