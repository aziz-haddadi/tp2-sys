package com.tp2.sync;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class HeadOfficeConsumer {
    private static final String UPSERT_SQL = """
            INSERT INTO sales_aggregated (source_office, source_sale_id, product_code, quantity, unit_price, sold_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                product_code = VALUES(product_code),
                quantity = VALUES(quantity),
                unit_price = VALUES(unit_price),
                sold_at = VALUES(sold_at)
            """;

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        ConnectionFactory factory = config.rabbitFactory();

        try (Connection rabbitConn = factory.newConnection();
             Channel channel = rabbitConn.createChannel()) {
            String queueBo1 = config.queueNameForOffice("bo1");
            String queueBo2 = config.queueNameForOffice("bo2");
            channel.queueDeclare(queueBo1, true, false, false, null);
            channel.queueDeclare(queueBo2, true, false, false, null);

            DeliverCallback callback = (consumerTag, delivery) -> {
                String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
                SaleRecord record = SaleRecord.fromMessage(payload);
                persistInHeadOffice(config, record);
                System.out.println("Synced to HO: " + payload);
            };

            channel.basicConsume(queueBo1, true, callback, consumerTag -> { });
            channel.basicConsume(queueBo2, true, callback, consumerTag -> { });

            System.out.println("HeadOfficeConsumer waiting for BO messages...");
            Thread.currentThread().join();
        }
    }

    private static void persistInHeadOffice(AppConfig config, SaleRecord record) {
        try (java.sql.Connection db = config.openDbConnection("ho");
             PreparedStatement ps = db.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, record.office());
            ps.setLong(2, record.id());
            ps.setString(3, record.productCode());
            ps.setInt(4, record.quantity());
            ps.setBigDecimal(5, record.unitPrice());
            ps.setTimestamp(6, Timestamp.valueOf(record.soldAt()));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist record in HO", e);
        }
    }
}
