package oleg.sopilnyak.test.authentication.configuration;

import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.impl.JwtServiceImpl;
import oleg.sopilnyak.test.authentication.service.infinispan.AccessTokensStorageDistributedImpl;
import oleg.sopilnyak.test.authentication.service.local.AccessTokensStorageLocalImpl;

import java.net.InetAddress;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.SneakyThrows;

@Configuration
public class JwtConfiguration {

    @Value("${application.infinispan.cluster.name:cluster}")
    private String clusterName;

    @Bean
    public JwtService jwtService() {
        return new JwtServiceImpl();
    }

    @Profile("distribute")
    @Bean
    @SneakyThrows
    public DefaultCacheManager infinispanEmbeddedCacheManager() {
        // Setup up a clustered cache manager
        final GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
        global.transport().defaultTransport()
                .clusterName(clusterName)
                .machineId(InetAddress.getLocalHost().getHostName())
        ;
        return new DefaultCacheManager(global.build());
    }

    @Profile("distribute")
    @Bean
    public AccessTokensStorage infinispanTokenStorage() {
        return new AccessTokensStorageDistributedImpl(infinispanEmbeddedCacheManager(), jwtService());
    }

    @Profile("!distribute")
    @Bean
    public AccessTokensStorage localTokenStorage() {
        return new AccessTokensStorageLocalImpl(jwtService());
    }
}
