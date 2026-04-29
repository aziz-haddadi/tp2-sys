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
            SELECT `date`, region, product, qty, cost, amt, tax, total
            FROM product_sales
            WHERE synced = 0
            ORDER BY `date`, region, product
            """;
    private static final String MARK_SYNCED_SQL = """
            UPDATE product_sales
            SET synced = true
            WHERE `date` = ? AND region = ? AND product = ? AND qty = ? AND cost = ? AND amt = ? AND tax = ? AND total = ? AND synced = 0
            LIMIT 1
            """;

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
                        rs.getDate("date").toLocalDate(),
                        rs.getString("region"),
                        rs.getString("product"),
                        rs.getInt("qty"),
                        rs.getBigDecimal("cost"),
                        rs.getBigDecimal("amt"),
                        rs.getBigDecimal("tax"),
                        rs.getBigDecimal("total")
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
                    update.setDate(1, java.sql.Date.valueOf(row.date()));
                    update.setString(2, row.region());
                    update.setString(3, row.product());
                    update.setInt(4, row.qty());
                    update.setBigDecimal(5, row.cost());
                    update.setBigDecimal(6, row.amt());
                    update.setBigDecimal(7, row.tax());
                    update.setBigDecimal(8, row.total());
                    update.addBatch();
                }
                update.executeBatch();
            }
            db.commit();
        }
    }
}
