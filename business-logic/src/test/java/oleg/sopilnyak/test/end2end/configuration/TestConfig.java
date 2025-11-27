package oleg.sopilnyak.test.end2end.configuration;

import static org.mockito.Mockito.spy;

import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.mapstruct.factory.Mappers;

@Configuration
public class TestConfig {
    @Bean
    public BusinessMessagePayloadMapper payloadMapper(){
        return spy(Mappers.getMapper(BusinessMessagePayloadMapper.class));
    }
}
