package com.nido.api;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.infrastructure.sealing.EncryptionBackfillRunner;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SealedColumns;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.port.out.SpaceRepository;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.Subtask;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 0.14.0 on a database written by an earlier version. What that version stored in clear, and what it encrypted
 * without the place it belongs to, is sealed at the first start, before anyone can be served, and the columns that
 * held values in clear go at the next start. Every start validates the schema as production does — the shared test
 * context does not.
 *
 * <p>The earlier version is 0.11's schema (migrated up to 064), so this is also an installation that
 * skips versions: the tables of the perimeter have not changed since.
 */
@IntegrationTestConfig
class EncryptionBackfillIT {

    private static final String KEY = "the-key-this-installation-always-had-32+";
    private static final String OTHER = "a-key-someone-typed-by-mistake-32-chars+";
    private static final String JWT = "the-jwt-secret-of-this-installation-32+";
    private static final String SALT_A = "0123456789abcdef0123456789abcdef";
    private static final String SALT_B = "fedcba9876543210fedcba9876543210";

    @Autowired JdbcTemplate sharedDatabase;
    @TempDir Path dataDir;

    private String database;
    private JdbcTemplate db;
    private UUID admin;
    private UUID spaceA;
    private UUID spaceB;

    @BeforeEach
    void aDatabaseOfItsOwn() {
        database = InstallationTestSupport.createDatabase(sharedDatabase);
        db = new JdbcTemplate(new DriverManagerDataSource(SharedContainers.jdbcUrl(database),
            SharedContainers.POSTGRES.getUsername(), SharedContainers.POSTGRES.getPassword()));
    }

    @AfterEach
    void drop() {
        InstallationTestSupport.dropDatabase(sharedDatabase, database);
    }

    private static String[] arguments(String key) {
        return new String[] {"nido.encryption.secret=" + key, "nido.jwt.secret=" + JWT, "spring.jpa.hibernate.ddl-auto=validate"};
    }

    private ConfigurableApplicationContext start(String key) {
        return InstallationTestSupport.boot(database, dataDir, arguments(key));
    }

    private UUID id(String sql, Object... args) {
        return db.queryForObject(sql, UUID.class, args);
    }

    /** One row in clear in every table 0.13.1 encrypts, written the way 0.13.0 wrote them, over two spaces. */
    private void writtenInClearByAnEarlierVersion() throws Exception {
        InstallationTestSupport.migrateUpTo(database, "064-");
        admin = id("INSERT INTO users (username, email, role) VALUES ('admin', 'admin@example.fr', 'SUPER_ADMIN') RETURNING id");
        spaceA = id("INSERT INTO spaces (type, name, description, accent, glyph, encryption_salt) "
            + "VALUES ('SHARED', 'Famille Le Gall 🏡', 'Rue des Lilas, 3ᵉ étage', '#c17a5c', '🏡', ?) RETURNING id", SALT_A);
        spaceB = id("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) "
            + "VALUES ('SHARED', 'Coloc', '#c17a5c', '🏡', ?) RETURNING id", SALT_B);
        UUID aisle = id("INSERT INTO shopping_categories (space_id, name, position) VALUES (?, 'Épicerie', 0) RETURNING id", spaceA);
        db.update("INSERT INTO shopping_items (space_id, category_id, name, position) VALUES (?, ?, 'Crème fraîche 🥛', 0)", spaceA, aisle);
        UUID task = id("INSERT INTO tasks (space_id, title, status, priority) VALUES (?, 'Rendez-vous oncologue', 'TODO', 'HIGH') RETURNING id", spaceA);
        db.update("INSERT INTO task_subtasks (task_id, position, text) VALUES (?, 0, 'Apporter l’ordonnance')", task);
        UUID series = id("""
            INSERT INTO recurring_task_series (space_id, title, priority, interval_type, interval_count, anchor_date,
                                               lead_interval_type, lead_interval_count)
            VALUES (?, 'Sortir les poubelles', 'MED', 'WEEKLY', 1, DATE '2026-10-05', 'DAILY', 0) RETURNING id""", spaceB);
        db.update("INSERT INTO recurring_task_series_subtask_templates (series_id, position, text) VALUES (?, 0, 'Trier le verre')", series);
        UUID recipe = id("""
            INSERT INTO kitchen_recipes (space_id, name, description, note, category, minutes, reference_portions)
            VALUES (?, 'Œufs brouillés', 'Le dimanche matin', '', 'PLAT', 10, 2) RETURNING id""", spaceB);
        db.update("INSERT INTO kitchen_recipe_ingredients (recipe_id, position, name, quantity, unit) VALUES (?, 0, 'Œufs', 4, 'PIECE')", recipe);
        db.update("INSERT INTO kitchen_recipe_steps (recipe_id, position, text) VALUES (?, 0, 'Battre les œufs')", recipe);
        db.update("INSERT INTO finance_categories (space_id, label, color, icon, type, is_default) "
            + "VALUES (?, 'Alimentation', '#f59e0b', 'Utensils', 'EXPENSE', true)", spaceA);
        db.update("INSERT INTO finance_categories (space_id, label, color, icon, type, is_default) "
            + "VALUES (?, 'Cantine des enfants', '#6366f1', 'Home', 'EXPENSE', true)", spaceA);
        TextEncryptor v1 = Encryptors.delux(KEY, SALT_A);
        UUID holidays = id("INSERT INTO finance_categories (space_id, label_encrypted, color, icon, type, is_default) "
            + "VALUES (?, ?, '#ec4899', 'Star', 'EXPENSE', false) RETURNING id", spaceA, v1.encrypt("Vacances"));
        for (String[] spent : new String[][] {{"Loyer", "850.00"}, {"Café", "3.50"}}) {
            db.update("INSERT INTO finance_transactions (space_id, label_encrypted, amount_encrypted, type, category_id, date) "
                + "VALUES (?, ?, ?, 'EXPENSE', ?, DATE '2026-10-01')", spaceA, v1.encrypt(spent[0]), v1.encrypt(spent[1]), holidays);
        }
        db.update("INSERT INTO calendar_events (space_id, title_encrypted, all_day, start_date, end_date, created_by) "
            + "VALUES (?, ?, true, DATE '2026-10-06', DATE '2026-10-06', ?)", spaceB, Encryptors.delux(KEY, SALT_B).encrypt("Dîner chez Mamie"), admin);
    }

    private void everythingReadsAsItWasWritten(ConfigurableApplicationContext app) {
        Space a = app.getBean(SpaceRepository.class).findById(spaceA).orElseThrow();
        assertThat(a.name()).isEqualTo("Famille Le Gall 🏡");
        assertThat(a.description()).isEqualTo("Rue des Lilas, 3ᵉ étage");
        assertThat(app.getBean(SpaceRepository.class).findById(spaceB).orElseThrow().description()).isNull();
        assertThat(app.getBean(ShoppingCategoryRepository.class).findBySpaceId(spaceA))
            .extracting(ShoppingCategory::name).containsExactly("Épicerie");
        assertThat(app.getBean(ShoppingItemRepository.class).findBySpaceId(spaceA))
            .extracting(ShoppingItem::name).containsExactly("Crème fraîche 🥛");
        Task task = app.getBean(TaskRepository.class).findBySpaceId(spaceA).getFirst();
        assertThat(task.title()).isEqualTo("Rendez-vous oncologue");
        assertThat(task.subtasks()).extracting(Subtask::text).containsExactly("Apporter l’ordonnance");
        RecurringTaskSeries series = app.getBean(RecurringTaskSeriesRepository.class).findBySpaceId(spaceB).getFirst();
        assertThat(series.title()).isEqualTo("Sortir les poubelles");
        assertThat(series.subtaskTemplates()).containsExactly("Trier le verre");
        Recipe recipe = app.getBean(RecipeRepository.class).findBySpaceId(spaceB).getFirst();
        assertThat(recipe.name()).isEqualTo("Œufs brouillés");
        assertThat(recipe.description()).isEqualTo("Le dimanche matin");
        assertThat(recipe.note()).isEmpty();
        assertThat(recipe.ingredients()).extracting(RecipeIngredient::name).containsExactly("Œufs");
        assertThat(recipe.steps()).containsExactly("Battre les œufs");
        assertThat(app.getBean(CategoryRepository.class).findBySpaceId(spaceA)).extracting(Category::label)
            .containsExactlyInAnyOrder("Alimentation", "Cantine des enfants", "Vacances");
        assertThat(app.getBean(TransactionRepository.class).findAllBySpaceId(spaceA))
            .extracting(Transaction::label, t -> t.amount().toPlainString())
            .containsExactlyInAnyOrder(tuple("Loyer", "850.00"), tuple("Café", "3.50"));
        assertThat(app.getBean(CalendarEventRepository.class)
                .findBySpaceIdOverlapping(spaceB, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6)))
            .extracting(CalendarEvent::title).containsExactly("Dîner chez Mamie");
    }

    @Test
    void after_the_upgrade_every_value_is_sealed_amounts_look_alike_and_a_swap_is_refused() throws Exception {
        writtenInClearByAnEarlierVersion();
        try (ConfigurableApplicationContext app = start(KEY)) {
            for (SealedColumns columns : app.getBeansOfType(SealedColumns.class).values()) {
                for (SealedColumn column : columns.columns()) {
                    assertThat(db.queryForObject("SELECT count(*) FROM " + column.table() + " WHERE " + column.column()
                        + " IS NOT NULL AND " + column.column() + " NOT LIKE 'v2:%'", Long.class)).as(column.toString()).isZero();
                }
            }
            assertThat(db.queryForList("SELECT DISTINCT length(amount_encrypted) FROM finance_transactions", Integer.class))
                .as("3.50 and 850.00 take the same room").hasSize(1);

            db.update("""
                UPDATE finance_transactions t SET amount_encrypted = o.amount_encrypted FROM finance_transactions o
                WHERE t.space_id = o.space_id AND t.id <> o.id""");

            assertThatThrownBy(() -> app.getBean(TransactionRepository.class).findAllBySpaceId(spaceA))
                .isInstanceOf(SealedValueRejected.class);
        }
    }

    @Test
    void a_start_stopped_before_its_vacuum_leaves_it_to_the_next() throws Exception {
        writtenInClearByAnEarlierVersion();
        try (ConfigurableApplicationContext ignored = start(KEY)) {
            // seals, vacuums, and owes nothing
        }
        assertThat(db.queryForObject("SELECT vacuum_owed FROM sealing_state", Boolean.class)).isFalse();
        // What a start stopped after its last batch and before its VACUUM leaves: nothing left to seal.
        db.update("UPDATE sealing_state SET vacuum_owed = true");
        long transactionFiles = filenode("finance_transactions");

        try (ConfigurableApplicationContext ignored = start(KEY)) {
            // the next start
        }

        assertThat(filenode("finance_transactions")).as("vacuumed by the next start").isNotEqualTo(transactionFiles);
        assertThat(db.queryForObject("SELECT vacuum_owed FROM sealing_state", Boolean.class)).isFalse();
    }

    @Test
    void two_instances_starting_together_both_start() throws Exception {
        // Liquibase's own lock makes the second wait for the first's migrations, and the sealing lock then makes it
        // find nothing left. Two migrations overlapping, and both succeeding, is SealedValueMigrationIT's to show.
        writtenInClearByAnEarlierVersion();
        // Spring Boot sets logging up for the whole JVM at every start, which two starts at once cannot share: both
        // keep the logging of the test run instead. Two processes, as in production, have one each.
        String loggingSystem = System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ConfigurableApplicationContext>> starts = List.of(pool.submit(() -> start(KEY)), pool.submit(() -> start(KEY)));
            for (Future<ConfigurableApplicationContext> started : starts) {
                started.get().close();
            }
        } finally {
            pool.shutdown();
            if (loggingSystem == null) {
                System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
            } else {
                System.setProperty(LoggingSystem.SYSTEM_PROPERTY, loggingSystem);
            }
        }

        assertThat(PlaintextPerimeter.valuesInClear(db)).isZero();
        assertThat(db.queryForObject("SELECT count(*) FROM finance_transactions WHERE amount_encrypted NOT LIKE 'v2:%'", Long.class))
            .isZero();
    }

    private long filenode(String table) {
        return db.queryForObject("SELECT pg_relation_filenode(?::regclass)", Long.class, table);
    }

    @Test
    void the_first_start_encrypts_everything_before_serving_and_the_next_drops_the_columns() throws Exception {
        writtenInClearByAnEarlierVersion();
        long itemFiles = filenode("shopping_items");
        long eventFiles = filenode("calendar_events");
        long memberFiles = filenode("space_members");
        AtomicLong inClearWhenServing = new AtomicLong(-1);
        ApplicationListener<WebServerInitializedEvent> whenServing = event -> inClearWhenServing.set(
            PlaintextPerimeter.valuesInClear(event.getApplicationContext().getBean(JdbcTemplate.class)));

        try (ConfigurableApplicationContext app = InstallationTestSupport.boot(database, dataDir, List.of(whenServing), arguments(KEY))) {
            assertThat(inClearWhenServing).as("values in clear when the web server opened").hasValue(0);
            everythingReadsAsItWasWritten(app);
        }
        assertThat(PlaintextPerimeter.valuesInClear(db)).isZero();
        assertThat(PlaintextPerimeter.columnsInClear(db)).as("dropped at the next start, not this one").isNotEmpty();
        assertThat(filenode("shopping_items")).as("rewritten by VACUUM FULL").isNotEqualTo(itemFiles);
        assertThat(filenode("calendar_events")).as("its values were sealed, then its old row versions vacuumed").isNotEqualTo(eventFiles);
        assertThat(filenode("space_members")).as("nothing of it is sealed").isEqualTo(memberFiles);

        try (ConfigurableApplicationContext app = start(KEY)) {
            everythingReadsAsItWasWritten(app);
        }
        assertThat(PlaintextPerimeter.columnsInClear(db)).isEmpty();
        assertThat(PlaintextPerimeter.requiredEncryptedColumnsAcceptingNull(db)).isEmpty();
    }

    @Test
    void a_value_written_in_clear_after_the_first_start_wins_and_holds_the_columns_back() throws Exception {
        writtenInClearByAnEarlierVersion();
        try (ConfigurableApplicationContext ignored = start(KEY)) {
            // the first start encrypts
        }
        UUID item = db.queryForObject("SELECT id FROM shopping_items", UUID.class);
        // What 0.13.0 does if started again on this database: write the name in clear, unaware of name_encrypted.
        db.update("UPDATE shopping_items SET name = 'Lait d’avoine' WHERE id = ?", item);

        try (ConfigurableApplicationContext app = start(KEY)) {
            assertThat(app.getBean(ShoppingItemRepository.class).findById(item).orElseThrow().name()).isEqualTo("Lait d’avoine");
        }
        assertThat(PlaintextPerimeter.columnsInClear(db)).as("068 waits for a start that finds nothing in clear")
            .contains("shopping_items.name");

        try (ConfigurableApplicationContext ignored = start(KEY)) {
            // nothing in clear any more: 068 runs
        }
        assertThat(PlaintextPerimeter.columnsInClear(db)).isEmpty();
    }

    @Test
    void a_new_installation_has_its_final_schema_from_the_first_start_and_rewrites_nothing() {
        try (ConfigurableApplicationContext ignored = InstallationTestSupport.boot(database, dataDir,
                "spring.jpa.hibernate.ddl-auto=validate")) {
            assertThat(PlaintextPerimeter.columnsInClear(db)).isEmpty();
            assertThat(PlaintextPerimeter.requiredEncryptedColumnsAcceptingNull(db)).isEmpty();
            // A table keeps the files it was created with until something rewrites it — VACUUM FULL among others.
            assertThat(db.queryForList("""
                    SELECT relname FROM pg_class
                    WHERE relkind = 'r' AND relname = ANY (?) AND relfilenode <> oid""", String.class,
                    (Object) PlaintextPerimeter.COLUMNS.stream().map(PlaintextPerimeter.Column::table).distinct().toArray(String[]::new)))
                .as("tables of the perimeter rewritten on a new installation").isEmpty();
        }
    }

    @Test
    void a_key_that_does_not_decrypt_what_is_already_encrypted_encrypts_nothing_and_the_right_key_still_starts() throws Exception {
        writtenInClearByAnEarlierVersion();
        long before = PlaintextPerimeter.valuesInClear(db);

        assertThatThrownBy(() -> start(OTHER).close()).hasStackTraceContaining("does not decrypt");

        assertThat(PlaintextPerimeter.valuesInClear(db)).isEqualTo(before);
        // The refusal says to start with the right key: that start must not be refused in turn.
        try (ConfigurableApplicationContext app = start(KEY)) {
            everythingReadsAsItWasWritten(app);
        }
    }

    @Test
    void a_wrong_key_whose_start_stopped_before_its_key_check_is_still_taken_back_and_the_right_key_starts() throws Exception {
        // The start records the fingerprint of the key it was given, then stops for a reason of its own — here, just
        // before the key check — so the data never had its say on that key.
        writtenInClearByAnEarlierVersion();
        ApplicationListener<ApplicationPreparedEvent> stoppingBeforeTheKeyCheck = event -> event.getApplicationContext()
            .getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String name) {
                    if (bean instanceof EncryptionBackfillRunner) {
                        throw new IllegalStateException("stopped before its key check");
                    }
                    return bean;
                }
            });
        assertThatThrownBy(() -> InstallationTestSupport.boot(database, dataDir, List.of(stoppingBeforeTheKeyCheck), arguments(OTHER)).close())
            .hasStackTraceContaining("stopped before its key check");

        assertThatThrownBy(() -> start(OTHER).close()).hasStackTraceContaining("does not decrypt");

        try (ConfigurableApplicationContext app = start(KEY)) {
            everythingReadsAsItWasWritten(app);
        }
    }

    @Test
    void a_row_that_cannot_be_encrypted_stops_the_start_without_showing_its_value() throws Exception {
        writtenInClearByAnEarlierVersion();
        UUID broken = id("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) "
            + "VALUES ('SHARED', 'Secret de famille', '#c17a5c', '🏡', 'not-a-hex-salt-not-a-hex-salt-00') RETURNING id");

        assertThatThrownBy(() -> start(KEY).close())
            .hasStackTraceContaining("Could not seal row " + broken + " of spaces.name_encrypted")
            .satisfies(failure -> assertThat(stackTraceOf(failure)).doesNotContain("Secret de famille"));
    }

    private static String stackTraceOf(Throwable failure) {
        StringWriter out = new StringWriter();
        failure.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    @Test
    void a_first_start_cut_short_is_finished_by_the_next_and_its_tables_are_vacuumed_too() throws Exception {
        // What a first start leaves when it stops halfway: 067 applied, finance done, shopping half done.
        InstallationTestSupport.migrateUpTo(database, "068-");
        admin = id("INSERT INTO users (username, email, role) VALUES ('admin', 'admin@example.fr', 'SUPER_ADMIN') RETURNING id");
        db.update("UPDATE instance SET setup_completed_at = now() WHERE id = 1");
        spaceA = id("INSERT INTO spaces (type, name_encrypted, accent, glyph, encryption_salt) "
            + "VALUES ('SHARED', ?, '#c17a5c', '🏡', ?) RETURNING id", Encryptors.delux(KEY, SALT_A).encrypt("Famille"), SALT_A);
        db.update("INSERT INTO finance_categories (space_id, label_encrypted, color, icon, type, is_default) "
            + "VALUES (?, ?, '#f59e0b', 'Utensils', 'EXPENSE', true)", spaceA, Encryptors.delux(KEY, SALT_A).encrypt("Alimentation"));
        UUID aisle = id("INSERT INTO shopping_categories (space_id, name_encrypted, position) VALUES (?, ?, 0) RETURNING id",
            spaceA, Encryptors.delux(KEY, SALT_A).encrypt("Épicerie"));
        db.update("INSERT INTO shopping_items (space_id, category_id, name_encrypted, position) VALUES (?, ?, ?, 0)",
            spaceA, aisle, Encryptors.delux(KEY, SALT_A).encrypt("Pâtes"));
        db.update("INSERT INTO shopping_items (space_id, category_id, name, position) VALUES (?, ?, 'Riz', 1)", spaceA, aisle);
        long financeFiles = filenode("finance_categories");

        try (ConfigurableApplicationContext app = start(KEY)) {
            assertThat(app.getBean(ShoppingItemRepository.class).findBySpaceId(spaceA))
                .extracting(ShoppingItem::name).containsExactly("Pâtes", "Riz");
            assertThat(app.getBean(CategoryRepository.class).findBySpaceId(spaceA))
                .extracting(Category::label).containsExactly("Alimentation");
        }

        assertThat(PlaintextPerimeter.valuesInClear(db)).isZero();
        assertThat(filenode("finance_categories")).as("encrypted by the start cut short, vacuumed by this one")
            .isNotEqualTo(financeFiles);
    }

    @Test
    void the_two_factor_secrets_already_encrypted_refuse_a_wrong_key_too() throws Exception {
        // An installation older than 0.12 whose only encrypted data is a two-factor secret.
        InstallationTestSupport.migrateUpTo(database, "064-");
        admin = id("INSERT INTO users (username, email, role) VALUES ('admin', 'admin@example.fr', 'SUPER_ADMIN') RETURNING id");
        String userSalt = admin.toString().replace("-", "");
        db.update("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES (?, ?, true)",
            admin, Encryptors.delux(KEY, userSalt).encrypt("JBSWY3DPEHPK3PXP"));

        assertThatThrownBy(() -> start(OTHER).close()).hasStackTraceContaining("does not decrypt the two-factor secrets");

        try (ConfigurableApplicationContext ignored = start(KEY)) {
            // the right key starts
        }
    }
}
