// Copyright 2008 Google Inc.  All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.saml.common;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.IdentifierGenerator;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.BasicEndpointSelector;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.core.Action;
import org.opensaml.saml2.core.Artifact;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.ArtifactResponse;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.AuthzDecisionQuery;
import org.opensaml.saml2.core.AuthzDecisionStatement;
import org.opensaml.saml2.core.DecisionTypeEnumeration;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.AuthzService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.PDPDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SSODescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.message.decoder.MessageDecoder;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncoder;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.soap.common.SOAPObject;
import org.opensaml.ws.soap.common.SOAPObjectBuilder;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.SecurityException;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.namespace.QName;

import static org.opensaml.common.xml.SAMLConstants.SAML20P_NS;

/**
 * A collection of utilities to support OpenSAML programming.
 *
 * The overwhelming majority of the definitions here are static factory methods for SAML
 * objects.
 */
public final class OpenSamlUtil {

  /**
   * The human-readable name of the service provider.
   */
  public static final String GOOGLE_PROVIDER_NAME = "Google Search Appliance";

  static {
    try {
      DefaultBootstrap.bootstrap();
    } catch (ConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final XMLObjectBuilderFactory objectBuilderFactory =
      Configuration.getBuilderFactory();

  // TODO(cph): @SuppressWarnings is needed because objectBuilderFactory.getBuilder() returns a
  // supertype of the actual type.
  @SuppressWarnings("unchecked")
  private static <T extends SAMLObject> SAMLObjectBuilder<T> makeSamlObjectBuilder(QName name) {
    return (SAMLObjectBuilder<T>) objectBuilderFactory.getBuilder(name);
  }

  private static final SAMLObjectBuilder<Action> actionBuilder =
      makeSamlObjectBuilder(Action.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Artifact> artifactBuilder =
      makeSamlObjectBuilder(Artifact.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<ArtifactResolve> artifactResolveBuilder =
      makeSamlObjectBuilder(ArtifactResolve.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<ArtifactResponse> artifactResponseBuilder =
      makeSamlObjectBuilder(ArtifactResponse.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Assertion> assertionBuilder =
      makeSamlObjectBuilder(Assertion.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthnContext> authnContextBuilder =
      makeSamlObjectBuilder(AuthnContext.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthnContextClassRef> authnContextClassRefBuilder =
      makeSamlObjectBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthnRequest> authnRequestBuilder =
      makeSamlObjectBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthnStatement> authnStatementBuilder =
      makeSamlObjectBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthzDecisionQuery> authzDecisionQueryBuilder =
      makeSamlObjectBuilder(AuthzDecisionQuery.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthzDecisionStatement> authzDecisionStatementBuilder =
      makeSamlObjectBuilder(AuthzDecisionStatement.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Issuer> issuerBuilder =
      makeSamlObjectBuilder(Issuer.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<NameID> nameIDBuilder =
      makeSamlObjectBuilder(NameID.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<NameIDPolicy> nameIdPolicyBuilder =
      makeSamlObjectBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Response> responseBuilder =
      makeSamlObjectBuilder(Response.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Status> statusBuilder =
      makeSamlObjectBuilder(Status.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<StatusCode> statusCodeBuilder =
      makeSamlObjectBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<StatusMessage> statusMessageBuilder =
      makeSamlObjectBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<Subject> subjectBuilder =
      makeSamlObjectBuilder(Subject.DEFAULT_ELEMENT_NAME);

  // Metadata builders

  private static final SAMLObjectBuilder<EntityDescriptor> entityDescriptorBuilder =
      makeSamlObjectBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<IDPSSODescriptor> idpSsoDescriptorBuilder =
      makeSamlObjectBuilder(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<PDPDescriptor> pdpDescriptorBuilder =
      makeSamlObjectBuilder(PDPDescriptor.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<RoleDescriptor> roleDescriptorBuilder =
      makeSamlObjectBuilder(RoleDescriptor.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<SPSSODescriptor> spSsoDescriptorBuilder =
      makeSamlObjectBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

  private static final SAMLObjectBuilder<ArtifactResolutionService>
      artifactResolutionServiceBuilder =
        makeSamlObjectBuilder(ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AssertionConsumerService> assertionConsumerServiceBuilder =
      makeSamlObjectBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<AuthzService> authzServiceBuilder =
      makeSamlObjectBuilder(AuthzService.DEFAULT_ELEMENT_NAME);
  private static final SAMLObjectBuilder<SingleSignOnService> singleSignOnServiceBuilder =
      makeSamlObjectBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME);

  // SOAP builders

  // TODO(cph): @SuppressWarnings is needed because objectBuilderFactory.getBuilder() returns a
  // supertype of the actual type.
  @SuppressWarnings("unchecked")
  private static <T extends SOAPObject> SOAPObjectBuilder<T> makeSoapObjectBuilder(QName name) {
    return (SOAPObjectBuilder<T>) objectBuilderFactory.getBuilder(name);
  }

  private static final SOAPObjectBuilder<Body> soapBodyBuilder =
      makeSoapObjectBuilder(Body.DEFAULT_ELEMENT_NAME);
  private static final SOAPObjectBuilder<Envelope> soapEnvelopeBuilder =
      makeSoapObjectBuilder(Envelope.DEFAULT_ELEMENT_NAME);

  // Identifier generator

  private static final IdentifierGenerator idGenerator;
  static {
    try {
      idGenerator = new SecureRandomIdentifierGenerator();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  // Non-instantiable class.
  private OpenSamlUtil() {
  }

  private static void initializeRequest(RequestAbstractType request) {
    request.setID(generateIdentifier());
    request.setVersion(SAMLVersion.VERSION_20);
    request.setIssueInstant(new DateTime());
  }

  private static void initializeResponse(StatusResponseType response, Status status,
      RequestAbstractType request) {
    response.setID(generateIdentifier());
    response.setVersion(SAMLVersion.VERSION_20);
    response.setIssueInstant(new DateTime());
    response.setStatus(status);
    if (request != null) {
      response.setInResponseTo(request.getID());
    }
  }

  /**
   * Static factory for SAML <code>Action</code> objects.
   *
   * @param name A URI identifying the represented action.
   * @param namespace A URI identifying the class of names being specified.
   * @return A new <code>Action</code> object.
   */
  public static Action makeAction(String name, String namespace) {
    Action action = actionBuilder.buildObject();
    action.setAction(name);
    action.setNamespace(namespace);
    return action;
  }

  /**
   * Static factory for SAML <code>Artifact</code> objects.
   *
   * @param value The artifact string.
   * @return A new <code>Artifact</code> object.
   */
  public static Artifact makeArtifact(String value) {
    Artifact element = artifactBuilder.buildObject();
    element.setArtifact(value);
    return element;
  }

  /**
   * Static factory for SAML <code>ArtifactResolve</code> objects.
   *
   * @param artifact The <code>Artifact</code> object to be resolved.
   * @return A new <code>ArtifactResolve</code> object.
   */
  public static ArtifactResolve makeArtifactResolve(Artifact artifact) {
    ArtifactResolve request = artifactResolveBuilder.buildObject();
    initializeRequest(request);
    request.setArtifact(artifact);
    return request;
  }

  /**
   * Static factory for SAML <code>ArtifactResolve</code> objects.
   *
   * A convenience method that wraps <var>value</var> in an <code>Artifact</code> object.
   *
   * @param value The artifact string to be resolved.
   * @return A new <code>ArtifactResolve</code> object.
   */
  public static ArtifactResolve makeArtifactResolve(String value) {
    return makeArtifactResolve(makeArtifact(value));
  }

  /**
   * Static factory for SAML <code>ArtifactResponse</code> objects.
   *
   * @param request The <code>ArtifactResolve</code> object that this is a response to.
   * @param status The <code>Status</code> object indicating the success of the resolution.
   * @return A new <code>ArtifactResponse</code> object.
   */
  public static ArtifactResponse makeArtifactResponse(ArtifactResolve request, Status status) {
    ArtifactResponse response = artifactResponseBuilder.buildObject();
    initializeResponse(response, status, request);
    return response;
  }

  /**
   * Static factory for SAML <code>ArtifactResponse</code> objects.
   *
   * @param request The <code>ArtifactResolve</code> object that this is a response to.
   * @param status The <code>Status</code> object indicating the success of the resolution.
   * @param message The SAML object that is the value of of the artifact binding.
   * @return A new <code>ArtifactResponse</code> object.
   */
  public static ArtifactResponse makeArtifactResponse(ArtifactResolve request, Status status,
      SAMLObject message) {
    ArtifactResponse response = makeArtifactResponse(request, status);
    response.setMessage(message);
    return response;
  }

  /**
   * Static factory for SAML <code>Assertion</code> objects.
   *
   * @param issuer The entity issuing this assertion.
   * @return A new <code>Assertion</code> object.
   */
  public static Assertion makeAssertion(Issuer issuer) {
    Assertion assertion = assertionBuilder.buildObject();
    assertion.setID(generateIdentifier());
    assertion.setVersion(SAMLVersion.VERSION_20);
    assertion.setIssueInstant(new DateTime());
    assertion.setIssuer(issuer);
    return assertion;
  }

  /**
   * Static factory for SAML <code>Assertion</code> objects.
   *
   * @param issuer The entity issuing this assertion.
   * @param subject The subject of the assertion.
   * @return A new <code>Assertion</code> object.
   */
  public static Assertion makeAssertion(Issuer issuer, Subject subject) {
    Assertion assertion = makeAssertion(issuer);
    assertion.setSubject(subject);
    return assertion;
  }

  /**
   * Static factory for SAML <code>AuthnContext</code> objects.
   *
   * @param classRef An <code>AuthnContextClassRef</code> identifying an authentication context
   * class.
   * @return A new <code>AuthnContext</code> object.
   */
  public static AuthnContext makeAuthnContext(AuthnContextClassRef classRef) {
    AuthnContext context = authnContextBuilder.buildObject();
    context.setAuthnContextClassRef(classRef);
    return context;
  }

  /**
   * Static factory for SAML <code>AuthnContext</code> objects.
   *
   * A convenience method that wraps the given URI in an <code>AuthnContextClassRef</code> object.
   *
   * @param uri A URI identifying an authentication context class.
   * @return A new <code>AuthnContext</code> object.
   */
  public static AuthnContext makeAuthnContext(String uri) {
    return makeAuthnContext(makeAuthnContextClassRef(uri));
  }

  /**
   * Static factory for SAML <code>AuthnContextClassRef</code> objects.
   *
   * @param uri A URI identifying an authentication context class.
   * @return A new <code>AuthnContextClassRef</code> object.
   */
  public static AuthnContextClassRef makeAuthnContextClassRef(String uri) {
    AuthnContextClassRef classRef = authnContextClassRefBuilder.buildObject();
    classRef.setAuthnContextClassRef(uri);
    return classRef;
  }

  /**
   * Static factory for SAML <code>AuthnRequest</code> objects.
   *
   * @return A new <code>AuthnRequest</code> object.
   */
  public static AuthnRequest makeAuthnRequest() {
    AuthnRequest request = authnRequestBuilder.buildObject();
    initializeRequest(request);
    return request;
  }

  /**
   * Static factory for SAML <code>AuthnStatement</code> objects.
   *
   * A convenience method that wraps the given URI in an <code>AuthnContext</code> object.
   *
   * @param uri A URI identifying an authentication context class.
   * @return A new <code>AuthnStatement</code> object.
   */
  public static AuthnStatement makeAuthnStatement(String uri) {
    return makeAuthnStatement(makeAuthnContext(uri), new DateTime());
  }

  /**
   * Static factory for SAML <code>AuthnStatement</code> objects.
   *
   * A convenience method that issues an authentication statement for the current time.
   *
   * @param context An authentication context object.
   * @return A new <code>AuthnStatement</code> object.
   */
  public static AuthnStatement makeAuthnStatement(AuthnContext context) {
    return makeAuthnStatement(context, new DateTime());
  }

  /**
   * Static factory for SAML <code>AuthnStatement</code> objects.
   *
   * @param context An authentication context object.
   * @param authnInstant The time of issue for this statement.
   * @return A new <code>AuthnStatement</code> object.
   */
  public static AuthnStatement makeAuthnStatement(AuthnContext context, DateTime authnInstant) {
    AuthnStatement statement = authnStatementBuilder.buildObject();
    statement.setAuthnContext(context);
    statement.setAuthnInstant(authnInstant);
    return statement;
  }

  /**
   * Static factory for SAML <code>AuthzDecisionQuery</code> objects.
   *
   * @param subject The subject requesting access to a resource.
   * @param resource The resource for which access is being requested.
   * @param action The action on the resource for which access is being requested.
   * @return A new <code>AuthzDecisionQuery</code> object.
   */
  public static AuthzDecisionQuery makeAuthzDecisionQuery(Subject subject, String resource,
      Action action) {
    AuthzDecisionQuery query = authzDecisionQueryBuilder.buildObject();
    query.setSubject(subject);
    query.setResource(resource);
    query.getActions().add(action);
    return query;
  }

  /**
   * Static factory for SAML <code>AuthzDecisionStatement</code> objects.
   *
   * @param resource The resource referred to by this access decision.
   * @param decision The access decision made by the authorization service.
   * @param action The action granted on the given resource.
   * @return A new <code>AuthzDecisionStatement</code> object.
   */
  public static AuthzDecisionStatement makeAuthzDecisionStatement(String resource,
      DecisionTypeEnumeration decision, Action action) {
    AuthzDecisionStatement statement = authzDecisionStatementBuilder.buildObject();
    statement.setResource(resource);
    statement.setDecision(decision);
    statement.getActions().add(action);
    return statement;
  }

  /**
   * Static factory for SAML <code>Issuer</code> objects.
   *
   * @param name The issuer of a response object.  In the absence of a specific format, this is a
   * URI identifying the issuer.
   * @return A new <code>Issuer</code> object.
   */
  public static Issuer makeIssuer(String name) {
    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(name);
    return issuer;
  }

  /**
   * Static factory for SAML <code>NameID</code> objects.
   *
   * @param name The name represented by this object.
   * @return A new <code>NameID</code> object.
   */
  public static NameID makeNameId(String name) {
    NameID id = nameIDBuilder.buildObject();
    id.setValue(name);
    return id;
  }

  /**
   * Static factory for SAML <code>NameIDPolicy</code> objects.
   *
   * @param format The URI specifying the format of a class of names.
   * @return A new <code>NameIDPolicy</code> object.
   */
  public static NameIDPolicy makeNameIdPolicy(String format) {
    NameIDPolicy idPolicy = nameIdPolicyBuilder.buildObject();
    idPolicy.setFormat(format);
    return idPolicy;
  }

  /**
   * Static factory for SAML <code>Response</code> objects.
   *
   * @param request The request that this is a response to.
   * @param status The <code>Status</code> object indicating the success of requested action.
   * @return A new <code>Response</code> object.
   */
  public static Response makeResponse(RequestAbstractType request, Status status) {
    Response response = responseBuilder.buildObject();
    initializeResponse(response, status, request);
    return response;
  }

  /**
   * Static factory for SAML <code>Status</code> objects.
   *
   * @param code A <code>StatusCode</code> object containing the SAML status-code URI.
   * @return A new <code>Status</code> object.
   */
  public static Status makeStatus(StatusCode code) {
    Status status = statusBuilder.buildObject();
    status.setStatusCode(code);
    return status;
  }

  /**
   * Static factory for SAML <code>Status</code> objects.
   *
   * A convenience method that supplies an empty <code>StatusCode</code> object.
   *
   * @return A new <code>Status</code> object.
   */
  public static Status makeStatus() {
    return makeStatus(makeStatusCode());
  }

  /**
   * Static factory for SAML <code>Status</code> objects.
   *
   * A convenience method that wraps the given URI in a <code>StatusCode</code> object.
   *
   * @param value A URI specifying one of the standard SAML status codes.
   * @return A new <code>Status</code> object.
   */
  public static Status makeStatus(String value) {
    return makeStatus(makeStatusCode(value));
  }

  /**
   * Static factory for SAML <code>StatusCode</code> objects.
   *
   * @return A new <code>StatusCode</code> object.
   */
  public static StatusCode makeStatusCode() {
    return statusCodeBuilder.buildObject();
  }

  /**
   * Static factory for SAML <code>StatusCode</code> objects.
   *
   * @param value A URI specifying one of the standard SAML status codes.
   * @return A new <code>StatusCode</code> object.
   */
  public static StatusCode makeStatusCode(String value) {
    StatusCode code = makeStatusCode();
    code.setValue(value);
    return code;
  }

  /**
   * Static factory for SAML <code>StatusMessage</code> objects.
   *
   * @param value A status message string.
   * @return A new <code>StatusMessage</code> object.
   */
  public static StatusMessage makeStatusMessage(String value) {
    StatusMessage message = statusMessageBuilder.buildObject();
    message.setMessage(value);
    return message;
  }

  /**
   * Static factory for SAML <code>Subject</code> objects.
   *
   * Returns a subject that has no distinguished identifier.  The caller is expected to fill in one
   * or more <code>SubjectConfirmation</code> elements.
   *
   * @return A new <code>Subject</code> object.
   */
  public static Subject makeSubject() {
    return subjectBuilder.buildObject();
  }

  /**
   * Static factory for SAML <code>Subject</code> objects.
   *
   * Returns a subject that has the given identifier.  The caller may optionally fill in any number
   * of <code>SubjectConfirmation</code> elements.
   *
   * @param id The identifier for this subject.
   * @return A new <code>Subject</code> object.
   */
  public static Subject makeSubject(NameID id) {
    Subject samlSubject = makeSubject();
    samlSubject.setNameID(id);
    return samlSubject;
  }

  /**
   * Static factory for SAML <code>Subject</code> objects.
   *
   * A convenience method that wraps the given name in a <code>NameId</code> object.
   *
   * @param name The name identifying the subject.
   * @return A new <code>Subject</code> object.
   */
  public static Subject makeSubject(String name) {
    return makeSubject(makeNameId(name));
  }

  /*
   * SOAP (needed for client-side operations, which OpenSAML doesn't support well)
   */

  /**
   * Static factory for SOAP <code>Body</code> objects.
   *
   * @return A new <code>Body</code> object.
   */
  public static Body makeSoapBody() {
    return soapBodyBuilder.buildObject();
  }

  /**
   * Static factory for SOAP <code>Envelope</code> objects.
   *
   * @return A new <code>Envelope</code> object.
   */
  public static Envelope makeSoapEnvelope() {
    return soapEnvelopeBuilder.buildObject();
  }

  /*
   * Metadata descriptions.
   */

  /**
   * Static factory for SAML <code>EntityDescriptor</code> objects.
   *
   * An entity is something that participates in one or more SAML profiles.  The descriptor for that
   * entity spells out the roles and profiles played by the entity.
   *
   * @return An <code>EntityDescriptor</code> object.
   */
  public static EntityDescriptor makeEntityDescriptor(String id) {
    EntityDescriptor descriptor = entityDescriptorBuilder.buildObject();
    descriptor.setEntityID(id);
    descriptor.setID(id);
    return descriptor;
  }

  // Metadata roles

  /**
   * Static factory for SAML <code>IDPSSODescriptor</code> objects.
   *
   * This descriptor represents the Identity Provider role in the Web SSO profile.
   *
   * @param entity The entity that this is a role for.
   * @return An <code>IDPSSODescriptor</code> object.
   */
  public static IDPSSODescriptor makeIdpSsoDescriptor(EntityDescriptor entity) {
    IDPSSODescriptor descriptor = idpSsoDescriptorBuilder.buildObject();
    initializeRoleDescriptor(descriptor, entity);
    return descriptor;
  }

  /**
   * Static factory for SAML <code>SPSSODescriptor</code> objects.
   *
   * This descriptor represents the Service Provider role in the Web SSO profile.
   *
   * @param entity The entity that this is a role for.
   * @return A <code>SPSSODescriptor</code> object.
   */
  public static SPSSODescriptor makeSpSsoDescriptor(EntityDescriptor entity) {
    SPSSODescriptor descriptor = spSsoDescriptorBuilder.buildObject();
    initializeRoleDescriptor(descriptor, entity);
    return descriptor;
  }

  /**
   * Static factory for SAML <code>makePdpDescriptor</code> objects.
   *
   * This descriptor represents the Policy Decision Point role in the Assertion Query/Request
   * profile.
   *
   * @param entity The entity that this is a role for.
   * @profile A <code>PDPDescriptor</code> object.
   */
  public static PDPDescriptor makePdpDescriptor(EntityDescriptor entity) {
    PDPDescriptor descriptor = pdpDescriptorBuilder.buildObject();
    initializeRoleDescriptor(descriptor, entity);
    return descriptor;
  }

  /**
   * Static factory for SAML <code>makeRoleDescriptor</code> objects.
   *
   * This descriptor represents a role that's a SAML extension.
   *
   * @param entity The entity that this is a role for.
   * @return A <code>RoleDescriptor</code> object.
   */
  public static RoleDescriptor makeRoleDescriptor(EntityDescriptor entity) {
    RoleDescriptor descriptor = roleDescriptorBuilder.buildObject();
    initializeRoleDescriptor(descriptor, entity);
    return descriptor;
  }

  private static void initializeRoleDescriptor(RoleDescriptor descriptor, EntityDescriptor entity) {
    descriptor.addSupportedProtocol(SAML20P_NS);
    entity.getRoleDescriptors().add(descriptor);
  }

  // Metadata services

  /**
   * Static factory for SAML <code>ArtifactResolutionService</code> objects.
   *
   * @param role The SAML SSO role providing this service.
   * @param binding The SAML binding implemented by this service.
   * @param location The URL that this service listens to.
   * @return A new <code>ArtifactResolutionService</code> object.
   */
  public static ArtifactResolutionService makeArtifactResolutionService(SSODescriptor role,
      String binding, String location) {
    ArtifactResolutionService service = artifactResolutionServiceBuilder.buildObject();
    service.setBinding(binding);
    service.setLocation(location);
    List<ArtifactResolutionService> services = role.getArtifactResolutionServices();
    // TODO(cph): Next two should be atomic.
    service.setIndex(services.size());
    services.add(service);
    return service;
  }

  /**
   * Static factory for SAML <code>AssertionConsumerService</code> objects.
   *
   * @param role The SAML SSO role providing this service.
   * @param binding The SAML binding implemented by this service.
   * @param location The URL that the service listens to.
   * @return A new <code>AssertionConsumerService</code> object.
   */
  public static AssertionConsumerService makeAssertionConsumerService(SPSSODescriptor role,
      String binding, String location) {
    AssertionConsumerService service = assertionConsumerServiceBuilder.buildObject();
    service.setBinding(binding);
    service.setLocation(location);
    List<AssertionConsumerService> services = role.getAssertionConsumerServices();
    // TODO(cph): Next two should be atomic.
    service.setIndex(services.size());
    services.add(service);
    return service;
  }

  /**
   * Static factory for SAML <code>AuthzService</code> objects.
   *
   * @param role The SAML authz query/request role providing this service.
   * @param binding The SAML binding implemented by this service.
   * @param location The URL that the service listens to.
   * @return A new <code>AuthzService</code> object.
   */
  public static AuthzService makeAuthzService(PDPDescriptor role, String binding, String location) {
    AuthzService service = authzServiceBuilder.buildObject();
    service.setBinding(binding);
    service.setLocation(location);
    role.getAuthzServices().add(service);
    return service;
  }

  /**
   * Static factory for SAML <code>SingleSignOnService</code> objects.
   *
   * @param role The SAML SSO role providing this service.
   * @param binding The SAML binding implemented by this service.
   * @param location The URL that the service listens to.
   * @return A new <code>SingleSignOnService</code> object.
   */
  public static SingleSignOnService makeSingleSignOnService(IDPSSODescriptor role, String binding,
      String location) {
    SingleSignOnService service = singleSignOnServiceBuilder.buildObject();
    service.setBinding(binding);
    service.setLocation(location);
    role.getSingleSignOnServices().add(service);
    return service;
  }

  /*
   * Endpoint selection
   */

  public static void initializeLocalEntity(
      SAMLMessageContext<? extends SAMLObject, ? extends SAMLObject, ? extends SAMLObject> context,
      EntityDescriptor entity, RoleDescriptor role, QName endpointType) {
    context.setLocalEntityId(entity.getEntityID());
    context.setLocalEntityMetadata(entity);
    context.setLocalEntityRole(endpointType);
    context.setLocalEntityRoleMetadata(role);
    context.setOutboundMessageIssuer(entity.getEntityID());
  }

  public static void initializePeerEntity(
      SAMLMessageContext<? extends SAMLObject, ? extends SAMLObject, ? extends SAMLObject> context,
      EntityDescriptor entity, RoleDescriptor role, QName endpointType, String binding) {
    context.setPeerEntityId(entity.getEntityID());
    context.setPeerEntityMetadata(entity);
    context.setPeerEntityRole(endpointType);
    context.setPeerEntityRoleMetadata(role);
    {
      BasicEndpointSelector selector = new BasicEndpointSelector();
      selector.setEntityMetadata(entity);
      selector.setEndpointType(endpointType);
      selector.setEntityRoleMetadata(role);
      selector.getSupportedIssuerBindings().add(binding);
      context.setPeerEntityEndpoint(selector.selectEndpoint());
    }
  }

  /*
   * Identifiers
   */

  /**
   * A convenience method for generating a random identifier.
   *
   * @return A new identifier string.
   */
  public static String generateIdentifier() {
    return idGenerator.generateIdentifier();
  }

  /*
   * Context and codecs
   */

  /**
   * Static factory for OpenSAML message-context objects.
   *
   * @param <TI> The type of the request object.
   * @param <TO> The type of the response object.
   * @param <TN> The type of the name identifier used for subjects.
   * @return A new message-context object.
   */
  public static <TI extends SAMLObject, TO extends SAMLObject, TN extends SAMLObject>
        SAMLMessageContext<TI, TO, TN> makeSamlMessageContext() {
    return new BasicSAMLMessageContext<TI, TO, TN>();
  }

  /**
   * Run a message encoder, translating exceptions into ServletException.
   *
   * @param encoder The message encoder to run.
   * @param context The message context to pass to the encoder.
   * @throws ServletException
   */
  public static void runEncoder(MessageEncoder encoder, MessageContext context)
      throws ServletException {
    try {
      encoder.encode(context);
    } catch (MessageEncodingException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Run a message decoder, translating exceptions into ServletException.
   *
   * @param decoder The message decoder to run.
   * @param context The message context to pass to the decoder.
   * @throws ServletException
   */
  public static void runDecoder(MessageDecoder decoder, MessageContext context)
      throws ServletException {
    try {
      decoder.decode(context);
    } catch (MessageDecodingException e) {
      throw new ServletException(e);
    } catch (SecurityException e) {
      throw new ServletException(e);
    }
  }

  public static String samlDateString() {
    return samlDateString(new DateTime());
  }

  public static String samlDateString(DateTime date) {
    return Configuration.getSAMLDateFormatter().print(date);
  }

  public static MetadataProvider getMetadataFromFile(String filename)
      throws MetadataProviderException {
    FilesystemMetadataProvider provider = new FilesystemMetadataProvider(new File(filename));
    provider.setParserPool(new BasicParserPool());
    // Causes null-pointer errors in OpenSAML code:
    //provider.setRequireValidMetadata(true);
    return provider;
  }
}
