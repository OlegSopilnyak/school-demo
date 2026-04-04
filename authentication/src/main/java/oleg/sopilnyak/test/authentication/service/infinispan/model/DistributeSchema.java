package oleg.sopilnyak.test.authentication.service.infinispan.model;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
        dependsOn = {
                org.infinispan.protostream.types.java.CommonTypes.class,
                org.infinispan.protostream.types.java.CommonContainerTypes.class
        }, schemaFileName = "distribute.model.proto", schemaFilePath = "proto",
        schemaPackageName = "authentication.service",
        service = false,
        includeClasses = {
                AccessCredentialsProto.class,
                UserDetailsProto.class,
                ProhibitedTokensProto.class
        }
)
public interface DistributeSchema extends GeneratedSchema {
}
