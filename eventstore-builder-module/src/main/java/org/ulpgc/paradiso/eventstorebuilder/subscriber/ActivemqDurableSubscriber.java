package org.ulpgc.paradiso.eventstorebuilder.subscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.paradiso.eventstorebuilder.store.EventFileStore;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.List;

public class ActivemqDurableSubscriber implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public ActivemqDurableSubscriber(String brokerUrl,
                                     String clientId,
                                     List<String> topicNames,
                                     EventFileStore store) throws Exception {

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        this.connection = factory.createConnection();
        this.connection.setClientID(clientId);
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        for (String topicName : topicNames) {
            Topic topic = session.createTopic(topicName);
            String subscriptionName = "sub-" + topicName;

            MessageConsumer consumer = session.createDurableSubscriber(topic, subscriptionName);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String jsonEvent = textMessage.getText();
                        store.append(topicName, jsonEvent);

                        System.out.println("[EventStoreBuilder] Guardado evento de topic: "
                                + topicName);
                    } else {
                        System.err.println("[EventStoreBuilder] Mensaje ignorado: no es TextMessage");
                    }
                } catch (Exception e) {
                    System.err.println("[EventStoreBuilder] Error guardando evento de "
                            + topicName + ": " + e.getMessage());
                }
            });

            consumers.add(consumer);

            System.out.println("[EventStoreBuilder] Suscripción durable registrada: "
                    + subscriptionName + " en topic " + topicName);
        }

        this.connection.start();
    }

    @Override
    public void close() throws Exception {
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