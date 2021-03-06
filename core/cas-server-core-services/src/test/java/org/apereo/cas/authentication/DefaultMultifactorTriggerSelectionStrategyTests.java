package org.apereo.cas.authentication;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.services.DefaultRegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.MultifactorAuthenticationProvider;
import org.apereo.cas.services.RegexRegisteredService;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Daniel Frett
 * @since 5.0.0
 */
public class DefaultMultifactorTriggerSelectionStrategyTests {
    private static final String MFA_INVALID = "mfaInvalid";
    private static final String MFA_PROVIDER_ID_1 = "mfa-id1";
    private static final String MFA_PROVIDER_ID_2 = "mfa-id2";
    private static final MultifactorAuthenticationProvider MFA_PROVIDER_1 = mock(MultifactorAuthenticationProvider.class);
    private static final MultifactorAuthenticationProvider MFA_PROVIDER_2 = mock(MultifactorAuthenticationProvider.class);
    private static final ImmutableSet<MultifactorAuthenticationProvider> VALID_PROVIDERS = ImmutableSet.of(MFA_PROVIDER_1, MFA_PROVIDER_2);
    private static final ImmutableSet<MultifactorAuthenticationProvider> NO_PROVIDERS = ImmutableSet.of();

    private static final String REQUEST_PARAM = "authn_method";

    private static final String RS_ATTR_1 = "rs_attr1";
    private static final String RS_ATTR_2 = "rs_attr2";
    private static final String RS_ATTR_3 = "rs_attr3";
    private static final String RS_ATTRS_12 = RS_ATTR_1 + ',' + RS_ATTR_2;
    private static final String P_ATTR_1 = "principal_attr_1";
    private static final String P_ATTR_2 = "principal_attr_2";
    private static final String P_ATTR_3 = "principal_attr_3";
    private static final String P_ATTRS_12 = P_ATTR_1 + ',' + P_ATTR_2;

    private static final String VALUE_1 = "enforce_1";
    private static final String VALUE_2 = "enforce_2";
    private static final String VALUE_PATTERN = "^enforce.*$";
    private static final String VALUE_NOMATCH = "noop";

    private final DefaultPrincipalFactory principalFactory = new DefaultPrincipalFactory();

    private DefaultMultifactorTriggerSelectionStrategy strategy;

    @Before
    public void setUp() {
        strategy = new DefaultMultifactorTriggerSelectionStrategy(P_ATTRS_12, REQUEST_PARAM);

        when(MFA_PROVIDER_1.getId()).thenReturn(MFA_PROVIDER_ID_1);
        when(MFA_PROVIDER_2.getId()).thenReturn(MFA_PROVIDER_ID_2);
    }

    @Test
    public void verifyNoProviders() throws Exception {
        assertThat(strategy.resolve(NO_PROVIDERS, mockRequest(MFA_PROVIDER_ID_1), mockService(MFA_PROVIDER_ID_1), null).isPresent(),
                is(false));
    }

    @Test
    public void verifyRequestParameterTrigger() throws Exception {
        // opt-in parameter only
        assertThat(strategy.resolve(VALID_PROVIDERS, mockRequest(MFA_PROVIDER_ID_1), null, null).orElse(null), is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, mockRequest(MFA_PROVIDER_ID_2), null, null).orElse(null), is(MFA_PROVIDER_ID_2));
        assertThat(strategy.resolve(VALID_PROVIDERS, mockRequest(MFA_INVALID), null, null).isPresent(), is(false));
    }

    @Test
    public void verifyRegisteredServiceTrigger() throws Exception {
        // regular RegisteredService trigger
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockService(MFA_PROVIDER_ID_1), null).orElse(null), is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockService(MFA_PROVIDER_ID_2), null).orElse(null), is(MFA_PROVIDER_ID_2));

        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockService(MFA_INVALID, MFA_PROVIDER_ID_1, MFA_PROVIDER_ID_2), null).get(),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockService(MFA_INVALID), null).isPresent(), is(false));

        // Principal attribute activated RegisteredService trigger - direct match
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTR_1, VALUE_1),
                mockPrincipal(RS_ATTR_1, VALUE_1)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTR_1, VALUE_1),
                mockPrincipal(RS_ATTR_1, VALUE_2)).orElse(null),
                nullValue());

        // Principal attribute activated RegisteredService trigger - multiple attrs
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12, VALUE_1),
                mockPrincipal(RS_ATTR_1, VALUE_1)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12, VALUE_1),
                mockPrincipal(RS_ATTR_2, VALUE_1)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12, VALUE_1),
                mockPrincipal(RS_ATTR_3, VALUE_1)).orElse(null),
                nullValue());

        // Principal attribute activated RegisteredService trigger - pattern value
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12,
                VALUE_PATTERN), mockPrincipal(RS_ATTR_2, VALUE_1)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12,
                VALUE_PATTERN), mockPrincipal(RS_ATTR_2, VALUE_2)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, mockPrincipalService(MFA_PROVIDER_ID_1, RS_ATTRS_12, VALUE_PATTERN),
                mockPrincipal(RS_ATTR_2, VALUE_NOMATCH)).isPresent(), is(false));
    }

    @Test
    public void verifyPrincipalAttributeTrigger() throws Exception {
        // Principal attribute trigger
        assertThat(strategy.resolve(VALID_PROVIDERS, null, null, mockPrincipal(P_ATTR_1, MFA_PROVIDER_ID_1)).orElse(null),
                is(MFA_PROVIDER_ID_1));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, null, mockPrincipal(P_ATTR_1, MFA_PROVIDER_ID_2)).orElse(null),
                is(MFA_PROVIDER_ID_2));
        assertThat(strategy.resolve(VALID_PROVIDERS, null, null, mockPrincipal(P_ATTR_1, MFA_INVALID)).isPresent(), is(false));
    }

    @Test
    public void verifyMultipleTriggers() throws Exception {
        // opt-in overrides everything
        assertThat(strategy.resolve(VALID_PROVIDERS, mockRequest(MFA_PROVIDER_ID_1), mockService(MFA_PROVIDER_ID_2),
                mockPrincipal(P_ATTR_1, MFA_PROVIDER_ID_2)).orElse(null),
                is(MFA_PROVIDER_ID_1));

        // RegisteredService overrides Principal attribute
        assertThat(strategy.resolve(VALID_PROVIDERS, mockRequest(MFA_INVALID), mockService(MFA_PROVIDER_ID_1),
                mockPrincipal(P_ATTR_1, MFA_PROVIDER_ID_2)).orElse(null),
                is(MFA_PROVIDER_ID_1));
    }

    private static HttpServletRequest mockRequest() {
        return mock(HttpServletRequest.class);
    }

    private static HttpServletRequest mockRequest(final String provider) {
        final HttpServletRequest request = mockRequest();
        when(request.getParameter(REQUEST_PARAM)).thenReturn(provider);
        return request;
    }

    private static RegexRegisteredService mockService(final String... providers) {
        final DefaultRegisteredServiceMultifactorPolicy policy = new DefaultRegisteredServiceMultifactorPolicy();
        policy.setMultifactorAuthenticationProviders(ImmutableSet.copyOf(providers));
        final RegexRegisteredService service = new RegexRegisteredService();
        service.setMultifactorPolicy(policy);
        return service;
    }

    private static RegexRegisteredService mockPrincipalService(final String provider, final String attrName,
                                                        final String attrValue) {
        final RegexRegisteredService service = mockService(provider);
        final DefaultRegisteredServiceMultifactorPolicy policy = (DefaultRegisteredServiceMultifactorPolicy) service
                .getMultifactorPolicy();
        policy.setPrincipalAttributeNameTrigger(attrName);
        policy.setPrincipalAttributeValueToMatch(attrValue);

        return service;
    }
    
    private Principal mockPrincipal(final String attrName, final String... attrValues) {
        return principalFactory.createPrincipal("user",
                ImmutableMap.of(attrName, attrValues.length == 1 ? attrValues[0] : Arrays.asList(attrValues)));
    }
}
