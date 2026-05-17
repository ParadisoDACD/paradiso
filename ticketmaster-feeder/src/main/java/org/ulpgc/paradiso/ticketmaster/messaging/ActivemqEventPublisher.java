package org.ulpgc.paradiso.ticketmaster.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

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
    public void close() {
        closeQuietly(producer::close);
        closeQuietly(session::close);
        closeQuietly(connection::close);
    }

    private void closeQuietly(JmsCloseOperation operation) {
        try {
            operation.close();
        } catch (JMSException ignored) {
        }
    }

    @FunctionalInterface
    private interface JmsCloseOperation {
        void close() throws JMSException;
    }
}