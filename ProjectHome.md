# Using Freewheel #

Create a `MessagingContext`

```
final EndPoint multicastAddress = new EndPoint(InetAddress.getByName("239.0.0.1"), 14999);
final MessagingContext ctx = 
        new MessagingContextFactory().createMulticastMessagingService(multicastAddress);

```

Create publishers

```
final MyServiceA serviceA = ctx.createPublisher(MyServiceA.class);
```

Create subscribers (on another host)

```
ctx.createSubscriber(MyServiceA.class, myServiceAImplementation);
```

Start the context

```
ctx.start();
```

Make remote invocations:
```
serviceA.onEvent(event);
```


Done!


### Dependencies ###

Are listed here: [DependencyList](http://code.google.com/p/freewheel-messaging/wiki/DependencyList)