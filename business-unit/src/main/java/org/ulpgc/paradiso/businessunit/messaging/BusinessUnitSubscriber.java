package org.ulpgc.paradiso.businessunit.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.paradiso.businessunit.event.BusinessEventProcessor;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

public class BusinessUnitSubscriber implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public BusinessUnitSubscriber(String brokerUrl, String clientId,
                                  List<String> topicNames,
                                  BusinessEventProcessor processor) throws JMSException {
        this(brokerUrl, clientId, topicNames, processor, () -> {});
    }

    public BusinessUnitSubscriber(String brokerUrl, String clientId,
                                  List<String> topicNames,
                                  BusinessEventProcessor processor,
                                  Runnable connectionLostHandler) throws JMSException {
        this.connection = createConnection(brokerUrl, clientId, connectionLostHandler);
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        subscribeToTopics(topicNames, processor);
        connection.start();
        System.out.println("[BusinessUnit] Subscriber activo. Escuchando mensajes...");
    }

    private Connection createConnection(String brokerUrl, String clientId,
                                        Runnable onLost) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection conn = factory.createConnection();
        conn.setClientID(clientId);
        conn.setExceptionListener(ex -> {
            System.err.println("[BusinessUnit] Conexión JMS interrumpida: " + ex.getMessage());
            onLost.run();
        });
        return conn;
    }

    private void subscribeToTopics(List<String> topicNames,
                                   BusinessEventProcessor processor) throws JMSException {
        for (String topicName : topicNames) {
            consumers.add(subscribeTo(topicName, processor));
        }
    }

    private MessageConsumer subscribeTo(String topicName,
                                        BusinessEventProcessor processor) throws JMSException {
        Topic topic = session.createTopic(topicName);
        String subscriptionName = "bu-" + topicName;
        MessageConsumer consumer = session.createDurableSubscriber(topic, subscriptionName);
        consumer.setMessageListener(msg -> handleMessage(msg, topicName, processor));
        System.out.println("[BusinessUnit] Suscripción durable registrada: " + subscriptionName);
        return consumer;
    }

    private void handleMessage(Message message, String topicName, BusinessEventProcessor processor) {
        try {
            if (message instanceof TextMessage textMessage) {
                processor.process(topicName, textMessage.getText());
                System.out.println("[BusinessUnit] [real-time] Evento de " + topicName);
            }
        } catch (Exception e) {
            System.err.println("[BusinessUnit] Error procesando mensaje de "
                    + topicName + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        consumers.forEach(consumer -> closeQuietly(consumer::close));
        closeQuietly(session::close);
        closeQuietly(connection::close);
    }

    @FunctionalInterface
    private interface JmsCloseOperation {
        void close() throws JMSException;
    }

    private void closeQuietly(JmsCloseOperation operation) {
        try {
            operation.close();
        } catch (JMSException ignored) {
        }
    }
}