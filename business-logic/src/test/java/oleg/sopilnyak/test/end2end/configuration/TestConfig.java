package oleg.sopilnyak.test.end2end.configuration;

import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.spy;

@Configuration
public class TestConfig {
    @Bean
    public BusinessMessagePayloadMapper payloadMapper(){
        return spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
    }
}
