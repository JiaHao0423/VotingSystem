package com.ben.com.backend.config;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aligns legacy MySQL columns with the current JPA model after enum-to-string migrations.
 */
@NullMarked
@Component
public class DatabaseSchemaPatcher implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatcher.class);

	@Language("SQL")
	private static final String VERSION_QUERY = "SELECT VERSION()";

	@Language("SQL")
	private static final String ALTER_VOTE_CHOICE =
			"ALTER TABLE vote_records MODIFY COLUMN choice VARCHAR(50) NOT NULL";

	@Language("SQL")
	private static final String FIND_ORPHAN_CHOICE_KEY_COLUMN = """
			SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
			WHERE TABLE_SCHEMA = DATABASE()
			AND TABLE_NAME = 'vote_records'
			AND COLUMN_NAME = 'choice_key'
			""";

	@Language("SQL")
	private static final String DROP_ORPHAN_CHOICE_KEY_COLUMN = "ALTER TABLE vote_records DROP COLUMN choice_key";

	@Language("SQL")
	private static final String FIND_PROPOSAL_COLUMN = """
			SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
			WHERE TABLE_SCHEMA = DATABASE()
			AND TABLE_NAME = 'proposals'
			AND COLUMN_NAME = ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public DatabaseSchemaPatcher(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!isMySql()) {
			return;
		}
		ensureVoteChoiceColumn();
		dropOrphanChoiceKeyColumn();
		ensureProposalColumns();
	}

	private boolean isMySql() {
		try {
			var product = jdbcTemplate.queryForObject(VERSION_QUERY, String.class);
			return product != null && product.toLowerCase().contains("mysql");
		} catch (Exception ex) {
			log.debug("Skipping schema patch: {}", ex.getMessage());
			return false;
		}
	}

	private void ensureVoteChoiceColumn() {
		try {
			jdbcTemplate.execute(ALTER_VOTE_CHOICE);
			log.info("Ensured vote_records.choice is VARCHAR(50)");
		} catch (Exception ex) {
			log.debug("vote_records.choice patch skipped: {}", ex.getMessage());
		}
	}

	private void dropOrphanChoiceKeyColumn() {
		try {
			var columns = jdbcTemplate.queryForList(FIND_ORPHAN_CHOICE_KEY_COLUMN);
			if (columns.isEmpty()) {
				return;
			}
			jdbcTemplate.execute(DROP_ORPHAN_CHOICE_KEY_COLUMN);
			log.info("Dropped orphan vote_records.choice_key column");
		} catch (Exception ex) {
			log.debug("vote_records.choice_key cleanup skipped: {}", ex.getMessage());
		}
	}

	private void ensureProposalColumns() {
		addProposalColumnIfMissing("vote_options", "TEXT NOT NULL");
		addProposalColumnIfMissing("pass_threshold_numerator", "INT NOT NULL DEFAULT 1");
		addProposalColumnIfMissing("pass_threshold_denominator", "INT NOT NULL DEFAULT 2");
		addProposalColumnIfMissing("threshold_base", "VARCHAR(20) NOT NULL DEFAULT 'ATTENDED'");
		addProposalColumnIfMissing("allow_revote", "BOOLEAN NOT NULL DEFAULT TRUE");
	}

	private void addProposalColumnIfMissing(String column, String definition) {
		try {
			var existing = jdbcTemplate.queryForList(FIND_PROPOSAL_COLUMN, column);
			if (!existing.isEmpty()) {
				return;
			}
			jdbcTemplate.execute("ALTER TABLE proposals ADD COLUMN " + column + " " + definition);
			log.info("Added proposals.{} column", column);
		} catch (Exception ex) {
			log.debug("proposals.{} patch skipped: {}", column, ex.getMessage());
		}
	}
}
