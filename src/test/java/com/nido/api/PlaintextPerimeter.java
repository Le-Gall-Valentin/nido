package com.nido.api;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** The columns 0.13.1 moves from clear to encrypted — the table of the spec — and what the schema says of them. */
final class PlaintextPerimeter {

    record Column(String table, String name, boolean required) {
        String encrypted() {
            return name + "_encrypted";
        }

        String qualified() {
            return table + "." + name;
        }
    }

    static final List<Column> COLUMNS = List.of(
        new Column("shopping_items", "name", true),
        new Column("shopping_categories", "name", true),
        new Column("tasks", "title", true),
        new Column("task_subtasks", "text", true),
        new Column("recurring_task_series", "title", true),
        new Column("recurring_task_series_subtask_templates", "text", true),
        new Column("kitchen_recipes", "name", true),
        new Column("kitchen_recipes", "description", false),
        new Column("kitchen_recipes", "note", false),
        new Column("kitchen_recipe_ingredients", "name", true),
        new Column("kitchen_recipe_steps", "text", true),
        new Column("finance_categories", "label", true),
        new Column("spaces", "name", true),
        new Column("spaces", "description", false));

    private PlaintextPerimeter() {}

    static boolean exists(JdbcTemplate jdbc, String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM information_schema.columns
                           WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?)""",
            Boolean.class, table, column));
    }

    static List<String> encryptedColumnsMissingOrNotText(JdbcTemplate jdbc) {
        return COLUMNS.stream()
            .filter(c -> !"text".equals(dataType(jdbc, c.table(), c.encrypted())))
            .map(c -> c.table() + "." + c.encrypted())
            .toList();
    }

    /** The columns in clear that still exist, emptied or not. */
    static List<String> columnsInClear(JdbcTemplate jdbc) {
        return COLUMNS.stream().filter(c -> exists(jdbc, c.table(), c.name())).map(Column::qualified).toList();
    }

    /** How many values are still in clear, over every column that still exists. */
    static long valuesInClear(JdbcTemplate jdbc) {
        return COLUMNS.stream()
            .filter(c -> exists(jdbc, c.table(), c.name()))
            .mapToLong(c -> jdbc.queryForObject(
                "SELECT count(*) FROM " + c.table() + " WHERE " + c.name() + " IS NOT NULL", Long.class))
            .sum();
    }

    static List<String> requiredEncryptedColumnsAcceptingNull(JdbcTemplate jdbc) {
        return COLUMNS.stream()
            .filter(Column::required)
            .filter(c -> "YES".equals(jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?""",
                String.class, c.table(), c.encrypted())))
            .map(c -> c.table() + "." + c.encrypted())
            .toList();
    }

    private static String dataType(JdbcTemplate jdbc, String table, String column) {
        List<String> found = jdbc.queryForList("""
            SELECT data_type FROM information_schema.columns
            WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?""",
            String.class, table, column);
        return found.isEmpty() ? null : found.getFirst();
    }
}
