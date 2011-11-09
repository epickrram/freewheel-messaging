package com.epickrram.freewheel.messaging.ptp;

import com.epickrram.freewheel.messaging.MessagingException;
import com.epickrram.freewheel.messaging.MessagingService;
import com.epickrram.freewheel.messaging.Receiver;
import com.epickrram.freewheel.messaging.ReceiverRegistry;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.server.embedded.EmbeddedHornetQ;
import org.hornetq.spi.core.remoting.ConnectorFactory;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;

// TODO - split into publishing service, subscribing service?
public final class PointToPointMessagingService implements MessagingService
{
    private final ReceiverRegistry receiverRegistry = new ReceiverRegistry();
    private final EndPoint endPoint;
    private final ServiceType serviceType;

    public PointToPointMessagingService(final EndPoint endPoint, final ServiceType serviceType)
    {
        this.endPoint = endPoint;
        this.serviceType = serviceType;
    }

    @Override
    public void send(final int topicId, final ByteArrayOutputStream byteArrayOutputStream) throws MessagingException
    {

    }

    @Override
    public void registerReceiver(final int topicId, final Receiver receiver)
    {
        // TODO set up listener
        receiverRegistry.registerReceiver(topicId, receiver);
    }

    @Override
    public void start() throws MessagingException
    {
        switch (serviceType)
        {
            case PUBLISH:
                createPublishMessagingService();

        }
    }

    private void createPublishMessagingService()
    {
        Configuration config = new ConfigurationImpl();
        HashSet<TransportConfiguration> transports = new HashSet<TransportConfiguration>();

        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        final HashMap<String, TransportConfiguration> connectorConfig = new HashMap<String, TransportConfiguration>();
        connectorConfig.put("connector", new TransportConfiguration(NettyConnectorFactory.class.getName()));

        config.setConnectorConfigurations(connectorConfig);
        

        EmbeddedHornetQ server = new EmbeddedHornetQ();

        server.setConfiguration(config);

    }

    @Override
    public void shutdown() throws MessagingException
    {
    }
}
