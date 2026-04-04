package oleg.sopilnyak.test.authentication.service.infinispan.model;

import java.util.Collection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import lombok.Setter;

/**
 * Model:  Protobuf Entity for tokens in the black-list (prohibited tokens)
 */
@Setter
public class ProhibitedTokensProto {
    // authorities granted to the user. Cannot be null.
    private Collection<String> prohibitedTokens;

    @ProtoFactory
    public static ProhibitedTokensProto of(Collection<String> prohibitedTokens) {
        return new ProhibitedTokensProto(prohibitedTokens);
    }

    @ProtoField(number = 1)
    public Collection<String> getProhibitedTokens() {
        return prohibitedTokens;
    }

    public boolean isEmpty() {
        return prohibitedTokens.isEmpty();
    }

    public boolean hasToken(String token) {
        return prohibitedTokens.contains(token);
    }

    // private methods
    private ProhibitedTokensProto(Collection<String> prohibitedTokens) {
        this.prohibitedTokens = prohibitedTokens;
    }
}
