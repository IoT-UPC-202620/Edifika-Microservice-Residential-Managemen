package com.residential.management.demo.residential.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Topología del lado Residential:
 * - Exchange topic compartido `edifika.events` (durable).
 * - Cola propia `residential.resident-link-completion` ligada a `iam.resident.user-created`,
 *   con DLX `edifika.dlx` y DLQ `residential.resident-link-completion.dlq`.
 * La cola `iam.resident-provisioning` la declara el servicio IAM.
 */
@Configuration
public class RabbitMqConfiguration {

    public static final String EVENTS_EXCHANGE = "edifika.events";
    public static final String DLX_EXCHANGE = "edifika.dlx";
    public static final String USER_CREATED_ROUTING_KEY = "iam.resident.user-created";
    public static final String LINK_COMPLETION_QUEUE = "residential.resident-link-completion";
    public static final String LINK_COMPLETION_DLQ = LINK_COMPLETION_QUEUE + ".dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue linkCompletionQueue() {
        return QueueBuilder.durable(LINK_COMPLETION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LINK_COMPLETION_DLQ)
                .build();
    }

    @Bean
    public Queue linkCompletionDlq() {
        return QueueBuilder.durable(LINK_COMPLETION_DLQ).build();
    }

    @Bean
    public Binding linkCompletionBinding() {
        return BindingBuilder.bind(linkCompletionQueue())
                .to(eventsExchange())
                .with(USER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding linkCompletionDlqBinding() {
        return BindingBuilder.bind(linkCompletionDlq())
                .to(dlxExchange())
                .with(LINK_COMPLETION_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    // 3 intentos, back-off exponencial 1s x2 con tope 10s. Stateless: el mensaje queda
    // unacked durante los reintentos; agotados, RejectAndDontRequeueRecoverer lo rechaza
    // (requeue=false) y la DLX lo rutea a la DLQ de la cola.
    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1_000L, 2.0, 10_000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            RetryOperationsInterceptor retryOperationsInterceptor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAdviceChain(retryOperationsInterceptor);
        return factory;
    }
}
