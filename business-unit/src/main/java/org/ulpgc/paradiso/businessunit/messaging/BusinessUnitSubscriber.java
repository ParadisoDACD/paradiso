package org.ulpgc.paradiso.businessunit.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.List;

public class BusinessUnitSubscriber implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public BusinessUnitSubscriber(String brokerUrl,
                                  String clientId,
                                  List<String> topicNames,
                                  BusinessEventProcessor processor) throws JMSException {
        this(brokerUrl, clientId, topicNames, processor, () -> {
        });
    }

    public BusinessUnitSubscriber(String brokerUrl,
                                  String clientId,
                                  List<String> topicNames,
                                  BusinessEventProcessor processor,
                                  Runnable connectionLostHandler) throws JMSException {

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        this.connection = factory.createConnection();
        this.connection.setClientID(clientId);
        this.connection.setExceptionListener(exception -> {
            System.err.println("[BusinessUnit] Conexión JMS interrumpida: " + exception.getMessage());
            connectionLostHandler.run();
        });

        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        for (String topicName : topicNames) {
            Topic topic = session.createTopic(topicName);
            String subscriptionName = "bu-" + topicName;

            MessageConsumer consumer = session.createDurableSubscriber(topic, subscriptionName);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String text = textMessage.getText();
                        processor.process(topicName, text);
                        System.out.println("[BusinessUnit] [real-time] Evento de " + topicName);
                    }
                } catch (Exception e) {
                    System.err.println("[BusinessUnit] Error procesando mensaje de "
                            + topicName + ": " + e.getMessage());
                }
            });

            consumers.add(consumer);
            System.out.println("[BusinessUnit] Suscripción durable registrada: " + subscriptionName);
        }

        connection.start();
        System.out.println("[BusinessUnit] Subscriber activo. Escuchando mensajes...");
    }

    @Override
    public void close() {
        System.out.println("[BusinessUnit] Cerrando subscriber...");

        for (MessageConsumer consumer : consumers) {
            try {
                consumer.close();
            } catch (Exception ignored) {
            }
        }

        try {
            session.close();
        } catch (Exception ignored) {
        }

        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}