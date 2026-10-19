package com.crediticio.evaluationquery.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Tokens de servicio de prueba con el contrato S1 que firma scoring-api (TokenServicioJwtFirmante): RS256 con
 * kid y typ scoring-svc+jwt; iss y sub scoring-api, aud evaluation-query-api-internal, scope
 * recomendaciones:generar, iat, exp (60 s) y jti, sin role.
 */
public final class TokensServicioPrueba {

    public static final String TIPO = "scoring-svc+jwt";

    private TokensServicioPrueba() {
    }

    public static JWTClaimsSet.Builder claimsValidos() {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer("scoring-api")
                .subject("scoring-api")
                .audience("evaluation-query-api-internal")
                .claim("scope", "recomendaciones:generar")
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(60)))
                .jwtID(UUID.randomUUID().toString());
    }

    public static String firmar(KeyPair par, String kid, JWTClaimsSet claims) {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid)
                .type(new JOSEObjectType(TIPO)).build(), claims);
        try {
            jwt.sign(new RSASSASigner(par.getPrivate()));
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return jwt.serialize();
    }
}
