package org.apache.ambari.api.services;


import org.apache.ambari.api.handlers.RequestHandler;
import org.apache.ambari.api.resource.ResourceDefinition;
import org.apache.ambari.api.services.serializers.ResultSerializer;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 9/12/12
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentServiceTest {

  @Test
  public void testGetComponent() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), eq(uriInfo), eq(Request.Type.GET),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);

    expect(responseFactory.createResponse(serializedResult)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    ComponentService componentService = new TestComponentService(resourceDef, clusterName, serviceName, componentName,
        requestFactory, responseFactory, requestHandler);
    assertSame(response, componentService.getComponent(httpHeaders, uriInfo, componentName));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testGetComponents() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String serviceName = "serviceName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), eq(uriInfo), eq(Request.Type.GET),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(responseFactory.createResponse(serializedResult)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    ComponentService componentService = new TestComponentService(resourceDef, clusterName, serviceName, null, requestFactory, responseFactory, requestHandler);
    assertSame(response, componentService.getComponents(httpHeaders, uriInfo));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  private class TestComponentService extends ComponentService {
    private RequestFactory m_requestFactory;
    private ResponseFactory m_responseFactory;
    private RequestHandler m_requestHandler;
    private ResourceDefinition m_resourceDef;
    private String m_clusterId;
    private String m_serviceId;
    private String m_componentId;

    private TestComponentService(ResourceDefinition resourceDef, String clusterId, String serviceId, String componentId, RequestFactory requestFactory, ResponseFactory responseFactory, RequestHandler handler) {
      super(clusterId, serviceId);
      m_requestFactory = requestFactory;
      m_responseFactory = responseFactory;
      m_requestHandler = handler;
      m_resourceDef = resourceDef;
      m_clusterId = clusterId;
      m_serviceId = serviceId;
      m_componentId = componentId;
    }

    @Override
    ResourceDefinition createResourceDefinition(String componentName, String clusterName, String serviceName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      assertEquals(m_componentId, componentName);
      return m_resourceDef;
    }

    @Override
    RequestFactory getRequestFactory() {
      return m_requestFactory;
    }

    @Override
    ResponseFactory getResponseFactory() {
      return m_responseFactory;
    }

    @Override
    RequestHandler getRequestHandler() {
      return m_requestHandler;
    }
  }
}
