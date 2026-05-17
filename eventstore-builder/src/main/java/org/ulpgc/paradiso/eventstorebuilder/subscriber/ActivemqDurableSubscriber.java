package org.ulpgc.paradiso.eventstorebuilder.subscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.paradiso.eventstorebuilder.store.EventFileStore;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

public class ActivemqDurableSubscriber implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public ActivemqDurableSubscriber(String brokerUrl, String clientId,
                                     List<String> topicNames,
                                     EventFileStore store) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        this.connection = factory.createConnection();
        this.connection.setClientID(clientId);
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        subscribeToTopics(topicNames, store);
        this.connection.start();
    }

    private void subscribeToTopics(List<String> topicNames, EventFileStore store) throws Exception {
        for (String topicName : topicNames) {
            consumers.add(subscribeTo(topicName, store));
        }
    }

    private MessageConsumer subscribeTo(String topicName, EventFileStore store) throws Exception {
        Topic topic = session.createTopic(topicName);
        String subscriptionName = "sub-" + topicName;
        MessageConsumer consumer = session.createDurableSubscriber(topic, subscriptionName);
        consumer.setMessageListener(msg -> handleMessage(msg, topicName, store));
        System.out.println("[EventStoreBuilder] Suscripción durable registrada: "
                + subscriptionName + " en topic " + topicName);
        return consumer;
    }

    private void handleMessage(Message message, String topicName, EventFileStore store) {
        try {
            if (message instanceof TextMessage textMessage) {
                store.append(topicName, textMessage.getText());
                System.out.println("[EventStoreBuilder] Guardado evento de topic: " + topicName);
            } else {
                System.err.println("[EventStoreBuilder] Mensaje ignorado: no es TextMessage");
            }
        } catch (Exception e) {
            System.err.println("[EventStoreBuilder] Error guardando evento de "
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