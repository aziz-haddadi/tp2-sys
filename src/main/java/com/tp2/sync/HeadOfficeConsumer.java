package com.tp2.sync;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;

public class HeadOfficeConsumer {
    private static final String INSERT_SQL = """
            INSERT INTO product_sales (`date`, region, product, qty, cost, amt, tax, total)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();

        try (Connection rabbitConn = config.rabbitFactory().newConnection();
             Channel channel = rabbitConn.createChannel()) {
            String queueBo1 = config.queueNameForOffice("bo1");
            String queueBo2 = config.queueNameForOffice("bo2");
            channel.queueDeclare(queueBo1, true, false, false, null);
            channel.queueDeclare(queueBo2, true, false, false, null);

            DeliverCallback callback = (tag, delivery) -> {
                String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
                SaleRecord sale = SaleRecord.fromMessage(payload);
                persistInHeadOffice(config, sale);
                System.out.println("Synced to HO: " + payload);
            };

            channel.basicConsume(queueBo1, true, callback, consumerTag -> { });
            channel.basicConsume(queueBo2, true, callback, consumerTag -> { });

            System.out.println("HeadOfficeConsumer waiting for BO messages...");
            Thread.currentThread().join();
        }
    }

    private static void persistInHeadOffice(AppConfig config, SaleRecord sale) {
        try (java.sql.Connection db = config.openDbConnection("ho");
             PreparedStatement ps = db.prepareStatement(INSERT_SQL)) {
            ps.setDate(1, java.sql.Date.valueOf(sale.date()));
            ps.setString(2, sale.region());
            ps.setString(3, sale.product());
            ps.setInt(4, sale.qty());
            ps.setBigDecimal(5, sale.cost());
            ps.setBigDecimal(6, sale.amt());
            ps.setBigDecimal(7, sale.tax());
            ps.setBigDecimal(8, sale.total());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist record in HO", e);
        }
    }
}
