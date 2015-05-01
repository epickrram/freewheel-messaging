# PTP Messaging #

Freewheel uses [netty](http://www.jboss.org/netty) under the hood for non-blocking IO tcp connectivity.
To create a `MessagingService` that uses point-to-point connections, use the `MessagingContextFactory`:
```
final MessagingContext ctx = factory.createPointToPointMessagingContext(endPointProvider);
```

An [EndPointProvider](http://code.google.com/p/freewheel-messaging/source/browse/trunk/src/main/java/com/epickrram/freewheel/messaging/ptp/EndPointProvider.java) must be supplied to the factory.
`PropertiesFileEndPointProvider` is provided as a convenient implementation, and will load a properties file from the classpath in order to obtain the necessary information.

The properties file should be of the following format:
```
endPoint.com.company.ServiceA.port=14001
endPoint.com.company.ServiceA.host=10.80.10.35

endPoint.com.company.ServiceB.port=14003
endPoint.com.company.ServiceB.host=10.80.10.46
```

This file will cause publishers for `com.company.ServiceA` to send data to `10.80.10.35` on port `14001`, and subscribers to `com.company.ServiceA` to bind to `0.0.0.0` on port `14001`.