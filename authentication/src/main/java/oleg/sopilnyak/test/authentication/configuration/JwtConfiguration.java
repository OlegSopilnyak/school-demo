package oleg.sopilnyak.test.authentication.configuration;

import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.ApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.impl.JwtServiceImpl;
import oleg.sopilnyak.test.authentication.service.infinispan.DistributeAccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.infinispan.DistributeApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.infinispan.DistributeUserService;
import oleg.sopilnyak.test.authentication.service.local.LocalAccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.local.LocalApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.local.LocalUserService;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import java.net.InetAddress;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@RequiredArgsConstructor
public class JwtConfiguration {
    private final PersistenceFacade persistenceFacade;

    @Value("${application.infinispan.cluster.name:cluster}")
    private String clusterName;

    @Bean
    public JwtService jwtService() {
        return new JwtServiceImpl();
    }

    @Bean
    @Profile("distribute")
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

    @Bean
    @Profile("distribute")
    public AccessTokensStorage infinispanTokenStorage() {
        return new DistributeAccessTokensStorage(infinispanEmbeddedCacheManager(), jwtService());
    }

    @Bean
    @Profile("distribute")
    public UserService ditstributeUserService() {
        return new DistributeUserService(persistenceFacade, infinispanTokenStorage());
    }

    @Bean
    @Profile("distribute")
    public ApplicationAccessFacade distributeApplicationAccessFacade() {
        return new DistributeApplicationAccessFacade(ditstributeUserService(), jwtService(), infinispanTokenStorage());
    }

    @Bean
    @Profile("!distribute")
    public AccessTokensStorage localTokenStorage() {
        return new LocalAccessTokensStorage(jwtService());
    }

    @Bean
    @Profile("!distribute")
    public UserService localUserService() {
        return new LocalUserService(persistenceFacade, localTokenStorage());
    }

    @Bean
    @Profile("!distribute")
    public ApplicationAccessFacade localApplicationAccessFacade() {
        return new LocalApplicationAccessFacade(localUserService(), jwtService(), localTokenStorage());
    }
}
