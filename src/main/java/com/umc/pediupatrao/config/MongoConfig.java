package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.StatusPedido;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StatusPedidoReadingConverter()));
    }

    /** Permite ler pedidos antigos gravados com status em texto livre (ex.: EM_PREPARO). */
    @ReadingConverter
    static class StatusPedidoReadingConverter implements Converter<String, StatusPedido> {
        @Override
        public StatusPedido convert(String valor) {
            return StatusPedido.deValorLegado(valor);
        }
    }
}
