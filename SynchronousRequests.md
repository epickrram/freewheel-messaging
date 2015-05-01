# Synchronous Requests #

Freewheel is designed to facilitate fully asynchronous messaging, however there are some scenarios in which it is necessary to send messages synchronously (for example, an authentication request). Freewheel comes with a mechanism for achieving this, which is outlined below.

Given a remote service:
```
public interface Service 
{
    void login(final String username, final String token);
}
```
create a response interface for notifying the method result to the client:
```
public interface ServiceResponse
{
    void onLoginResponse(final String username, final boolean authenticated);
}
```
and a client interface to tie the two together using the `@SyncMethod` annotation:
```
public interface ServiceClient
{
    @SyncMethod(requestMethod="login", responseMethod="onLoginResponse");
    boolean doLogin(final String username, final String token);
}
```
then use `SynchronousProxyHelper` to create an implementation that will handle synchronous calls:
```
final SynchronousProxyHelper proxyHelper = new SynchronousProxyHelper();

final ServiceResponse serviceResponse = messagingContext.createPublisher(ServiceResponse.class);

messagingContext.createSubscriber(Service.class, new ServiceImpl(serviceResponse));

final ServiceClient serviceClient =
    proxyHelper.createSynchronousProxy(Service.class, ServiceResponse.class, ServiceClient.class, messagingContext);

messagingContext.start();

final boolean authenticated = serviceClient.doLogin("foo", "bar");
```