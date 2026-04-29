package com.tp2.sync;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class BranchPublisher {
    private static final String SELECT_PENDING_SQL = """
            SELECT id, product_code, quantity, unit_price, sold_at, office
            FROM sales
            WHERE synced = 0
            ORDER BY id
            """;
    private static final String MARK_SYNCED_SQL = "UPDATE sales SET synced = true WHERE id = ?";

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || (!args[0].equals("bo1") && !args[0].equals("bo2"))) {
            throw new IllegalArgumentException("Usage: BranchPublisher <bo1|bo2>");
        }

        String branchKey = args[0];
        AppConfig config = new AppConfig();
        List<SaleRecord> pendingRows = readPendingSales(config, branchKey);

        if (pendingRows.isEmpty()) {
            System.out.println("No pending sales for " + branchKey);
            return;
        }

        publishRows(config, branchKey, pendingRows);
        markRowsAsSynced(config, branchKey, pendingRows);
        System.out.println("Published and marked synced rows: " + pendingRows.size() + " from " + branchKey);
    }

    private static List<SaleRecord> readPendingSales(AppConfig config, String branchKey) throws SQLException {
        List<SaleRecord> rows = new ArrayList<>();
        try (java.sql.Connection db = config.openDbConnection(branchKey);
             PreparedStatement ps = db.prepareStatement(SELECT_PENDING_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new SaleRecord(
                        rs.getLong("id"),
                        rs.getString("product_code"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getTimestamp("sold_at").toLocalDateTime(),
                        rs.getString("office")
                ));
            }
        }
        return rows;
    }

    private static void publishRows(AppConfig config, String branchKey, List<SaleRecord> rows) throws Exception {
        String queueName = config.queueNameForOffice(branchKey);
        try (Connection rabbitConn = config.rabbitFactory().newConnection();
             Channel channel = rabbitConn.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);
            for (SaleRecord row : rows) {
                String payload = row.toMessage();
                channel.basicPublish("", queueName, null, payload.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void markRowsAsSynced(AppConfig config, String branchKey, List<SaleRecord> rows) throws SQLException {
        try (java.sql.Connection db = config.openDbConnection(branchKey)) {
            db.setAutoCommit(false);
            try (PreparedStatement update = db.prepareStatement(MARK_SYNCED_SQL)) {
                for (SaleRecord row : rows) {
                    update.setLong(1, row.id());
                    update.addBatch();
                }
                update.executeBatch();
            }
            db.commit();
        }
    }
}
