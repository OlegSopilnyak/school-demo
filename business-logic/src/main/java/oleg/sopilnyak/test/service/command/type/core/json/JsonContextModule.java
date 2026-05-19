package oleg.sopilnyak.test.service.command.type.core.json;

import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

/**
 * ObjectMapper:Module: The module to serialize/deserialize The Context
 *
 * @see Context
 * @see CommandsFactoriesFarm
 * @see CommandContextSerializer
 * @see CommandContextDeserializer
 */
public class JsonContextModule<T> extends SimpleModule {
    // the reference to command factories farm
    private final transient CommandsFactoriesFarm<? extends RootCommand<T>> farm;

    public JsonContextModule(ApplicationContext context, CommandsFactoriesFarm<? extends RootCommand<T>> farm) {
        Assert.notNull(context, "ApplicationContext mustn't be null");
        // assign the commands farm
        this.farm = farm;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setupModule(final SetupContext setupContext) {
        final SimpleSerializers serializers = new SimpleSerializers();
        final SimpleDeserializers deserializers = new SimpleDeserializers();
        // add serializer/deserializer for command context
        serializers.addSerializer(Context.class, new CommandContextSerializer());
        deserializers.addDeserializer(Context.class, new CommandContextDeserializer<>(farm));
        // accept modified serializer/deserializer
        setupContext.addSerializers(serializers);
        setupContext.addDeserializers(deserializers);
    }
}
