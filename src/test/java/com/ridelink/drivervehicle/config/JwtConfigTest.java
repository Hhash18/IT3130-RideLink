package com.ridelink.drivervehicle.config;



import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;
class JwtConfigTest {
    String token(String secret,JWSAlgorithm algorithm,String subject,Instant expires) throws Exception {
        var claims=new JWTClaimsSet.Builder().subject(subject).claim("role","PASSENGER").claim("email","passenger@example.test");
        if(expires!=null) claims.expirationTime(Date.from(expires));
        var token=new SignedJWT(new JWSHeader(algorithm),claims.build());token.sign(new MACSigner(secret));return token.serialize();
    }
    @Test void validatesAllAccountKeyLengthsAndSingleRoleClaim() throws Exception {
        for(int length:new int[]{32,48,64}) {
            String secret="x".repeat(length);
            var decoder=new JwtConfig().jwtDecoder(secret);
            var algorithm=length==64 ? JWSAlgorithm.HS512 : length==48 ? JWSAlgorithm.HS384 : JWSAlgorithm.HS256;
            assertThat(decoder.decode(token(secret,algorithm,"1",Instant.now().plusSeconds(300))).getClaimAsString("role")).isEqualTo("PASSENGER");
        }
    }
    @Test void rejectsInvalidSignatureExpiryAndSubject() throws Exception {
        String secret="x".repeat(32);var decoder=new JwtConfig().jwtDecoder(secret);
        for(String invalid:new String[]{token("y".repeat(32),JWSAlgorithm.HS256,"1",Instant.now().plusSeconds(300)),
            token(secret,JWSAlgorithm.HS256,"1",Instant.now().minusSeconds(300)),token(secret,JWSAlgorithm.HS256,"",Instant.now().plusSeconds(300)),
            token(secret,JWSAlgorithm.HS256,"1",null)})
            assertThatThrownBy(() -> decoder.decode(invalid)).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }
    @Test void rejectsMissingSecret() {
        var config=new JwtConfig();
        assertThatThrownBy(() -> config.jwtDecoder("")).isInstanceOf(IllegalArgumentException.class);
    }
}
