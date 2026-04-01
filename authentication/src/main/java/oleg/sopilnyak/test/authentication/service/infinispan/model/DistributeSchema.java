package oleg.sopilnyak.test.authentication.service.infinispan.model;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
        className = "DistributeModelSchema",
        schemaPackageName = "authentication.service",
        service = false,
        includeClasses = {
                AccessCredentialsProto.class,
                UserDetailsProto.class
        }
)
public interface DistributeSchema extends GeneratedSchema {
}
