package br.com.pediupatrao.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
@Configuration
public class MongoConfig {
    @Bean MongoTransactionManager transactionManager(MongoDatabaseFactory factory) { return new MongoTransactionManager(factory); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
