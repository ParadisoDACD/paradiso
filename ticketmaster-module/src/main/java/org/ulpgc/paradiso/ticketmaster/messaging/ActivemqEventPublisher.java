package org.ulpgc.paradiso.ticketmaster.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class ActivemqEventPublisher implements EventPublisher {

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public ActivemqEventPublisher(String brokerUrl, String topicName) throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        this.connection = factory.createConnection();
        this.connection.start();

        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = session.createTopic(topicName);
        this.producer = session.createProducer(topic);
        this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    @Override
    public void publish(String jsonEvent) throws Exception {
        TextMessage message = session.createTextMessage(jsonEvent);
        producer.send(message);
    }

    @Override
    public void close() throws Exception {
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception ignored) {
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }
}